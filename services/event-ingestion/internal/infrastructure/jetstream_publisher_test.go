package infrastructure

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/domain"
	"github.com/nats-io/nats.go"
)

type jetStreamStub struct {
	message     *nats.Msg
	ack         *nats.PubAck
	err         error
	deadline    time.Time
	hasDeadline bool
}

func (stub *jetStreamStub) Publish(context context.Context, message *nats.Msg) (*nats.PubAck, error) {
	stub.message = message
	stub.deadline, stub.hasDeadline = context.Deadline()
	return stub.ack, stub.err
}

func TestJetStreamPublisherMarshalsCanonicalEnvelopeAndWaitsForAck(t *testing.T) {
	stub := &jetStreamStub{ack: &nats.PubAck{Stream: "NEXORA_EVENTS", Sequence: 17}}
	publisher, err := newJetStreamPublisher(stub, time.Second)
	if err != nil {
		t.Fatalf("NewJetStreamPublisher() error = %v", err)
	}
	envelope := publisherEnvelope()
	ack, err := publisher.Publish(context.Background(), "nexora.events.publication", envelope)
	if err != nil {
		t.Fatalf("Publish() error = %v", err)
	}
	if stub.message == nil || !stub.hasDeadline || stub.message.Header.Get(nats.MsgIdHdr) != envelope.EventID || stub.message.Header.Get("Nexora-Schema-Version") != domain.SchemaVersion {
		t.Fatalf("published message = %#v", stub.message)
	}
	if ack.Stream != "NEXORA_EVENTS" || ack.Sequence != 17 || ack.Duplicate {
		t.Fatalf("Publish() ack = %#v", ack)
	}
}

func TestJetStreamPublisherRejectsWrongRouteOrIncompleteAck(t *testing.T) {
	envelope := publisherEnvelope()
	tests := []struct {
		name    string
		subject string
		stub    *jetStreamStub
	}{
		{name: "wrong route", subject: "nexora.events.workflow", stub: &jetStreamStub{}},
		{name: "missing ack", subject: "nexora.events.publication", stub: &jetStreamStub{ack: &nats.PubAck{}}},
		{name: "transport error", subject: "nexora.events.publication", stub: &jetStreamStub{err: errors.New("nats unavailable")}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			publisher, err := newJetStreamPublisher(test.stub, time.Second)
			if err != nil {
				t.Fatalf("NewJetStreamPublisher() error = %v", err)
			}
			if _, err := publisher.Publish(context.Background(), test.subject, envelope); err == nil {
				t.Fatal("Publish() succeeded")
			}
		})
	}
}

func TestJetStreamPublisherRejectsNilDependency(t *testing.T) {
	if publisher, err := newJetStreamPublisher(nil, time.Second); err == nil || publisher != nil {
		t.Fatalf("newJetStreamPublisher(nil) = %#v, %v", publisher, err)
	}
}

func TestJetStreamPublisherBoundsSlowAcknowledgementAndHonorsCancellation(t *testing.T) {
	slow := jetStreamFunc(func(ctx context.Context, _ *nats.Msg) (*nats.PubAck, error) {
		<-ctx.Done()
		return nil, ctx.Err()
	})
	publisher, err := newJetStreamPublisher(slow, 5*time.Millisecond)
	if err != nil {
		t.Fatalf("newJetStreamPublisher() error = %v", err)
	}
	if _, err := publisher.Publish(context.Background(), "nexora.events.publication", publisherEnvelope()); !errors.Is(err, context.DeadlineExceeded) {
		t.Fatalf("slow Publish() error = %v", err)
	}

	contextCanceled, cancel := context.WithCancel(context.Background())
	cancel()
	if _, err := publisher.Publish(contextCanceled, "nexora.events.publication", publisherEnvelope()); !errors.Is(err, context.Canceled) {
		t.Fatalf("canceled Publish() error = %v", err)
	}
}

type jetStreamFunc func(context.Context, *nats.Msg) (*nats.PubAck, error)

func (function jetStreamFunc) Publish(ctx context.Context, message *nats.Msg) (*nats.PubAck, error) {
	return function(ctx, message)
}

func publisherEnvelope() domain.EventEnvelope {
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
		PayloadDigest:        "sha256:1111111111111111111111111111111111111111111111111111111111",
		SafePayload:          map[string]any{"safeDisplay": map[string]any{"label": "Publication invalidated", "status": "queued"}},
		OccurredAt:           time.Date(2026, 8, 11, 0, 0, 0, 0, time.UTC),
		SchemaVersion:        domain.SchemaVersion,
	}
}
