package config

import (
	"fmt"
	"net"
	"strconv"
	"strings"
	"time"
)

const (
	defaultAddress       = "127.0.0.1:18080"
	defaultBodyLimit     = int64(64 * 1024)
	minimumBodyLimit     = int64(1024)
	maximumBodyLimit     = int64(1024 * 1024)
	defaultHeaderTimeout = 2 * time.Second
	defaultReadTimeout   = 5 * time.Second
	defaultWriteTimeout  = 5 * time.Second
	defaultIdleTimeout   = 30 * time.Second
	defaultShutdown      = 10 * time.Second
)

// LookupEnv matches os.LookupEnv and keeps configuration parsing testable.
type LookupEnv func(string) (string, bool)

// Config contains only bounded HTTP lifecycle settings. NATS and authentication
// configuration are deliberately absent until their contract-owned packets.
type Config struct {
	Address           string
	BodyLimitBytes    int64
	ReadHeaderTimeout time.Duration
	ReadTimeout       time.Duration
	WriteTimeout      time.Duration
	IdleTimeout       time.Duration
	ShutdownTimeout   time.Duration
}

// Load returns local-safe defaults and rejects invalid overrides before serving.
func Load(lookup LookupEnv) (Config, error) {
	if lookup == nil {
		return Config{}, fmt.Errorf("environment lookup is required")
	}

	address, err := stringValue(lookup, "NEXORA_EVENT_INGESTION_ADDR", defaultAddress)
	if err != nil {
		return Config{}, err
	}
	host, port, err := net.SplitHostPort(address)
	if err != nil {
		return Config{}, fmt.Errorf("NEXORA_EVENT_INGESTION_ADDR must be host:port: %w", err)
	}
	parsedHost := net.ParseIP(host)
	if parsedHost == nil || !parsedHost.IsLoopback() {
		return Config{}, fmt.Errorf("NEXORA_EVENT_INGESTION_ADDR must use a literal loopback IP")
	}
	parsedPort, err := strconv.ParseUint(port, 10, 16)
	if err != nil || parsedPort == 0 {
		return Config{}, fmt.Errorf("NEXORA_EVENT_INGESTION_ADDR must use TCP port 1..65535")
	}

	bodyLimit, err := int64Value(lookup, "NEXORA_EVENT_INGESTION_BODY_LIMIT_BYTES", defaultBodyLimit)
	if err != nil {
		return Config{}, err
	}
	if bodyLimit < minimumBodyLimit || bodyLimit > maximumBodyLimit {
		return Config{}, fmt.Errorf("NEXORA_EVENT_INGESTION_BODY_LIMIT_BYTES must be between %d and %d", minimumBodyLimit, maximumBodyLimit)
	}

	headerTimeout, err := durationValue(lookup, "NEXORA_EVENT_INGESTION_READ_HEADER_TIMEOUT", defaultHeaderTimeout)
	if err != nil {
		return Config{}, err
	}
	readTimeout, err := durationValue(lookup, "NEXORA_EVENT_INGESTION_READ_TIMEOUT", defaultReadTimeout)
	if err != nil {
		return Config{}, err
	}
	writeTimeout, err := durationValue(lookup, "NEXORA_EVENT_INGESTION_WRITE_TIMEOUT", defaultWriteTimeout)
	if err != nil {
		return Config{}, err
	}
	idleTimeout, err := durationValue(lookup, "NEXORA_EVENT_INGESTION_IDLE_TIMEOUT", defaultIdleTimeout)
	if err != nil {
		return Config{}, err
	}
	shutdownTimeout, err := durationValue(lookup, "NEXORA_EVENT_INGESTION_SHUTDOWN_TIMEOUT", defaultShutdown)
	if err != nil {
		return Config{}, err
	}

	return Config{
		Address:           address,
		BodyLimitBytes:    bodyLimit,
		ReadHeaderTimeout: headerTimeout,
		ReadTimeout:       readTimeout,
		WriteTimeout:      writeTimeout,
		IdleTimeout:       idleTimeout,
		ShutdownTimeout:   shutdownTimeout,
	}, nil
}

func stringValue(lookup LookupEnv, name, fallback string) (string, error) {
	value, ok := lookup(name)
	if !ok {
		return fallback, nil
	}
	value = strings.TrimSpace(value)
	if value == "" {
		return "", fmt.Errorf("%s must not be empty", name)
	}
	return value, nil
}

func int64Value(lookup LookupEnv, name string, fallback int64) (int64, error) {
	value, ok := lookup(name)
	if !ok {
		return fallback, nil
	}
	parsed, err := strconv.ParseInt(strings.TrimSpace(value), 10, 64)
	if err != nil {
		return 0, fmt.Errorf("%s must be an integer: %w", name, err)
	}
	return parsed, nil
}

func durationValue(lookup LookupEnv, name string, fallback time.Duration) (time.Duration, error) {
	value, ok := lookup(name)
	if !ok {
		return fallback, nil
	}
	parsed, err := time.ParseDuration(strings.TrimSpace(value))
	if err != nil || parsed <= 0 {
		return 0, fmt.Errorf("%s must be a positive Go duration", name)
	}
	return parsed, nil
}
