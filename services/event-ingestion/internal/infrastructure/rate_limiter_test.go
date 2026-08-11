package infrastructure

import (
	"testing"
	"time"
)

func TestFixedWindowLimiterBoundsRequestsAndRetainedKeys(t *testing.T) {
	now := time.Date(2026, 8, 11, 0, 0, 0, 0, time.UTC)
	limiter := NewFixedWindowLimiter(2, time.Minute, 2)
	if limiter == nil {
		t.Fatal("NewFixedWindowLimiter() returned nil")
	}
	for index := 0; index < 2; index++ {
		if !limiter.Allow("alpha", now) {
			t.Fatalf("request %d was rejected", index+1)
		}
	}
	if limiter.Allow("alpha", now) {
		t.Fatal("over-limit request was allowed")
	}
	if !limiter.Allow("beta", now) {
		t.Fatal("independent key was rejected")
	}
	if limiter.Allow("gamma", now) {
		t.Fatal("new key was allowed after cache limit")
	}
	if !limiter.Allow("gamma", now.Add(time.Minute)) {
		t.Fatal("expired entries did not release capacity")
	}
}

func TestFixedWindowLimiterFailsClosedForInvalidConfigurationOrKey(t *testing.T) {
	if NewFixedWindowLimiter(0, time.Minute, 1) != nil {
		t.Fatal("invalid limiter configuration was accepted")
	}
	limiter := NewFixedWindowLimiter(1, time.Minute, 1)
	if limiter.Allow("", time.Now()) {
		t.Fatal("empty principal key was allowed")
	}
}
