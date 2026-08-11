package application

import (
	"context"
	"errors"
	"io"
	"log/slog"
	"net"
	"net/http"
	"testing"
	"time"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/config"
)

func TestRunClosesListenerAndMarksUnreadyOnCancellation(t *testing.T) {
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("Listen() error = %v", err)
	}

	server := NewServer(testConfig(), slog.New(slog.NewTextHandler(io.Discard, nil)))
	server.listen = func(network, address string) (net.Listener, error) {
		if network != "tcp" || address != server.config.Address {
			t.Fatalf("listen called with %q %q", network, address)
		}
		return listener, nil
	}

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	errorsFromRun := make(chan error, 1)
	go func() { errorsFromRun <- server.Run(ctx) }()

	readyURL := "http://" + listener.Addr().String() + "/readyz"
	awaitStatus(t, readyURL, http.StatusOK)
	cancel()

	select {
	case err := <-errorsFromRun:
		if err != nil {
			t.Fatalf("Run() error = %v", err)
		}
	case <-time.After(time.Second):
		t.Fatal("Run() did not complete after cancellation")
	}
	if server.readiness.IsReady() {
		t.Fatal("readiness stayed true after shutdown")
	}
}

func TestRunReturnsListenerFailure(t *testing.T) {
	server := NewServer(testConfig(), slog.New(slog.NewTextHandler(io.Discard, nil)))
	listenerErr := errors.New("listener unavailable")
	server.listen = func(string, string) (net.Listener, error) {
		return nil, listenerErr
	}

	err := server.Run(context.Background())
	if !errors.Is(err, listenerErr) {
		t.Fatalf("Run() error = %v", err)
	}
}

func testConfig() config.Config {
	return config.Config{
		Address:           "127.0.0.1:18080",
		BodyLimitBytes:    64 * 1024,
		ReadHeaderTimeout: time.Second,
		ReadTimeout:       time.Second,
		WriteTimeout:      time.Second,
		IdleTimeout:       time.Second,
		ShutdownTimeout:   time.Second,
	}
}

func awaitStatus(t *testing.T, url string, want int) {
	t.Helper()
	client := &http.Client{Timeout: 100 * time.Millisecond}
	deadline := time.Now().Add(time.Second)
	for time.Now().Before(deadline) {
		response, err := client.Get(url)
		if err == nil {
			_ = response.Body.Close()
			if response.StatusCode == want {
				return
			}
		}
		time.Sleep(10 * time.Millisecond)
	}
	t.Fatalf("endpoint %s did not return %d", url, want)
}
