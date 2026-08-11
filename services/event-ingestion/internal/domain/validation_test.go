package domain

import (
	"encoding/json"
	"errors"
	"testing"
	"time"
)

func TestValidateEnvelopeRejectsUnsafePayloadAndRouting(t *testing.T) {
	envelope := validationEnvelope()
	tests := []struct {
		name   string
		mutate func(EventEnvelope) EventEnvelope
	}{
		{name: "unsafe key", mutate: func(event EventEnvelope) EventEnvelope {
			event.SafePayload["body"] = "private"
			return event
		}},
		{name: "unsafe nested value", mutate: func(event EventEnvelope) EventEnvelope {
			event.SafePayload["safeDisplay"].(map[string]any)["hint"] = "Bearer private-value"
			return event
		}},
		{name: "wrong topic", mutate: func(event EventEnvelope) EventEnvelope {
			event.Topic = "tenant:10000000-0000-4000-8000-000000000002:publication"
			return event
		}},
		{name: "identity mismatch", mutate: func(event EventEnvelope) EventEnvelope {
			event.SafePayload["organizationId"] = "10000000-0000-4000-8000-000000000002"
			return event
		}},
		{name: "invalid progress", mutate: func(event EventEnvelope) EventEnvelope {
			event.SafePayload["progress"] = json.Number("101")
			return event
		}},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			candidate := cloneEnvelope(envelope)
			candidate = test.mutate(candidate)
			if err := ValidateEnvelope(candidate); !errors.Is(err, ErrInvalidEnvelope) {
				t.Fatalf("ValidateEnvelope() error = %v", err)
			}
		})
	}
}

func TestValidateAuthorizedEnvelopeRejectsExpiredContext(t *testing.T) {
	now := time.Date(2026, 8, 11, 0, 0, 0, 0, time.UTC)
	envelope := validationEnvelope()
	authorization := Authorization{
		OrganizationID: envelope.OrganizationID,
		SubjectID:      envelope.SubjectID,
		ActorID:        envelope.ActorID,
		ResourceType:   envelope.ResourceType,
		ResourceID:     envelope.ResourceID,
		EventType:      envelope.EventType,
		IssuedAt:       now.Add(-2 * time.Minute),
		ExpiresAt:      now.Add(-time.Minute),
	}
	if err := ValidateAuthorizedEnvelope(now, authorization, envelope); !errors.Is(err, ErrUnownedSubject) {
		t.Fatalf("ValidateAuthorizedEnvelope() error = %v", err)
	}
}

func validationEnvelope() EventEnvelope {
	return EventEnvelope{
		EventID:              "70000000-0000-4000-8000-000000000001",
		EventType:            EventTypePublicationInvalidated,
		EventVersion:         1,
		OrganizationID:       "10000000-0000-4000-8000-000000000001",
		SubjectID:            "90000000-0000-4000-8000-000000000001",
		ResourceType:         "page",
		ResourceID:           "30000000-0000-4000-8000-000000000001",
		Topic:                "tenant:10000000-0000-4000-8000-000000000001:publication",
		ActorID:              "80000000-0000-4000-8000-000000000001",
		TraceID:              "trace-event-alpha-publication",
		IdempotencyKeyDigest: "sha256:event-contract-alpha-publication",
		PayloadDigest:        "sha256:1111111111111111111111111111111111111111111111111111111111111111",
		SafePayload: map[string]any{
			"organizationId": "10000000-0000-4000-8000-000000000001",
			"subjectId":      "90000000-0000-4000-8000-000000000001",
			"actorId":        "80000000-0000-4000-8000-000000000001",
			"resourceId":     "30000000-0000-4000-8000-000000000001",
			"resourceType":   "page",
			"eventVersion":   json.Number("1"),
			"traceId":        "trace-event-alpha-publication",
			"schemaVersion":  "1.0.0",
			"safeDisplay": map[string]any{
				"label":  "Alpha publication invalidated",
				"status": "queued",
			},
		},
		OccurredAt:    time.Date(2026, 8, 10, 23, 59, 59, 0, time.UTC),
		SchemaVersion: SchemaVersion,
	}
}

func cloneEnvelope(source EventEnvelope) EventEnvelope {
	clone := source
	clone.SafePayload = make(map[string]any, len(source.SafePayload))
	for key, value := range source.SafePayload {
		if display, ok := value.(map[string]any); ok {
			copied := make(map[string]any, len(display))
			for displayKey, displayValue := range display {
				copied[displayKey] = displayValue
			}
			clone.SafePayload[key] = copied
			continue
		}
		clone.SafePayload[key] = value
	}
	return clone
}
