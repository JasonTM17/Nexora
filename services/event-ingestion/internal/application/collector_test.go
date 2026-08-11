package application

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/domain"
)

var collectorNow = time.Date(2026, 8, 11, 0, 0, 0, 0, time.UTC)

type authorizerStub struct {
	authorization domain.Authorization
	err           error
}

func (stub authorizerStub) Authorize(context.Context, string, domain.EventEnvelope) (domain.Authorization, error) {
	return stub.authorization, stub.err
}

type limiterStub struct {
	allowed bool
	key     string
}

func (stub *limiterStub) Allow(key string, _ time.Time) bool {
	stub.key = key
	return stub.allowed
}

type publisherStub struct {
	subject  string
	envelope domain.EventEnvelope
	ack      PublishAck
	err      error
}

func (stub *publisherStub) Publish(_ context.Context, subject string, envelope domain.EventEnvelope) (PublishAck, error) {
	stub.subject = subject
	stub.envelope = envelope
	return stub.ack, stub.err
}

func TestCollectorPublishesOnlyAuthorizedValidatedEnvelope(t *testing.T) {
	envelope := validEnvelope()
	limiter := &limiterStub{allowed: true}
	publisher := &publisherStub{ack: PublishAck{Stream: "EVENTS", Sequence: 12}}
	collector, err := NewCollector(
		authorizerStub{authorization: validAuthorization(envelope)},
		limiter,
		publisher,
		func() time.Time { return collectorNow },
	)
	if err != nil {
		t.Fatalf("NewCollector() error = %v", err)
	}

	receipt, err := collector.Ingest(context.Background(), "verified-credential", envelope)
	if err != nil {
		t.Fatalf("Ingest() error = %v", err)
	}
	if limiter.key != envelope.OrganizationID+":"+envelope.SubjectID {
		t.Fatalf("limiter key = %q", limiter.key)
	}
	if publisher.subject != "nexora.events.publication" || publisher.envelope.EventID != envelope.EventID {
		t.Fatalf("publish subject/envelope = %q %#v", publisher.subject, publisher.envelope)
	}
	if receipt.EventID != envelope.EventID || receipt.Stream != "EVENTS" || receipt.Sequence != 12 {
		t.Fatalf("receipt = %#v", receipt)
	}
}

func TestCollectorRejectsAuthorizationMismatchBeforeRateOrPublish(t *testing.T) {
	envelope := validEnvelope()
	authorization := validAuthorization(envelope)
	authorization.OrganizationID = "10000000-0000-4000-8000-000000000002"
	limiter := &limiterStub{allowed: true}
	publisher := &publisherStub{}
	collector, err := NewCollector(
		authorizerStub{authorization: authorization}, limiter, publisher, func() time.Time { return collectorNow })
	if err != nil {
		t.Fatalf("NewCollector() error = %v", err)
	}

	_, err = collector.Ingest(context.Background(), "verified-credential", envelope)
	if !errors.Is(err, domain.ErrUnauthorized) {
		t.Fatalf("Ingest() error = %v", err)
	}
	if limiter.key != "" || publisher.subject != "" {
		t.Fatalf("unauthorized event reached limiter/publisher: %q %q", limiter.key, publisher.subject)
	}
}

func TestCollectorRejectsRateLimitAndPublishFailure(t *testing.T) {
	envelope := validEnvelope()
	tests := []struct {
		name       string
		limiter    *limiterStub
		publisher  *publisherStub
		want       error
		wantCalled bool
	}{
		{name: "rate limited", limiter: &limiterStub{allowed: false}, publisher: &publisherStub{}, want: domain.ErrRateLimited},
		{name: "publish failed", limiter: &limiterStub{allowed: true}, publisher: &publisherStub{err: errors.New("nats unavailable")}, want: domain.ErrPublish, wantCalled: true},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			collector, err := NewCollector(
				authorizerStub{authorization: validAuthorization(envelope)},
				test.limiter,
				test.publisher,
				func() time.Time { return collectorNow },
			)
			if err != nil {
				t.Fatalf("NewCollector() error = %v", err)
			}
			_, err = collector.Ingest(context.Background(), "verified-credential", envelope)
			if !errors.Is(err, test.want) {
				t.Fatalf("Ingest() error = %v, want %v", err, test.want)
			}
			if (test.publisher.subject != "") != test.wantCalled {
				t.Fatalf("publisher called = %t", test.publisher.subject != "")
			}
		})
	}
}

func validAuthorization(envelope domain.EventEnvelope) domain.Authorization {
	return domain.Authorization{
		OrganizationID: envelope.OrganizationID,
		SubjectID:      envelope.SubjectID,
		ActorID:        envelope.ActorID,
		ResourceType:   envelope.ResourceType,
		ResourceID:     envelope.ResourceID,
		EventType:      envelope.EventType,
		IssuedAt:       collectorNow.Add(-time.Minute),
		ExpiresAt:      collectorNow.Add(time.Minute),
	}
}

func validEnvelope() domain.EventEnvelope {
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
		TraceID:              "trace-event-alpha-publication",
		IdempotencyKeyDigest: "sha256:event-contract-alpha-publication",
		PayloadDigest:        "sha256:1111111111111111111111111111111111111111111111111111111111111111",
		SafePayload: map[string]any{
			"resourceId":     "30000000-0000-4000-8000-000000000001",
			"resourceType":   "page",
			"organizationId": "10000000-0000-4000-8000-000000000001",
			"subjectId":      "90000000-0000-4000-8000-000000000001",
			"actorId":        "80000000-0000-4000-8000-000000000001",
			"eventVersion":   1,
			"traceId":        "trace-event-alpha-publication",
			"schemaVersion":  "1.0.0",
			"safeDisplay": map[string]any{
				"label":   "Alpha publication invalidated",
				"status":  "queued",
				"hint":    "Awaiting durable publication",
				"variant": "warning",
			},
		},
		OccurredAt:    collectorNow.Add(-time.Second),
		SchemaVersion: domain.SchemaVersion,
	}
}
