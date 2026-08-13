package transport

import (
	"fmt"
	"net/http"
	"sync"
	"time"
)

const (
	outcomeAccepted     = "accepted"
	outcomeUnauthorized = "unauthorized"
	outcomeInvalid      = "invalid"
	outcomeBodyTooLarge = "body_too_large"
	outcomeRateLimited  = "rate_limited"
	outcomeUnavailable  = "unavailable"
	outcomeOverloaded   = "overloaded"
	outcomeInternal     = "internal"
)

var metricOutcomes = []string{
	outcomeAccepted,
	outcomeUnauthorized,
	outcomeInvalid,
	outcomeBodyTooLarge,
	outcomeRateLimited,
	outcomeUnavailable,
	outcomeOverloaded,
	outcomeInternal,
}

// ingestionMetrics retains only bounded aggregate request results. It never
// records credentials, tenant identifiers, event IDs, trace IDs, or payloads.
type ingestionMetrics struct {
	mu                  sync.Mutex
	inFlight            uint64
	requests            map[string]uint64
	durationCount       uint64
	durationNanoseconds uint64
}

func newIngestionMetrics() *ingestionMetrics {
	requests := make(map[string]uint64, len(metricOutcomes))
	for _, outcome := range metricOutcomes {
		requests[outcome] = 0
	}
	return &ingestionMetrics{requests: requests}
}

func (metrics *ingestionMetrics) begin() time.Time {
	metrics.mu.Lock()
	metrics.inFlight++
	metrics.mu.Unlock()
	return time.Now()
}

func (metrics *ingestionMetrics) finish(status int, started time.Time) {
	duration := time.Since(started)
	if duration < 0 {
		duration = 0
	}
	metrics.mu.Lock()
	defer metrics.mu.Unlock()
	if metrics.inFlight > 0 {
		metrics.inFlight--
	}
	metrics.requests[outcomeForStatus(status)]++
	metrics.durationCount++
	metrics.durationNanoseconds += uint64(duration)
}

func (metrics *ingestionMetrics) finishAs(outcome string, started time.Time) {
	duration := time.Since(started)
	if duration < 0 {
		duration = 0
	}
	metrics.mu.Lock()
	defer metrics.mu.Unlock()
	if metrics.inFlight > 0 {
		metrics.inFlight--
	}
	metrics.requests[outcome]++
	metrics.durationCount++
	metrics.durationNanoseconds += uint64(duration)
}

func (metrics *ingestionMetrics) writePrometheus(writer http.ResponseWriter, _ *http.Request) {
	metrics.mu.Lock()
	inFlight := metrics.inFlight
	durationCount := metrics.durationCount
	durationNanoseconds := metrics.durationNanoseconds
	requests := make(map[string]uint64, len(metrics.requests))
	for outcome, count := range metrics.requests {
		requests[outcome] = count
	}
	metrics.mu.Unlock()

	writer.Header().Set("Cache-Control", "no-store")
	writer.Header().Set("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
	writer.Header().Set("X-Content-Type-Options", "nosniff")
	_, _ = fmt.Fprintln(writer, "# HELP nexora_event_ingestion_http_requests_total Bounded ingestion HTTP outcomes.")
	_, _ = fmt.Fprintln(writer, "# TYPE nexora_event_ingestion_http_requests_total counter")
	for _, outcome := range metricOutcomes {
		_, _ = fmt.Fprintf(writer, "nexora_event_ingestion_http_requests_total{outcome=%q} %d\n", outcome, requests[outcome])
	}
	_, _ = fmt.Fprintln(writer, "# HELP nexora_event_ingestion_http_in_flight_requests Current ingestion requests.")
	_, _ = fmt.Fprintln(writer, "# TYPE nexora_event_ingestion_http_in_flight_requests gauge")
	_, _ = fmt.Fprintf(writer, "nexora_event_ingestion_http_in_flight_requests %d\n", inFlight)
	_, _ = fmt.Fprintln(writer, "# HELP nexora_event_ingestion_http_request_duration_seconds Ingestion request duration.")
	_, _ = fmt.Fprintln(writer, "# TYPE nexora_event_ingestion_http_request_duration_seconds summary")
	_, _ = fmt.Fprintf(writer, "nexora_event_ingestion_http_request_duration_seconds_count %d\n", durationCount)
	_, _ = fmt.Fprintf(writer, "nexora_event_ingestion_http_request_duration_seconds_sum %.9f\n", float64(durationNanoseconds)/float64(time.Second))
}

func outcomeForStatus(status int) string {
	switch status {
	case http.StatusAccepted:
		return outcomeAccepted
	case http.StatusUnauthorized:
		return outcomeUnauthorized
	case http.StatusBadRequest, http.StatusUnprocessableEntity:
		return outcomeInvalid
	case http.StatusRequestEntityTooLarge:
		return outcomeBodyTooLarge
	case http.StatusTooManyRequests:
		return outcomeRateLimited
	case http.StatusServiceUnavailable:
		return outcomeUnavailable
	default:
		return outcomeInternal
	}
}
