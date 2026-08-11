package application

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/domain"
)

var (
	ErrUnauthorized = errors.New("event ingestion unauthorized")
	ErrRateLimited  = errors.New("event ingestion rate limited")
	ErrPublish      = errors.New("event ingestion publish failed")
)

type Authorizer interface {
	Authorize(context.Context, string, domain.EventEnvelope) (domain.Authorization, error)
}

type Limiter interface {
	Allow(string, time.Time) bool
}

type Publisher interface {
	Publish(context.Context, string, domain.EventEnvelope) (PublishAck, error)
}

type PublishAck struct {
	Stream    string
	Sequence  uint64
	Duplicate bool
}

type Receipt struct {
	EventID   string
	Subject   string
	Stream    string
	Sequence  uint64
	Duplicate bool
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

func (collector *Collector) Ingest(ctx context.Context, credential string, envelope domain.EventEnvelope) (Receipt, error) {
	if ctx == nil {
		return Receipt{}, fmt.Errorf("ingestion context is required")
	}
	authorization, err := collector.authorizer.Authorize(ctx, credential, envelope)
	if err != nil {
		return Receipt{}, fmt.Errorf("%w: %v", ErrUnauthorized, err)
	}
	now := collector.now()
	if err := domain.ValidateAuthorizedEnvelope(now, authorization, envelope); err != nil {
		if errors.Is(err, domain.ErrUnownedSubject) {
			return Receipt{}, fmt.Errorf("%w: %v", ErrUnauthorized, err)
		}
		return Receipt{}, err
	}
	limitKey := authorization.OrganizationID + ":" + authorization.SubjectID
	if !collector.limiter.Allow(limitKey, now) {
		return Receipt{}, ErrRateLimited
	}
	route, _ := envelope.EventType.Route()
	ack, err := collector.publisher.Publish(ctx, route.NATSSubject, envelope)
	if err != nil {
		return Receipt{}, fmt.Errorf("%w: %v", ErrPublish, err)
	}
	return Receipt{
		EventID:   envelope.EventID,
		Subject:   route.NATSSubject,
		Stream:    ack.Stream,
		Sequence:  ack.Sequence,
		Duplicate: ack.Duplicate,
	}, nil
}
