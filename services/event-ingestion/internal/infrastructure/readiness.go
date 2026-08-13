package infrastructure

import "sync/atomic"

// Readiness is local process state only. Later packets may compose it with
// NATS connectivity, but health endpoints never imply durable persistence.
type Readiness struct {
	ready atomic.Bool
}

func NewReadiness() *Readiness {
	status := &Readiness{}
	status.ready.Store(true)
	return status
}

func (status *Readiness) IsReady() bool {
	return status.ready.Load()
}

func (status *Readiness) MarkUnready() {
	status.ready.Store(false)
}
