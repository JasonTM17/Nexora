package domain

import (
	"encoding/json"
	"errors"
	"fmt"
	"math"
	"regexp"
	"strconv"
	"strings"
	"time"
	"unicode"
)

const (
	SchemaVersion = "1.0.0"
	maxAuthTTL    = 5 * time.Minute
)

var (
	ErrInvalidEnvelope = errors.New("invalid event envelope")
	ErrUnownedSubject  = errors.New("unowned subject")

	uuidPattern         = regexp.MustCompile(`(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`)
	resourceTypePattern = regexp.MustCompile(`^[a-z][a-z0-9_-]{0,63}$`)
	traceIDPattern      = regexp.MustCompile(`^[A-Za-z0-9._-]{1,128}$`)
	digestPattern       = regexp.MustCompile(`^[A-Za-z0-9:_-]{16,160}$`)
	forbiddenValue      = regexp.MustCompile(`(?i)(authorization|bearer|token|secret|password|cookie|provider|prompt|private[ _-]?key|access[ _-]?token|api[ _-]?key|pii|email|phone|body|raw|html|document)`)
)

var allowedPayloadKeys = map[string]struct{}{
	"resourceId": {}, "resourceType": {}, "organizationId": {}, "subjectId": {},
	"actorId": {}, "eventVersion": {}, "jobState": {}, "progress": {},
	"correlationId": {}, "traceId": {}, "receiptId": {}, "schemaVersion": {},
	"safeDisplay": {},
}

var allowedDisplayKeys = map[string]struct{}{
	"label": {}, "status": {}, "hint": {}, "state": {}, "variant": {}, "progressText": {},
}

var allowedVariants = map[string]struct{}{
	"neutral": {}, "success": {}, "warning": {}, "danger": {}, "info": {},
}

func ValidateAuthorizedEnvelope(now time.Time, authorization Authorization, envelope EventEnvelope) error {
	if err := validateAuthorization(now, authorization); err != nil {
		return err
	}
	if err := ValidateEnvelope(envelope); err != nil {
		return err
	}
	if envelope.OrganizationID != authorization.OrganizationID ||
		envelope.SubjectID != authorization.SubjectID ||
		envelope.ActorID != authorization.ActorID ||
		envelope.ResourceType != authorization.ResourceType ||
		envelope.ResourceID != authorization.ResourceID ||
		envelope.EventType != authorization.EventType {
		return fmt.Errorf("%w: envelope does not match trusted context", ErrUnownedSubject)
	}
	return nil
}

func ValidateEnvelope(envelope EventEnvelope) error {
	if !uuidPattern.MatchString(envelope.EventID) ||
		!uuidPattern.MatchString(envelope.OrganizationID) ||
		!uuidPattern.MatchString(envelope.SubjectID) ||
		!uuidPattern.MatchString(envelope.ActorID) ||
		!uuidPattern.MatchString(envelope.ResourceID) {
		return invalid("identifier")
	}
	if !resourceTypePattern.MatchString(envelope.ResourceType) {
		return invalid("resourceType")
	}
	if envelope.EventVersion <= 0 {
		return invalid("eventVersion")
	}
	route, ok := envelope.EventType.Route()
	if !ok {
		return invalid("eventType")
	}
	ownerID := envelope.OrganizationID
	if route.Scope == "resource" {
		ownerID = envelope.ResourceID
	}
	if envelope.Topic != route.Scope+":"+ownerID+":"+route.Purpose || len(envelope.Topic) > 180 {
		return invalid("topic")
	}
	if envelope.SchemaVersion != SchemaVersion {
		return invalid("schemaVersion")
	}
	if !safeDigest(envelope.IdempotencyKeyDigest) || !safeDigest(envelope.PayloadDigest) {
		return invalid("digest")
	}
	if !traceIDPattern.MatchString(envelope.TraceID) || forbiddenValue.MatchString(envelope.TraceID) {
		return invalid("traceId")
	}
	if envelope.OccurredAt.IsZero() {
		return invalid("occurredAt")
	}
	if err := validateSafePayload(envelope); err != nil {
		return err
	}
	return nil
}

func validateAuthorization(now time.Time, authorization Authorization) error {
	if authorization.IssuedAt.IsZero() || authorization.ExpiresAt.IsZero() ||
		!authorization.ExpiresAt.After(authorization.IssuedAt) ||
		authorization.ExpiresAt.Sub(authorization.IssuedAt) > maxAuthTTL ||
		now.Before(authorization.IssuedAt) || !now.Before(authorization.ExpiresAt) {
		return fmt.Errorf("%w: authorization is not current", ErrUnownedSubject)
	}
	if !uuidPattern.MatchString(authorization.OrganizationID) ||
		!uuidPattern.MatchString(authorization.SubjectID) ||
		!uuidPattern.MatchString(authorization.ActorID) ||
		!uuidPattern.MatchString(authorization.ResourceID) ||
		!resourceTypePattern.MatchString(authorization.ResourceType) {
		return fmt.Errorf("%w: authorization identity is invalid", ErrUnownedSubject)
	}
	if _, ok := authorization.EventType.Route(); !ok {
		return fmt.Errorf("%w: authorization event type is invalid", ErrUnownedSubject)
	}
	return nil
}

func validateSafePayload(envelope EventEnvelope) error {
	if envelope.SafePayload == nil {
		return invalid("safePayload")
	}
	for key, value := range envelope.SafePayload {
		if _, ok := allowedPayloadKeys[key]; !ok {
			return invalid("safePayload." + key)
		}
		switch key {
		case "safeDisplay":
			if err := validateSafeDisplay(value); err != nil {
				return err
			}
		case "progress":
			number, ok := numericValue(value)
			if !ok || number < 0 || number > 100 {
				return invalid("safePayload.progress")
			}
		case "eventVersion":
			number, ok := numericValue(value)
			if !ok || number < 1 || number != math.Trunc(number) {
				return invalid("safePayload.eventVersion")
			}
		default:
			text, ok := value.(string)
			if !ok || !safeString(text, 1, 160) {
				return invalid("safePayload." + key)
			}
		}
	}

	identity := map[string]string{
		"organizationId": envelope.OrganizationID,
		"subjectId":      envelope.SubjectID,
		"actorId":        envelope.ActorID,
		"resourceId":     envelope.ResourceID,
		"resourceType":   envelope.ResourceType,
		"traceId":        envelope.TraceID,
		"schemaVersion":  envelope.SchemaVersion,
	}
	for key, expected := range identity {
		if value, ok := envelope.SafePayload[key]; ok && value != expected {
			return invalid("safePayload." + key)
		}
	}
	if value, ok := envelope.SafePayload["eventVersion"]; ok {
		number, valid := numericValue(value)
		if !valid || number != float64(envelope.EventVersion) {
			return invalid("safePayload.eventVersion")
		}
	}
	return nil
}

func validateSafeDisplay(value any) error {
	display, ok := value.(map[string]any)
	if !ok {
		return invalid("safePayload.safeDisplay")
	}
	if _, ok := display["label"]; !ok {
		return invalid("safePayload.safeDisplay.label")
	}
	if _, ok := display["status"]; !ok {
		return invalid("safePayload.safeDisplay.status")
	}
	for key, value := range display {
		if _, ok := allowedDisplayKeys[key]; !ok {
			return invalid("safePayload.safeDisplay." + key)
		}
		text, ok := value.(string)
		if !ok || !printable(text) || forbiddenValue.MatchString(text) {
			return invalid("safePayload.safeDisplay." + key)
		}
		switch key {
		case "label":
			if len(text) < 1 || len(text) > 80 {
				return invalid("safePayload.safeDisplay.label")
			}
		case "status", "state":
			if len(text) < 1 || len(text) > 32 {
				return invalid("safePayload.safeDisplay." + key)
			}
		case "hint":
			if len(text) > 120 {
				return invalid("safePayload.safeDisplay.hint")
			}
		case "variant":
			if _, ok := allowedVariants[text]; !ok {
				return invalid("safePayload.safeDisplay.variant")
			}
		case "progressText":
			if len(text) > 40 {
				return invalid("safePayload.safeDisplay.progressText")
			}
		}
	}
	return nil
}

func numericValue(value any) (float64, bool) {
	switch number := value.(type) {
	case json.Number:
		parsed, err := number.Float64()
		return parsed, err == nil
	case float64:
		return number, true
	case float32:
		return float64(number), true
	case int:
		return float64(number), true
	case int64:
		return float64(number), true
	case int32:
		return float64(number), true
	case uint:
		return float64(number), true
	case uint64:
		if number > 1<<53 {
			return 0, false
		}
		return float64(number), true
	default:
		return 0, false
	}
}

func safeString(value string, minimum, maximum int) bool {
	return len(value) >= minimum && len(value) <= maximum && printable(value) && !forbiddenValue.MatchString(value)
}

func boundedPrintable(value string, minimum, maximum int) bool {
	return len(value) >= minimum && len(value) <= maximum && printable(value)
}

func safeDigest(value string) bool {
	return digestPattern.MatchString(value) && !forbiddenValue.MatchString(value)
}

func printable(value string) bool {
	return !strings.ContainsFunc(value, func(character rune) bool {
		return !unicode.IsPrint(character)
	})
}

func invalid(field string) error {
	return fmt.Errorf("%w: %s", ErrInvalidEnvelope, strconv.Quote(field))
}
