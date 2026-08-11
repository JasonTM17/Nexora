package main

import (
	"context"
	"errors"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/application"
	"github.com/jasontm17/nexora/services/event-ingestion/internal/config"
)

func main() {
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	settings, err := config.Load(os.LookupEnv)
	if err != nil {
		logger.Error("event ingestion configuration is invalid", "error", err)
		os.Exit(1)
	}

	server := application.NewServer(settings, logger)
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	if err := server.Run(ctx); err != nil && !errors.Is(err, context.Canceled) {
		logger.Error("event ingestion server stopped unexpectedly", "error", err)
		os.Exit(1)
	}
}
