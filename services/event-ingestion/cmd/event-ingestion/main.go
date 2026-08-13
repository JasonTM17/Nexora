package main

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/application"
	"github.com/jasontm17/nexora/services/event-ingestion/internal/config"
	"github.com/jasontm17/nexora/services/event-ingestion/internal/transport"
)

func main() {
	if len(os.Args) == 2 && os.Args[1] == "--healthcheck" {
		healthcheck()
		return
	}

	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	settings, err := config.Load(os.LookupEnv)
	if err != nil {
		logger.Error("event ingestion configuration is invalid", "error", err)
		os.Exit(1)
	}

	options := make([]transport.HandlerOption, 0, 1)
	if settings.IngestionEnabled() {
		runtime, err := application.NewIngestionRuntime(settings)
		if err != nil {
			logger.Error("event ingestion dependencies are unavailable", "error", err)
			os.Exit(1)
		}
		defer func() {
			if err := runtime.Close(); err != nil {
				logger.Warn("event ingestion dependencies did not close cleanly", "error", err)
			}
		}()
		options = append(options,
			transport.WithEventIngestion(runtime.Collector, settings.BodyLimitBytes),
			transport.WithIngestionConcurrency(settings.MaxConcurrency),
		)
	}

	server := application.NewServer(settings, logger, options...)
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	if err := server.Run(ctx); err != nil && !errors.Is(err, context.Canceled) {
		logger.Error("event ingestion server stopped unexpectedly", "error", err)
		os.Exit(1)
	}
}

func healthcheck() {
	settings, err := config.Load(os.LookupEnv)
	if err != nil {
		os.Exit(1)
	}
	client := http.Client{
		Timeout: time.Second,
		CheckRedirect: func(_ *http.Request, _ []*http.Request) error {
			return http.ErrUseLastResponse
		},
	}
	if err := checkHealth(settings, &client); err != nil {
		os.Exit(1)
	}
}

func checkHealth(settings config.Config, client *http.Client) error {
	if client == nil {
		return fmt.Errorf("healthcheck HTTP client is required")
	}
	response, err := client.Get("http://" + settings.Address + "/healthz")
	if err != nil {
		return fmt.Errorf("request health endpoint: %w", err)
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		return fmt.Errorf("health endpoint returned HTTP %d", response.StatusCode)
	}
	return nil
}
