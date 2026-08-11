// Package domain contains the Go-owned projection of the frozen canonical
// envelope. This type accepts no input yet; validation and NATS publishing are
// introduced in their dedicated follow-up commit.
package domain

import "time"

type EventType string

const (
	EventTypePublicationInvalidated EventType = "PUBLICATION_INVALIDATED"
	EventTypeWorkflowTransitioned   EventType = "WORKFLOW_TRANSITIONED"
	EventTypeJobProgressChanged     EventType = "JOB_PROGRESS_CHANGED"
	EventTypeNotificationEnqueued   EventType = "NOTIFICATION_ENQUEUED"
	EventTypePresenceChanged        EventType = "PRESENCE_CHANGED"
	EventTypeOutboxRecorded         EventType = "OUTBOX_RECORDED"
)

// EventEnvelope mirrors the M3-T01 canonical event fields without adding a
// second shared contract source.
type EventEnvelope struct {
	EventID              string         `json:"eventId"`
	EventType            EventType      `json:"eventType"`
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

type Authorization struct {
	OrganizationID string
	SubjectID      string
	ActorID        string
	ResourceType   string
	ResourceID     string
	EventType      EventType
	IssuedAt       time.Time
	ExpiresAt      time.Time
}

// PublishReceipt is the local acknowledgement received after an event has
// been accepted by the configured NATS transport. HTTP callers receive only
// the event ID; the remaining fields are kept for local correlation and logs.
type PublishReceipt struct {
	EventID   string
	Subject   string
	Stream    string
	Sequence  uint64
	Duplicate bool
}

type PublishAck struct {
	Stream    string
	Sequence  uint64
	Duplicate bool
}

type Route struct {
	Scope       string
	Purpose     string
	NATSSubject string
}

var routes = map[EventType]Route{
	EventTypePublicationInvalidated: {Scope: "tenant", Purpose: "publication", NATSSubject: "nexora.events.publication"},
	EventTypeWorkflowTransitioned:   {Scope: "tenant", Purpose: "workflow", NATSSubject: "nexora.events.workflow"},
	EventTypeJobProgressChanged:     {Scope: "resource", Purpose: "job-progress", NATSSubject: "nexora.events.job-progress"},
	EventTypeNotificationEnqueued:   {Scope: "tenant", Purpose: "notification", NATSSubject: "nexora.events.notification"},
	EventTypePresenceChanged:        {Scope: "resource", Purpose: "presence", NATSSubject: "nexora.events.presence"},
	EventTypeOutboxRecorded:         {Scope: "tenant", Purpose: "outbox", NATSSubject: "nexora.events.outbox"},
}

func (eventType EventType) Route() (Route, bool) {
	route, ok := routes[eventType]
	return route, ok
}
