package transport

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/domain"
)

type readinessStub bool

func (stub readinessStub) IsReady() bool { return bool(stub) }

type ingestorStub struct {
	credential string
	envelope   domain.EventEnvelope
	receipt    domain.PublishReceipt
	err        error
}

func (stub *ingestorStub) Ingest(_ context.Context, credential string, envelope domain.EventEnvelope) (domain.PublishReceipt, error) {
	stub.credential = credential
	stub.envelope = envelope
	return stub.receipt, stub.err
}

func TestHealthAndReadinessEndpoints(t *testing.T) {
	tests := []struct {
		name       string
		path       string
		ready      readinessStub
		wantStatus int
	}{
		{name: "live", path: "/healthz", ready: false, wantStatus: http.StatusOK},
		{name: "ready", path: "/readyz", ready: true, wantStatus: http.StatusOK},
		{name: "not ready", path: "/readyz", ready: false, wantStatus: http.StatusServiceUnavailable},
		{name: "ingestion unavailable until authorized", path: "/v1/events", ready: true, wantStatus: http.StatusNotFound},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			request := httptest.NewRequest(http.MethodGet, test.path, nil)
			response := httptest.NewRecorder()
			NewHandler(test.ready).ServeHTTP(response, request)
			if response.Code != test.wantStatus {
				t.Fatalf("status = %d, want %d", response.Code, test.wantStatus)
			}
			if response.Header().Get("Cache-Control") != "no-store" && test.path != "/v1/events" {
				t.Fatalf("Cache-Control = %q", response.Header().Get("Cache-Control"))
			}
		})
	}
}

func TestMetricsEndpointReportsOnlyBoundedRequestOutcomes(t *testing.T) {
	ingestor := &ingestorStub{receipt: domain.PublishReceipt{EventID: "70000000-0000-4000-8000-000000000099"}}
	handler := NewHandler(readinessStub(true), WithEventIngestion(ingestor, 4096))

	unauthorized := httptest.NewRequest(http.MethodPost, "/v1/events", nil)
	handler.ServeHTTP(httptest.NewRecorder(), unauthorized)

	body, err := json.Marshal(transportEnvelope())
	if err != nil {
		t.Fatalf("Marshal() error = %v", err)
	}
	accepted := httptest.NewRequest(http.MethodPost, "/v1/events", strings.NewReader(string(body)))
	accepted.Header.Set("Content-Type", "application/json")
	accepted.Header.Set("Authorization", "Bearer verified-local-credential")
	handler.ServeHTTP(httptest.NewRecorder(), accepted)

	response := httptest.NewRecorder()
	handler.ServeHTTP(response, httptest.NewRequest(http.MethodGet, "/metrics", nil))
	metrics := response.Body.String()
	if response.Code != http.StatusOK || response.Header().Get("Cache-Control") != "no-store" || response.Header().Get("X-Content-Type-Options") != "nosniff" {
		t.Fatalf("metrics response = %d %#v", response.Code, response.Header())
	}
	for _, expected := range []string{
		`nexora_event_ingestion_http_requests_total{outcome="accepted"} 1`,
		`nexora_event_ingestion_http_requests_total{outcome="unauthorized"} 1`,
		`nexora_event_ingestion_http_request_duration_seconds_count 2`,
		`nexora_event_ingestion_http_in_flight_requests 0`,
	} {
		if !strings.Contains(metrics, expected) {
			t.Fatalf("metrics missing %q: %s", expected, metrics)
		}
	}
	for _, unexpected := range []string{"verified-local-credential", "10000000-0000-4000-8000-000000000001", "00000000000000000000000000000001"} {
		if strings.Contains(metrics, unexpected) {
			t.Fatalf("metrics leaked request value %q: %s", unexpected, metrics)
		}
	}
}

func TestEventIngestionAcceptsOnlyBoundedBearerJSON(t *testing.T) {
	ingestor := &ingestorStub{receipt: domain.PublishReceipt{EventID: "70000000-0000-4000-8000-000000000099"}}
	handler := NewHandler(readinessStub(true), WithEventIngestion(ingestor, 4096))
	body, err := json.Marshal(transportEnvelope())
	if err != nil {
		t.Fatalf("Marshal() error = %v", err)
	}
	request := httptest.NewRequest(http.MethodPost, "/v1/events", strings.NewReader(string(body)))
	request.Header.Set("Content-Type", "application/json; charset=utf-8")
	request.Header.Set("Authorization", "Bearer verified-local-credential")
	response := httptest.NewRecorder()

	handler.ServeHTTP(response, request)
	if response.Code != http.StatusAccepted {
		t.Fatalf("status = %d, body = %s", response.Code, response.Body.String())
	}
	if ingestor.credential != "verified-local-credential" || ingestor.envelope.EventID == "" {
		t.Fatalf("ingestor inputs = %q %#v", ingestor.credential, ingestor.envelope)
	}
	if response.Header().Get("Cache-Control") != "no-store" {
		t.Fatalf("Cache-Control = %q", response.Header().Get("Cache-Control"))
	}
	if !strings.Contains(response.Body.String(), `"eventId":"70000000-0000-4000-8000-000000000001"`) {
		t.Fatalf("response did not retain request event ID: %s", response.Body.String())
	}
}

func TestEventIngestionRejectsUnsafeRequestBeforeCollector(t *testing.T) {
	tests := []struct {
		name        string
		header      string
		body        string
		contentType string
		bodyLimit   int64
		wantStatus  int
	}{
		{name: "missing bearer", body: `{}`, contentType: "application/json", bodyLimit: 4096, wantStatus: http.StatusUnauthorized},
		{name: "unknown field", header: "Bearer valid", body: `{"eventId":"x","unknown":true}`, contentType: "application/json", bodyLimit: 4096, wantStatus: http.StatusBadRequest},
		{name: "unsupported content type", header: "Bearer valid", body: `{}`, contentType: "text/plain", bodyLimit: 4096, wantStatus: http.StatusBadRequest},
		{name: "body too large", header: "Bearer valid", body: fmt.Sprintf(`{"data":"%s"}`, strings.Repeat("x", 2048)), contentType: "application/json", bodyLimit: 1024, wantStatus: http.StatusRequestEntityTooLarge},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			ingestor := &ingestorStub{}
			handler := NewHandler(readinessStub(true), WithEventIngestion(ingestor, test.bodyLimit))
			request := httptest.NewRequest(http.MethodPost, "/v1/events", strings.NewReader(test.body))
			request.Header.Set("Content-Type", test.contentType)
			if test.header != "" {
				request.Header.Set("Authorization", test.header)
			}
			response := httptest.NewRecorder()
			handler.ServeHTTP(response, request)
			if response.Code != test.wantStatus {
				t.Fatalf("status = %d, body = %s", response.Code, response.Body.String())
			}
			if ingestor.credential != "" {
				t.Fatalf("collector was called with %q", ingestor.credential)
			}
		})
	}
}

func TestEventIngestionMapsCollectorFailuresWithoutLeakingDetails(t *testing.T) {
	tests := []struct {
		name       string
		err        error
		wantStatus int
		wantCode   string
	}{
		{name: "unauthorized", err: fmt.Errorf("%w: stale membership", domain.ErrUnauthorized), wantStatus: http.StatusUnauthorized, wantCode: "UNAUTHORIZED"},
		{name: "rate limited", err: domain.ErrRateLimited, wantStatus: http.StatusTooManyRequests, wantCode: "RATE_LIMITED"},
		{name: "invalid", err: domain.ErrInvalidEnvelope, wantStatus: http.StatusUnprocessableEntity, wantCode: "INVALID_EVENT"},
		{name: "publisher", err: fmt.Errorf("%w: nats unavailable", domain.ErrPublish), wantStatus: http.StatusServiceUnavailable, wantCode: "INGESTION_UNAVAILABLE"},
		{name: "unexpected", err: errors.New("database password leaked"), wantStatus: http.StatusServiceUnavailable, wantCode: "INGESTION_UNAVAILABLE"},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			ingestor := &ingestorStub{err: test.err}
			handler := NewHandler(readinessStub(true), WithEventIngestion(ingestor, 4096))
			body, err := json.Marshal(transportEnvelope())
			if err != nil {
				t.Fatalf("Marshal() error = %v", err)
			}
			request := httptest.NewRequest(http.MethodPost, "/v1/events", strings.NewReader(string(body)))
			request.Header.Set("Content-Type", "application/json")
			request.Header.Set("Authorization", "Bearer valid")
			response := httptest.NewRecorder()
			handler.ServeHTTP(response, request)
			if response.Code != test.wantStatus || !strings.Contains(response.Body.String(), test.wantCode) || strings.Contains(response.Body.String(), "password") {
				t.Fatalf("status/body = %d %s", response.Code, response.Body.String())
			}
		})
	}
}

func transportEnvelope() domain.EventEnvelope {
	now := time.Date(2026, 8, 11, 0, 0, 0, 0, time.UTC)
	return domain.EventEnvelope{
		EventID:              "70000000-0000-4000-8000-000000000001",
		EventType:            domain.EventTypePublicationInvalidated,
		EventVersion:         1,
		OrganizationID:       "10000000-0000-4000-8000-000000000001",
		SubjectID:            "90000000-0000-4000-8000-000000000001",
		ResourceType:         "page",
		ResourceID:           "30000000-0000-4000-8000-000000000001",
		Topic:                "tenant:10000000-0000-4000-8000-000000000001:publication",
		ActorID:              "80000000-0000-4000-8000-000000000001",
		TraceID:              "00000000000000000000000000000001",
		IdempotencyKeyDigest: "sha256:6fb55c56873b994eb10bd194e65b098681d86ab4f57d100ab32d79fa0abc7a7a",
		PayloadDigest:        "sha256:5757d2decc21af6e904697887c476532756da2d79bd132fe70ef14d0dfbc60f1",
		SafePayload: map[string]any{
			"resourceId":     "30000000-0000-4000-8000-000000000001",
			"resourceType":   "page",
			"organizationId": "10000000-0000-4000-8000-000000000001",
			"subjectId":      "90000000-0000-4000-8000-000000000001",
			"actorId":        "80000000-0000-4000-8000-000000000001",
			"eventVersion":   1,
			"correlationId":  "10000000000000000000000000000001",
			"traceId":        "00000000000000000000000000000001",
			"receiptId":      "a1000000-0000-4000-8000-000000000001",
			"schemaVersion":  "1.1.0",
			"safeDisplay":    map[string]any{"label": "PUBLICATION_INVALIDATED", "status": "QUEUED", "variant": "warning"},
		},
		OccurredAt:    now,
		SchemaVersion: domain.SchemaVersion,
	}
}
