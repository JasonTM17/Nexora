package infrastructure

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/domain"
	"github.com/nats-io/nats.go"
)

// JetStreamPublisher waits for the server's durable publish acknowledgement.
// The caller must provide a connection configured by the later runtime-wiring
// packet; this package neither opens a network connection nor carries secrets.
type JetStreamPublisher struct {
	jetStream jetStream
}

type jetStream interface {
	PublishMsg(*nats.Msg, ...nats.PubOpt) (*nats.PubAck, error)
}

func NewJetStreamPublisher(jetStream jetStream) (*JetStreamPublisher, error) {
	if jetStream == nil {
		return nil, fmt.Errorf("jetstream publisher is required")
	}
	return &JetStreamPublisher{jetStream: jetStream}, nil
}

func (publisher *JetStreamPublisher) Publish(ctx context.Context, subject string, envelope domain.EventEnvelope) (domain.PublishAck, error) {
	if publisher == nil || publisher.jetStream == nil || ctx == nil {
		return domain.PublishAck{}, fmt.Errorf("jetstream publisher context is required")
	}
	route, ok := envelope.EventType.Route()
	if !ok || subject != route.NATSSubject {
		return domain.PublishAck{}, fmt.Errorf("jetstream publish subject is not authorized for %q", envelope.EventType)
	}
	payload, err := json.Marshal(envelope)
	if err != nil {
		return domain.PublishAck{}, fmt.Errorf("marshal canonical event: %w", err)
	}
	message := nats.NewMsg(subject)
	message.Data = payload
	message.Header.Set(nats.MsgIdHdr, envelope.EventID)
	message.Header.Set("Nexora-Schema-Version", envelope.SchemaVersion)
	ack, err := publisher.jetStream.PublishMsg(message, nats.Context(ctx))
	if err != nil {
		return domain.PublishAck{}, fmt.Errorf("jetstream publish: %w", err)
	}
	if ack == nil || ack.Stream == "" || ack.Sequence == 0 {
		return domain.PublishAck{}, fmt.Errorf("jetstream returned an incomplete publish acknowledgement")
	}
	return domain.PublishAck{Stream: ack.Stream, Sequence: ack.Sequence, Duplicate: ack.Duplicate}, nil
}
