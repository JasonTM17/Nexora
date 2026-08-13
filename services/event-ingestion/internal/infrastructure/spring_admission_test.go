package infrastructure

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/domain"
)

var admissionNow = time.Date(2026, 8, 12, 0, 0, 0, 0, time.UTC)

func TestSpringAdmissionAuthorizerReturnsOnlyMatchingBoundedDecision(t *testing.T) {
	envelope := admissionEnvelope()
	server := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		if request.URL.Path != "/api/v1/internal/event-admission/publication-invalidated" ||
			request.Header.Get("Authorization") != "Bearer verified-credential" ||
			request.Header.Get("X-Nexora-Organization-Id") != envelope.OrganizationID {
			t.Fatalf("unexpected admission request: %s %#v", request.URL.Path, request.Header)
		}
		var candidate map[string]any
		if err := json.NewDecoder(request.Body).Decode(&candidate); err != nil || candidate["eventVersion"] != float64(envelope.EventVersion) {
			t.Fatalf("candidate = %#v, %v", candidate, err)
		}
		_ = json.NewEncoder(writer).Encode(admissionResponse(envelope, admissionNow.Add(30*time.Second)))
	}))
	defer server.Close()
	authorizer, err := newSpringAdmissionAuthorizer(server.URL+"/api/v1/internal/event-admission", server.Client(), time.Second, func() time.Time { return admissionNow })
	if err != nil {
		t.Fatalf("newSpringAdmissionAuthorizer() error = %v", err)
	}
	authorization, err := authorizer.Authorize(context.Background(), "verified-credential", envelope)
	if err != nil {
		t.Fatalf("Authorize() error = %v", err)
	}
	if authorization.OrganizationID != envelope.OrganizationID || authorization.ExpiresAt != admissionNow.Add(30*time.Second) {
		t.Fatalf("authorization = %#v", authorization)
	}
}

func TestSpringAdmissionAuthorizerRejectsMismatchedOrUnsafeResponses(t *testing.T) {
	envelope := admissionEnvelope()
	tests := []struct {
		name   string
		status int
		body   any
	}{
		{name: "forbidden", status: http.StatusForbidden, body: map[string]string{"code": "PERMISSION_DENIED"}},
		{name: "version mismatch", status: http.StatusOK, body: admissionResponseWithVersion(envelope, 2, admissionNow.Add(time.Second))},
		{name: "unbounded validity", status: http.StatusOK, body: admissionResponse(envelope, admissionNow.Add(6*time.Minute))},
		{name: "unknown response field", status: http.StatusOK, body: map[string]any{"unexpected": true}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			server := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, _ *http.Request) {
				writer.WriteHeader(test.status)
				_ = json.NewEncoder(writer).Encode(test.body)
			}))
			defer server.Close()
			authorizer, err := newSpringAdmissionAuthorizer(server.URL+"/api/v1/internal/event-admission", server.Client(), time.Second, func() time.Time { return admissionNow })
			if err != nil {
				t.Fatalf("newSpringAdmissionAuthorizer() error = %v", err)
			}
			if _, err := authorizer.Authorize(context.Background(), "verified-credential", envelope); err == nil {
				t.Fatal("Authorize() accepted unsafe admission response")
			}
		})
	}
}

func admissionEnvelope() domain.EventEnvelope {
	return domain.EventEnvelope{
		EventID: "70000000-0000-4000-8000-000000000001", EventType: domain.EventTypePublicationInvalidated,
		EventVersion: 1, OrganizationID: "10000000-0000-4000-8000-000000000001",
		SubjectID: "90000000-0000-4000-8000-000000000001", ResourceType: "page",
		ResourceID: "30000000-0000-4000-8000-000000000001", ActorID: "80000000-0000-4000-8000-000000000001",
		Topic: "tenant:10000000-0000-4000-8000-000000000001:publication", SchemaVersion: domain.SchemaVersion,
	}
}

func admissionResponse(envelope domain.EventEnvelope, validUntil time.Time) map[string]any {
	return admissionResponseWithVersion(envelope, envelope.EventVersion, validUntil)
}

func admissionResponseWithVersion(envelope domain.EventEnvelope, eventVersion int64, validUntil time.Time) map[string]any {
	return map[string]any{
		"organizationId": envelope.OrganizationID, "subjectId": envelope.SubjectID, "actorId": envelope.ActorID,
		"resourceType": envelope.ResourceType, "resourceId": envelope.ResourceID, "eventType": envelope.EventType,
		"eventVersion": eventVersion, "schemaVersion": envelope.SchemaVersion, "topic": envelope.Topic,
		"validUntil": validUntil.Format(time.RFC3339Nano),
	}
}
