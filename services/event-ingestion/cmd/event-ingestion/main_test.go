package main

import (
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/config"
)

func TestCheckHealth(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name       string
		statusCode int
		wantError  bool
	}{
		{name: "healthy", statusCode: http.StatusOK},
		{name: "unhealthy", statusCode: http.StatusServiceUnavailable, wantError: true},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			server := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
				if request.URL.Path != "/healthz" {
					t.Errorf("request path = %q, want /healthz", request.URL.Path)
				}
				writer.WriteHeader(test.statusCode)
			}))
			defer server.Close()

			serverURL, err := url.Parse(server.URL)
			if err != nil {
				t.Fatalf("parse server URL: %v", err)
			}
			err = checkHealth(config.Config{Address: serverURL.Host}, server.Client())
			if (err != nil) != test.wantError {
				t.Fatalf("checkHealth() error = %v, wantError %t", err, test.wantError)
			}
		})
	}
}

func TestCheckHealthRequiresClient(t *testing.T) {
	t.Parallel()
	if err := checkHealth(config.Config{Address: "127.0.0.1:18080"}, nil); err == nil {
		t.Fatal("checkHealth() error = nil, want non-nil")
	}
}
