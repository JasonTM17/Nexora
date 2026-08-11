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
		{name: "unsafe display label", mutate: func(event EventEnvelope) EventEnvelope {
			event.SafePayload["safeDisplay"].(map[string]any)["label"] = "alice@example.test"
			return event
		}},
		{name: "unsafe display variant", mutate: func(event EventEnvelope) EventEnvelope {
			event.SafePayload["safeDisplay"].(map[string]any)["variant"] = "success"
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
		{name: "secret in idempotency digest", mutate: func(event EventEnvelope) EventEnvelope {
			event.IdempotencyKeyDigest = "Bearer-secret-value-0001"
			return event
		}},
		{name: "email in payload digest", mutate: func(event EventEnvelope) EventEnvelope {
			event.PayloadDigest = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
			return event
		}},
		{name: "wrong payload digest", mutate: func(event EventEnvelope) EventEnvelope {
			event.PayloadDigest = "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
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
		TraceID:              "00000000000000000000000000000001",
		IdempotencyKeyDigest: "sha256:6fb55c56873b994eb10bd194e65b098681d86ab4f57d100ab32d79fa0abc7a7a",
		PayloadDigest:        "sha256:5757d2decc21af6e904697887c476532756da2d79bd132fe70ef14d0dfbc60f1",
		SafePayload: map[string]any{
			"organizationId": "10000000-0000-4000-8000-000000000001",
			"subjectId":      "90000000-0000-4000-8000-000000000001",
			"actorId":        "80000000-0000-4000-8000-000000000001",
			"resourceId":     "30000000-0000-4000-8000-000000000001",
			"resourceType":   "page",
			"eventVersion":   json.Number("1"),
			"correlationId":  "10000000000000000000000000000001",
			"traceId":        "00000000000000000000000000000001",
			"receiptId":      "a1000000-0000-4000-8000-000000000001",
			"schemaVersion":  "1.1.0",
			"safeDisplay": map[string]any{
				"label":   "PUBLICATION_INVALIDATED",
				"status":  "QUEUED",
				"variant": "warning",
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
