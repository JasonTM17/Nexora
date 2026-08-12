package transport

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strings"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/domain"
)

const maximumCredentialBytes = 4096

// Readiness reports only this process's ability to receive requests.
type Readiness interface {
	IsReady() bool
}

// EventIngestor admits only a validated event through a trusted collector.
// It intentionally has no concrete authorization implementation in this
// transport package: credential issuance remains owned by the backend.
type EventIngestor interface {
	Ingest(context.Context, string, domain.EventEnvelope) (domain.PublishReceipt, error)
}

type handlerOptions struct {
	ingestor  EventIngestor
	bodyLimit int64
	metrics   *ingestionMetrics
}

type HandlerOption func(*handlerOptions)

// WithEventIngestion registers the HTTP event endpoint only when an owning
// backend has provided a trusted collector and a bounded body limit.
func WithEventIngestion(ingestor EventIngestor, bodyLimit int64) HandlerOption {
	return func(options *handlerOptions) {
		if ingestor != nil && bodyLimit > 0 {
			options.ingestor = ingestor
			options.bodyLimit = bodyLimit
		}
	}
}

// NewHandler provides process health endpoints. The event route is unavailable
// unless an explicit trusted ingestion dependency is supplied.
func NewHandler(readiness Readiness, options ...HandlerOption) http.Handler {
	settings := handlerOptions{metrics: newIngestionMetrics()}
	for _, option := range options {
		if option != nil {
			option(&settings)
		}
	}
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
	mux.HandleFunc("GET /metrics", settings.metrics.writePrometheus)
	if settings.ingestor != nil {
		mux.HandleFunc("POST /v1/events", func(writer http.ResponseWriter, request *http.Request) {
			handleObservedIngest(writer, request, settings.ingestor, settings.bodyLimit, settings.metrics)
		})
	}
	return mux
}

func handleObservedIngest(writer http.ResponseWriter, request *http.Request, ingestor EventIngestor, bodyLimit int64, metrics *ingestionMetrics) {
	started := metrics.begin()
	recorder := &statusRecorder{ResponseWriter: writer}
	defer func() { metrics.finish(recorder.statusCode(), started) }()
	handleIngest(recorder, request, ingestor, bodyLimit)
}

type statusRecorder struct {
	http.ResponseWriter
	status int
}

func (recorder *statusRecorder) WriteHeader(status int) {
	if recorder.status != 0 {
		return
	}
	recorder.status = status
	recorder.ResponseWriter.WriteHeader(status)
}

func (recorder *statusRecorder) Write(body []byte) (int, error) {
	if recorder.status == 0 {
		recorder.WriteHeader(http.StatusOK)
	}
	return recorder.ResponseWriter.Write(body)
}

func (recorder *statusRecorder) statusCode() int {
	if recorder.status == 0 {
		return http.StatusOK
	}
	return recorder.status
}

func handleIngest(writer http.ResponseWriter, request *http.Request, ingestor EventIngestor, bodyLimit int64) {
	credential, ok := bearerCredential(request.Header.Get("Authorization"))
	if !ok {
		writeError(writer, http.StatusUnauthorized, "UNAUTHORIZED")
		return
	}
	envelope, err := decodeEnvelope(writer, request, bodyLimit)
	if err != nil {
		if isTooLarge(err) {
			writeError(writer, http.StatusRequestEntityTooLarge, "BODY_TOO_LARGE")
			return
		}
		writeError(writer, http.StatusBadRequest, "INVALID_EVENT")
		return
	}
	_, err = ingestor.Ingest(request.Context(), credential, envelope)
	if err != nil {
		switch {
		case errors.Is(err, domain.ErrUnauthorized):
			writeError(writer, http.StatusUnauthorized, "UNAUTHORIZED")
		case errors.Is(err, domain.ErrRateLimited):
			writeError(writer, http.StatusTooManyRequests, "RATE_LIMITED")
		case errors.Is(err, domain.ErrInvalidEnvelope):
			writeError(writer, http.StatusUnprocessableEntity, "INVALID_EVENT")
		default:
			writeError(writer, http.StatusServiceUnavailable, "INGESTION_UNAVAILABLE")
		}
		return
	}
	writer.Header().Set("Cache-Control", "no-store")
	writer.Header().Set("Content-Type", "application/json; charset=utf-8")
	writer.WriteHeader(http.StatusAccepted)
	_ = json.NewEncoder(writer).Encode(struct {
		EventID string `json:"eventId"`
	}{EventID: envelope.EventID})
}

func bearerCredential(header string) (string, bool) {
	parts := strings.Fields(header)
	if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") || len(parts[1]) == 0 || len(parts[1]) > maximumCredentialBytes {
		return "", false
	}
	return parts[1], true
}

func decodeEnvelope(writer http.ResponseWriter, request *http.Request, bodyLimit int64) (domain.EventEnvelope, error) {
	if bodyLimit < 1 || !strings.EqualFold(strings.TrimSpace(strings.Split(request.Header.Get("Content-Type"), ";")[0]), "application/json") {
		return domain.EventEnvelope{}, errors.New("unsupported event payload")
	}
	defer request.Body.Close()
	decoder := json.NewDecoder(http.MaxBytesReader(writer, request.Body, bodyLimit))
	decoder.UseNumber()
	decoder.DisallowUnknownFields()
	var envelope domain.EventEnvelope
	if err := decoder.Decode(&envelope); err != nil {
		return domain.EventEnvelope{}, err
	}
	if err := decoder.Decode(&struct{}{}); !errors.Is(err, io.EOF) {
		if err == nil {
			return domain.EventEnvelope{}, errors.New("multiple event documents")
		}
		return domain.EventEnvelope{}, err
	}
	return envelope, nil
}

func isTooLarge(err error) bool {
	var maximum *http.MaxBytesError
	return errors.As(err, &maximum)
}

func writeError(writer http.ResponseWriter, status int, code string) {
	writer.Header().Set("Cache-Control", "no-store")
	writer.Header().Set("Content-Type", "application/json; charset=utf-8")
	writer.WriteHeader(status)
	_ = json.NewEncoder(writer).Encode(struct {
		Code string `json:"code"`
	}{Code: code})
}
