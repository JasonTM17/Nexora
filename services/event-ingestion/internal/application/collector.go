package application

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/domain"
)

type Authorizer interface {
	Authorize(context.Context, string, domain.EventEnvelope) (domain.Authorization, error)
}

type Limiter interface {
	Allow(string, time.Time) bool
}

type Publisher interface {
	Publish(context.Context, string, domain.EventEnvelope) (domain.PublishAck, error)
}

type Collector struct {
	authorizer Authorizer
	limiter    Limiter
	publisher  Publisher
	now        func() time.Time
}

func NewCollector(authorizer Authorizer, limiter Limiter, publisher Publisher, now func() time.Time) (*Collector, error) {
	if authorizer == nil || limiter == nil || publisher == nil {
		return nil, fmt.Errorf("collector dependencies are required")
	}
	if now == nil {
		now = time.Now
	}
	return &Collector{authorizer: authorizer, limiter: limiter, publisher: publisher, now: now}, nil
}

func (collector *Collector) Ingest(ctx context.Context, credential string, envelope domain.EventEnvelope) (domain.PublishReceipt, error) {
	if ctx == nil {
		return domain.PublishReceipt{}, fmt.Errorf("ingestion context is required")
	}
	authorization, err := collector.authorizer.Authorize(ctx, credential, envelope)
	if err != nil {
		return domain.PublishReceipt{}, fmt.Errorf("%w: %v", domain.ErrUnauthorized, err)
	}
	now := collector.now()
	if err := domain.ValidateAuthorizedEnvelope(now, authorization, envelope); err != nil {
		if errors.Is(err, domain.ErrUnownedSubject) {
			return domain.PublishReceipt{}, fmt.Errorf("%w: %v", domain.ErrUnauthorized, err)
		}
		return domain.PublishReceipt{}, err
	}
	limitKey := authorization.OrganizationID + ":" + authorization.SubjectID
	if !collector.limiter.Allow(limitKey, now) {
		return domain.PublishReceipt{}, domain.ErrRateLimited
	}
	route, _ := envelope.EventType.Route()
	ack, err := collector.publisher.Publish(ctx, route.NATSSubject, envelope)
	if err != nil {
		return domain.PublishReceipt{}, fmt.Errorf("%w: %v", domain.ErrPublish, err)
	}
	return domain.PublishReceipt{
		EventID:   envelope.EventID,
		Subject:   route.NATSSubject,
		Stream:    ack.Stream,
		Sequence:  ack.Sequence,
		Duplicate: ack.Duplicate,
	}, nil
}
