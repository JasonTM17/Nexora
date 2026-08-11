package transport

import (
	"net/http"
	"net/http/httptest"
	"testing"
)

type readinessStub bool

func (stub readinessStub) IsReady() bool { return bool(stub) }

func TestHealthAndReadinessEndpoints(t *testing.T) {
	tests := []struct {
		name       string
		path       string
		ready      readinessStub
		wantStatus int
	}{
		{name: "live", path: "/healthz", ready: false, wantStatus: http.StatusOK},
		{name: "ready", path: "/readyz", ready: true, wantStatus: http.StatusOK},
		{name: "not ready", path: "/readyz", ready: false, wantStatus: http.StatusServiceUnavailable},
		{name: "ingestion unavailable until authorized", path: "/v1/events", ready: true, wantStatus: http.StatusNotFound},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			request := httptest.NewRequest(http.MethodGet, test.path, nil)
			response := httptest.NewRecorder()
			NewHandler(test.ready).ServeHTTP(response, request)
			if response.Code != test.wantStatus {
				t.Fatalf("status = %d, want %d", response.Code, test.wantStatus)
			}
			if response.Header().Get("Cache-Control") != "no-store" && test.path != "/v1/events" {
				t.Fatalf("Cache-Control = %q", response.Header().Get("Cache-Control"))
			}
		})
	}
}
