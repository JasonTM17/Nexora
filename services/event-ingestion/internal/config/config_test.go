package config

import (
	"strings"
	"testing"
	"time"
)

func TestLoadUsesBoundedLocalDefaults(t *testing.T) {
	settings, err := Load(func(string) (string, bool) { return "", false })
	if err != nil {
		t.Fatalf("Load() error = %v", err)
	}
	if settings.Address != "127.0.0.1:18080" {
		t.Fatalf("Address = %q", settings.Address)
	}
	if settings.BodyLimitBytes != 64*1024 {
		t.Fatalf("BodyLimitBytes = %d", settings.BodyLimitBytes)
	}
	if settings.ReadHeaderTimeout != 2*time.Second || settings.ShutdownTimeout != 10*time.Second {
		t.Fatalf("unexpected default timeouts: %#v", settings)
	}
	if settings.RateLimitPerMinute != 60 || settings.RateLimitKeys != 10_000 {
		t.Fatalf("unexpected default rate limits: %#v", settings)
	}
	if settings.PublishTimeout != 2*time.Second {
		t.Fatalf("PublishTimeout = %s", settings.PublishTimeout)
	}
	if settings.IngestionEnabled() {
		t.Fatal("default process unexpectedly enables event ingestion")
	}
}

func TestLoadEnablesOnlyCompleteValidatedIngestionDependencies(t *testing.T) {
	settings, err := Load(func(name string) (string, bool) {
		switch name {
		case "NEXORA_EVENT_INGESTION_ADMISSION_URL":
			return "http://127.0.0.1:8080/api/v1/internal/event-admission", true
		case "NEXORA_EVENT_INGESTION_NATS_URL":
			return "nats://127.0.0.1:4222", true
		default:
			return "", false
		}
	})
	if err != nil || !settings.IngestionEnabled() {
		t.Fatalf("Load() = %#v, %v", settings, err)
	}

	for name, value := range map[string]string{
		"NEXORA_EVENT_INGESTION_ADMISSION_URL": "https://example.test/other",
		"NEXORA_EVENT_INGESTION_NATS_URL":      "https://127.0.0.1:4222",
	} {
		t.Run(name, func(t *testing.T) {
			_, err := Load(func(key string) (string, bool) {
				if key == name {
					return value, true
				}
				if key == "NEXORA_EVENT_INGESTION_ADMISSION_URL" {
					return "http://127.0.0.1:8080/api/v1/internal/event-admission", true
				}
				if key == "NEXORA_EVENT_INGESTION_NATS_URL" {
					return "nats://127.0.0.1:4222", true
				}
				return "", false
			})
			if err == nil {
				t.Fatal("Load() accepted unsafe ingestion dependency")
			}
		})
	}
	_, err = Load(func(name string) (string, bool) {
		if name == "NEXORA_EVENT_INGESTION_ADMISSION_URL" {
			return "http://127.0.0.1:8080/api/v1/internal/event-admission", true
		}
		return "", false
	})
	if err == nil {
		t.Fatal("Load() accepted an incomplete ingestion dependency pair")
	}
}

func TestLoadAcceptsLiteralLoopbackAddresses(t *testing.T) {
	for _, address := range []string{"127.0.0.1:65535", "[::1]:18080"} {
		t.Run(address, func(t *testing.T) {
			settings, err := Load(func(name string) (string, bool) {
				if name == "NEXORA_EVENT_INGESTION_ADDR" {
					return address, true
				}
				return "", false
			})
			if err != nil {
				t.Fatalf("Load() error = %v", err)
			}
			if settings.Address != address {
				t.Fatalf("Address = %q, want %q", settings.Address, address)
			}
		})
	}
}

func TestLoadRejectsUnsafeOverrides(t *testing.T) {
	tests := []struct {
		name  string
		key   string
		value string
	}{
		{name: "empty address", key: "NEXORA_EVENT_INGESTION_ADDR", value: " "},
		{name: "missing port", key: "NEXORA_EVENT_INGESTION_ADDR", value: "127.0.0.1"},
		{name: "missing host", key: "NEXORA_EVENT_INGESTION_ADDR", value: ":18080"},
		{name: "wildcard bind", key: "NEXORA_EVENT_INGESTION_ADDR", value: "0.0.0.0:18080"},
		{name: "non loopback bind", key: "NEXORA_EVENT_INGESTION_ADDR", value: "192.0.2.12:18080"},
		{name: "invalid port", key: "NEXORA_EVENT_INGESTION_ADDR", value: "127.0.0.1:not-a-port"},
		{name: "zero port", key: "NEXORA_EVENT_INGESTION_ADDR", value: "127.0.0.1:0"},
		{name: "small body", key: "NEXORA_EVENT_INGESTION_BODY_LIMIT_BYTES", value: "1023"},
		{name: "large body", key: "NEXORA_EVENT_INGESTION_BODY_LIMIT_BYTES", value: "1048577"},
		{name: "invalid timeout", key: "NEXORA_EVENT_INGESTION_READ_TIMEOUT", value: "0s"},
		{name: "small publish timeout", key: "NEXORA_EVENT_INGESTION_PUBLISH_TIMEOUT", value: "500us"},
		{name: "large publish timeout", key: "NEXORA_EVENT_INGESTION_PUBLISH_TIMEOUT", value: "31s"},
		{name: "zero rate limit", key: "NEXORA_EVENT_INGESTION_RATE_LIMIT_PER_MINUTE", value: "0"},
		{name: "large rate limit", key: "NEXORA_EVENT_INGESTION_RATE_LIMIT_PER_MINUTE", value: "10001"},
		{name: "zero rate keys", key: "NEXORA_EVENT_INGESTION_RATE_LIMIT_KEYS", value: "0"},
		{name: "large rate keys", key: "NEXORA_EVENT_INGESTION_RATE_LIMIT_KEYS", value: "100001"},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := Load(func(name string) (string, bool) {
				if name == test.key {
					return test.value, true
				}
				return "", false
			})
			if err == nil || !strings.Contains(err.Error(), test.key) {
				t.Fatalf("Load() error = %v, want %s", err, test.key)
			}
		})
	}
}
