// Package domain contains the Go-owned projection of the frozen canonical
// envelope. This type accepts no input yet; validation and NATS publishing are
// introduced in their dedicated follow-up commit.
package domain

import "time"

// EventEnvelope mirrors the M3-T01 canonical event fields without adding a
// second shared contract source.
type EventEnvelope struct {
	EventID              string         `json:"eventId"`
	EventType            string         `json:"eventType"`
	EventVersion         int64          `json:"eventVersion"`
	OrganizationID       string         `json:"organizationId"`
	SubjectID            string         `json:"subjectId"`
	ResourceType         string         `json:"resourceType"`
	ResourceID           string         `json:"resourceId"`
	Topic                string         `json:"topic"`
	ActorID              string         `json:"actorId"`
	TraceID              string         `json:"traceId"`
	IdempotencyKeyDigest string         `json:"idempotencyKeyDigest"`
	PayloadDigest        string         `json:"payloadDigest"`
	SafePayload          map[string]any `json:"safePayload"`
	OccurredAt           time.Time      `json:"occurredAt"`
	SchemaVersion        string         `json:"schemaVersion"`
}
