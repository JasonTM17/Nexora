package infrastructure

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/domain"
)

const maximumAdmissionResponseBytes = 4096

type httpDoer interface {
	Do(*http.Request) (*http.Response, error)
}

// SpringAdmissionAuthorizer asks the existing JWT/RLS authority to normalize
// one Go-ingress candidate. It does not mint, cache or interpret access tokens.
type SpringAdmissionAuthorizer struct {
	endpoint string
	client   httpDoer
	timeout  time.Duration
	now      func() time.Time
}

func NewSpringAdmissionAuthorizer(admissionURL string, timeout time.Duration) (*SpringAdmissionAuthorizer, error) {
	if !strings.HasSuffix(admissionURL, "/api/v1/internal/event-admission") || timeout < time.Millisecond || timeout > 30*time.Second {
		return nil, fmt.Errorf("valid Spring admission endpoint and bounded timeout are required")
	}
	return newSpringAdmissionAuthorizer(admissionURL, &http.Client{
		Timeout:       timeout,
		CheckRedirect: func(_ *http.Request, _ []*http.Request) error { return http.ErrUseLastResponse },
	}, timeout, time.Now)
}

func newSpringAdmissionAuthorizer(endpoint string, client httpDoer, timeout time.Duration, now func() time.Time) (*SpringAdmissionAuthorizer, error) {
	if endpoint == "" || client == nil || now == nil || timeout < time.Millisecond || timeout > 30*time.Second {
		return nil, fmt.Errorf("Spring admission authorizer dependencies are required")
	}
	return &SpringAdmissionAuthorizer{endpoint: endpoint, client: client, timeout: timeout, now: now}, nil
}

func (authorizer *SpringAdmissionAuthorizer) Authorize(ctx context.Context, credential string, envelope domain.EventEnvelope) (domain.Authorization, error) {
	if authorizer == nil || ctx == nil || strings.TrimSpace(credential) == "" {
		return domain.Authorization{}, fmt.Errorf("Spring admission authorization is required")
	}
	requestBody, err := json.Marshal(struct {
		EventType     domain.EventType `json:"eventType"`
		ResourceType  string           `json:"resourceType"`
		ResourceID    string           `json:"resourceId"`
		EventVersion  int64            `json:"eventVersion"`
		SchemaVersion string           `json:"schemaVersion"`
	}{envelope.EventType, envelope.ResourceType, envelope.ResourceID, envelope.EventVersion, envelope.SchemaVersion})
	if err != nil {
		return domain.Authorization{}, fmt.Errorf("marshal Spring admission candidate: %w", err)
	}
	requestContext, cancel := context.WithTimeout(ctx, authorizer.timeout)
	defer cancel()
	request, err := http.NewRequestWithContext(requestContext, http.MethodPost,
		authorizer.endpoint+"/publication-invalidated", bytes.NewReader(requestBody))
	if err != nil {
		return domain.Authorization{}, fmt.Errorf("create Spring admission request: %w", err)
	}
	request.Header.Set("Authorization", "Bearer "+credential)
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("X-Nexora-Organization-Id", envelope.OrganizationID)
	response, err := authorizer.client.Do(request)
	if err != nil {
		return domain.Authorization{}, fmt.Errorf("request Spring admission: %w", err)
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		return domain.Authorization{}, fmt.Errorf("Spring admission returned HTTP %d", response.StatusCode)
	}
	body, err := io.ReadAll(io.LimitReader(response.Body, maximumAdmissionResponseBytes+1))
	if err != nil || len(body) > maximumAdmissionResponseBytes {
		return domain.Authorization{}, fmt.Errorf("read bounded Spring admission response")
	}
	var decision admissionDecision
	decoder := json.NewDecoder(bytes.NewReader(body))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&decision); err != nil {
		return domain.Authorization{}, fmt.Errorf("decode Spring admission response: %w", err)
	}
	if err := decoder.Decode(&struct{}{}); err != io.EOF {
		return domain.Authorization{}, fmt.Errorf("Spring admission response has trailing content")
	}
	now := authorizer.now()
	if decision.ValidUntil.IsZero() || !decision.ValidUntil.After(now) || decision.ValidUntil.Sub(now) > 5*time.Minute {
		return domain.Authorization{}, fmt.Errorf("Spring admission validity is not bounded")
	}
	if decision.EventVersion != envelope.EventVersion || decision.SchemaVersion != envelope.SchemaVersion || decision.Topic != envelope.Topic {
		return domain.Authorization{}, fmt.Errorf("Spring admission decision does not match candidate routing")
	}
	return domain.Authorization{
		OrganizationID: decision.OrganizationID,
		SubjectID:      decision.SubjectID,
		ActorID:        decision.ActorID,
		ResourceType:   decision.ResourceType,
		ResourceID:     decision.ResourceID,
		EventType:      decision.EventType,
		IssuedAt:       now,
		ExpiresAt:      decision.ValidUntil,
	}, nil
}

type admissionDecision struct {
	OrganizationID string           `json:"organizationId"`
	SubjectID      string           `json:"subjectId"`
	ActorID        string           `json:"actorId"`
	ResourceType   string           `json:"resourceType"`
	ResourceID     string           `json:"resourceId"`
	EventType      domain.EventType `json:"eventType"`
	EventVersion   int64            `json:"eventVersion"`
	SchemaVersion  string           `json:"schemaVersion"`
	Topic          string           `json:"topic"`
	ValidUntil     time.Time        `json:"validUntil"`
}
