package infrastructure

import (
	"strings"
	"sync"
	"time"
)

// FixedWindowLimiter bounds both requests per principal and the number of
// principals retained in memory. A saturated principal cache fails closed.
type FixedWindowLimiter struct {
	mu          sync.Mutex
	limit       int
	window      time.Duration
	maximumKeys int
	entries     map[string]windowEntry
	lastSweep   time.Time
}

type windowEntry struct {
	startedAt time.Time
	used      int
}

func NewFixedWindowLimiter(limit int, window time.Duration, maximumKeys int) *FixedWindowLimiter {
	if limit < 1 || window <= 0 || maximumKeys < 1 {
		return nil
	}
	return &FixedWindowLimiter{
		limit:       limit,
		window:      window,
		maximumKeys: maximumKeys,
		entries:     make(map[string]windowEntry),
	}
}

func (limiter *FixedWindowLimiter) Allow(key string, now time.Time) bool {
	if limiter == nil || strings.TrimSpace(key) == "" || now.IsZero() {
		return false
	}
	limiter.mu.Lock()
	defer limiter.mu.Unlock()
	limiter.sweep(now)

	entry, found := limiter.entries[key]
	if !found {
		if len(limiter.entries) >= limiter.maximumKeys {
			return false
		}
		entry = windowEntry{startedAt: now}
	}
	if now.Before(entry.startedAt) {
		return false
	}
	if now.Sub(entry.startedAt) >= limiter.window {
		entry = windowEntry{startedAt: now}
	}
	if entry.used >= limiter.limit {
		limiter.entries[key] = entry
		return false
	}
	entry.used++
	limiter.entries[key] = entry
	return true
}

func (limiter *FixedWindowLimiter) sweep(now time.Time) {
	if !limiter.lastSweep.IsZero() && now.Sub(limiter.lastSweep) < limiter.window {
		return
	}
	for key, entry := range limiter.entries {
		if now.Sub(entry.startedAt) >= limiter.window {
			delete(limiter.entries, key)
		}
	}
	limiter.lastSweep = now
}
