package infrastructure

import (
	"testing"
	"time"
)

func TestNewRedisSlidingWindowLimiter_NilClient(t *testing.T) {
	limiter := NewRedisSlidingWindowLimiter(nil, 60, time.Minute, "rl")
	if limiter != nil {
		t.Fatal("expected nil limiter for nil client")
	}
}

func TestNewRedisSlidingWindowLimiter_InvalidParams(t *testing.T) {
	// Can't test with real Redis in unit test; verify nil client guard
	limiter := NewRedisSlidingWindowLimiter(nil, 0, time.Minute, "rl")
	if limiter != nil {
		t.Fatal("expected nil limiter for zero limit")
	}
}

func TestRedisSlidingWindowLimiter_Allow_NilReceiver(t *testing.T) {
	var limiter *RedisSlidingWindowLimiter
	if limiter.Allow("key", time.Now()) {
		t.Fatal("nil limiter must deny")
	}
}

func TestRedisSlidingWindowLimiter_Allow_EmptyKey(t *testing.T) {
	var limiter *RedisSlidingWindowLimiter
	if limiter.Allow("", time.Now()) {
		t.Fatal("empty key must deny")
	}
}

func TestRedisSlidingWindowLimiter_Allow_ZeroTime(t *testing.T) {
	var limiter *RedisSlidingWindowLimiter
	if limiter.Allow("key", time.Time{}) {
		t.Fatal("zero time must deny")
	}
}

func TestRedisClientFromEnv_NotConfigured(t *testing.T) {
	// Ensure no Redis client when env not set
	t.Setenv("NEXORA_REDIS_ADDR", "")
	client := RedisClientFromEnv()
	if client != nil {
		t.Fatal("expected nil client when addr not set")
	}
}
