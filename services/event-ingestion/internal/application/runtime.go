package application

import (
	"fmt"
	"time"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/config"
	"github.com/jasontm17/nexora/services/event-ingestion/internal/infrastructure"
	"github.com/nats-io/nats.go"
)

// IngestionRuntime owns only local process dependencies. Its route is enabled
// only after Spring has independently re-authorized the caller's candidate.
type IngestionRuntime struct {
	Collector      *Collector
	connection     *nats.Conn
	publishTimeout time.Duration
}

func NewIngestionRuntime(settings config.Config) (*IngestionRuntime, error) {
	if !settings.IngestionEnabled() {
		return nil, fmt.Errorf("event ingestion runtime configuration is incomplete")
	}
	connection, err := nats.Connect(settings.NATSURL, nats.Timeout(settings.PublishTimeout))
	if err != nil {
		return nil, fmt.Errorf("connect event ingestion NATS: %w", err)
	}
	jetStream, err := connection.JetStream()
	if err != nil {
		connection.Close()
		return nil, fmt.Errorf("create event ingestion JetStream client: %w", err)
	}
	publisher, err := infrastructure.NewJetStreamPublisher(jetStream, settings.PublishTimeout)
	if err != nil {
		connection.Close()
		return nil, err
	}
	authorizer, err := infrastructure.NewSpringAdmissionAuthorizer(settings.AdmissionURL, settings.PublishTimeout)
	if err != nil {
		connection.Close()
		return nil, err
	}
	// M6-R01: Prefer Redis-backed limiter for multi-replica deployments.
	// Falls back to in-memory fixed-window when Redis is not configured.
	var limiter Limiter
	if redisClient := infrastructure.RedisClientFromEnv(); redisClient != nil {
		limiter = infrastructure.NewRedisSlidingWindowLimiter(redisClient, settings.RateLimitPerMinute, time.Minute, "rl:ingest")
	}
	if limiter == nil {
		limiter = infrastructure.NewFixedWindowLimiter(settings.RateLimitPerMinute, time.Minute, settings.RateLimitKeys)
	}
	collector, err := NewCollector(authorizer, limiter, publisher, time.Now)
	if err != nil {
		connection.Close()
		return nil, err
	}
	return &IngestionRuntime{Collector: collector, connection: connection, publishTimeout: settings.PublishTimeout}, nil
}

func (runtime *IngestionRuntime) Close() error {
	if runtime == nil || runtime.connection == nil {
		return nil
	}
	defer runtime.connection.Close()
	if err := runtime.connection.FlushTimeout(runtime.publishTimeout); err != nil {
		return fmt.Errorf("flush event ingestion NATS: %w", err)
	}
	return nil
}
