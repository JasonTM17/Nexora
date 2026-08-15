package infrastructure

import (
	"context"
	"os"
	"strings"
	"time"

	"github.com/redis/go-redis/v9"
)

// RedisSlidingWindowLimiter provides a shared rate limiter backed by Redis for
// multi-replica deployments. Uses Redis INCR + EXPIRE for atomic sliding-window
// counting. Falls back to deny on Redis failure (fail-closed).
type RedisSlidingWindowLimiter struct {
	client redis.Cmdable
	limit  int
	window time.Duration
	prefix string
}

// NewRedisSlidingWindowLimiter creates a Redis-backed limiter. Returns nil if
// parameters are invalid.
func NewRedisSlidingWindowLimiter(client redis.Cmdable, limit int, window time.Duration, prefix string) *RedisSlidingWindowLimiter {
	if client == nil || limit < 1 || window <= 0 || strings.TrimSpace(prefix) == "" {
		return nil
	}
	return &RedisSlidingWindowLimiter{
		client: client,
		limit:  limit,
		window: window,
		prefix: prefix,
	}
}

// Allow checks whether the key is within the rate limit. Fail-closed: returns
// false on Redis errors.
func (limiter *RedisSlidingWindowLimiter) Allow(key string, now time.Time) bool {
	if limiter == nil || strings.TrimSpace(key) == "" || now.IsZero() {
		return false
	}
	ctx, cancel := context.WithTimeout(context.Background(), 100*time.Millisecond)
	defer cancel()

	redisKey := limiter.prefix + ":" + key
	pipe := limiter.client.Pipeline()
	incr := pipe.Incr(ctx, redisKey)
	pipe.Expire(ctx, redisKey, limiter.window)
	_, err := pipe.Exec(ctx)
	if err != nil {
		return false // fail-closed
	}

	count, err := incr.Result()
	if err != nil {
		return false
	}
	return count <= int64(limiter.limit)
}

// RedisClientFromEnv creates a Redis client from environment configuration.
// Returns nil if Redis is not configured (caller should fall back to in-memory).
func RedisClientFromEnv() redis.Cmdable {
	addr := strings.TrimSpace(os.Getenv("NEXORA_REDIS_ADDR"))
	if addr == "" {
		return nil
	}
	return redis.NewClient(&redis.Options{
		Addr:     addr,
		Password: os.Getenv("NEXORA_REDIS_PASSWORD"),
		DB:       0,
	})
}
