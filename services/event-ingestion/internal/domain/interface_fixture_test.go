package domain

import (
	"bytes"
	_ "embed"
	"encoding/json"
	"errors"
	"io"
	"testing"
)

//go:embed testdata/v1/publication-invalidated.json
var publicationInvalidatedFixture []byte

func TestFrozenPublicationFixtureMatchesLocalEnvelopeBoundary(t *testing.T) {
	decoder := json.NewDecoder(bytes.NewReader(publicationInvalidatedFixture))
	decoder.UseNumber()
	var envelope EventEnvelope
	if err := decoder.Decode(&envelope); err != nil {
		t.Fatalf("Decode() error = %v", err)
	}
	if err := decoder.Decode(&struct{}{}); !errors.Is(err, io.EOF) {
		t.Fatalf("fixture contains another JSON value: %v", err)
	}
	if err := ValidateEnvelope(envelope); err != nil {
		t.Fatalf("ValidateEnvelope() error = %v", err)
	}
	route, ok := envelope.EventType.Route()
	if !ok || route.NATSSubject != "nexora.events.publication" {
		t.Fatalf("Route() = %#v, %t", route, ok)
	}
}
