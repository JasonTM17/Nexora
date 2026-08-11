package transport

import (
	"net/http"
)

// Readiness reports only this process's ability to receive requests.
type Readiness interface {
	IsReady() bool
}

// NewHandler provides process health endpoints. Event ingestion is intentionally
// not registered until authorization, validation, and publish semantics exist.
func NewHandler(readiness Readiness) http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", func(writer http.ResponseWriter, _ *http.Request) {
		writer.Header().Set("Cache-Control", "no-store")
		writer.WriteHeader(http.StatusOK)
	})
	mux.HandleFunc("GET /readyz", func(writer http.ResponseWriter, _ *http.Request) {
		writer.Header().Set("Cache-Control", "no-store")
		if !readiness.IsReady() {
			writer.WriteHeader(http.StatusServiceUnavailable)
			return
		}
		writer.WriteHeader(http.StatusOK)
	})
	return mux
}
