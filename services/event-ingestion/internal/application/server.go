package application

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"time"

	"github.com/jasontm17/nexora/services/event-ingestion/internal/config"
	"github.com/jasontm17/nexora/services/event-ingestion/internal/infrastructure"
	"github.com/jasontm17/nexora/services/event-ingestion/internal/transport"
)

// Server owns one bounded HTTP listener and its graceful shutdown lifecycle.
type Server struct {
	config     config.Config
	logger     *slog.Logger
	readiness  *infrastructure.Readiness
	listen     func(network, address string) (net.Listener, error)
	httpServer *http.Server
}

func NewServer(settings config.Config, logger *slog.Logger) *Server {
	if logger == nil {
		logger = slog.Default()
	}
	readiness := infrastructure.NewReadiness()
	return &Server{
		config:    settings,
		logger:    logger,
		readiness: readiness,
		listen:    net.Listen,
		httpServer: &http.Server{
			Addr:              settings.Address,
			Handler:           transport.NewHandler(readiness),
			ReadHeaderTimeout: settings.ReadHeaderTimeout,
			ReadTimeout:       settings.ReadTimeout,
			WriteTimeout:      settings.WriteTimeout,
			IdleTimeout:       settings.IdleTimeout,
			MaxHeaderBytes:    16 * 1024,
		},
	}
}

// Run listens until context cancellation and then shuts down inside the
// configured deadline. It creates no worker goroutines beyond net/http.
func (server *Server) Run(ctx context.Context) error {
	if ctx == nil {
		return fmt.Errorf("run context is required")
	}
	listener, err := server.listen("tcp", server.config.Address)
	if err != nil {
		return fmt.Errorf("listen on %s: %w", server.config.Address, err)
	}
	errorsFromServer := make(chan error, 1)
	go func() {
		server.logger.Info("event ingestion server listening", "address", server.config.Address)
		errorsFromServer <- server.httpServer.Serve(listener)
	}()

	select {
	case err := <-errorsFromServer:
		if errors.Is(err, http.ErrServerClosed) {
			return nil
		}
		return err
	case <-ctx.Done():
		server.readiness.MarkUnready()
		shutdownContext, cancel := context.WithTimeout(context.Background(), server.config.ShutdownTimeout)
		defer cancel()
		if err := server.httpServer.Shutdown(shutdownContext); err != nil {
			return fmt.Errorf("shutdown event ingestion server: %w", err)
		}
		select {
		case err := <-errorsFromServer:
			if err != nil && !errors.Is(err, http.ErrServerClosed) {
				return err
			}
		case <-time.After(server.config.ShutdownTimeout):
			return fmt.Errorf("event ingestion server did not stop within %s", server.config.ShutdownTimeout)
		}
		return nil
	}
}
