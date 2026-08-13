package domain

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"math"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"time"
	"unicode"
)

const (
	SchemaVersion         = "1.1.0"
	maxAuthTTL            = 5 * time.Minute
	maxEventVersion int64 = 9007199254740991
)

var (
	ErrInvalidEnvelope = errors.New("invalid event envelope")
	ErrUnownedSubject  = errors.New("unowned subject")

	uuidPattern    = regexp.MustCompile(`^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`)
	traceIDPattern = regexp.MustCompile(`^[a-f0-9]{32}$`)
	digestPattern  = regexp.MustCompile(`^sha256:[a-f0-9]{64}$`)
	forbiddenValue = regexp.MustCompile(`(?i)(authorization|bearer|token|secret|password|cookie|provider|prompt|private[ _-]?key|access[ _-]?token|api[ _-]?key|pii|email|phone|body|raw|html|document)`)
)

var allowedPayloadKeys = map[string]struct{}{
	"resourceId": {}, "resourceType": {}, "organizationId": {}, "subjectId": {},
	"actorId": {}, "eventVersion": {}, "jobState": {}, "progress": {},
	"correlationId": {}, "traceId": {}, "receiptId": {}, "schemaVersion": {},
	"safeDisplay": {},
}

var allowedDisplayKeys = map[string]struct{}{
	"label": {}, "status": {}, "variant": {},
}

var jobStateCatalog = map[string]struct{}{
	"QUEUED": {}, "RUNNING": {}, "COMPLETED": {}, "FAILED": {}, "CANCELED": {},
}

var safeDisplayVariants = map[EventType]map[string]string{
	EventTypePublicationInvalidated: {
		"QUEUED":      "warning",
		"PUBLISHED":   "success",
		"ARCHIVED":    "neutral",
		"INVALIDATED": "danger",
	},
	EventTypeWorkflowTransitioned: {
		"PENDING":   "info",
		"IN_REVIEW": "warning",
		"PUBLISHED": "success",
		"ARCHIVED":  "neutral",
		"FAILED":    "danger",
	},
	EventTypeJobProgressChanged: {
		"QUEUED":    "info",
		"RUNNING":   "warning",
		"COMPLETED": "success",
		"FAILED":    "danger",
		"CANCELED":  "neutral",
	},
	EventTypeNotificationEnqueued: {
		"QUEUED":    "info",
		"DELIVERED": "success",
		"FAILED":    "danger",
	},
	EventTypePresenceChanged: {
		"ACTIVE":   "success",
		"INACTIVE": "neutral",
	},
	EventTypeOutboxRecorded: {
		"PENDING":     "info",
		"CLAIMED":     "warning",
		"PUBLISHED":   "success",
		"FAILED":      "danger",
		"DEAD_LETTER": "danger",
	},
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
	if envelope.EventVersion < 1 || envelope.EventVersion > maxEventVersion {
		return invalid("eventVersion")
	}
	route, ok := envelope.EventType.Route()
	if !ok {
		return invalid("eventType")
	}
	if _, ok := route.ResourceTypes[envelope.ResourceType]; !ok {
		return invalid("resourceType")
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
	if !safeTrace(envelope.TraceID) {
		return invalid("traceId")
	}
	if envelope.OccurredAt.IsZero() {
		return invalid("occurredAt")
	}
	if err := validateSafePayload(envelope); err != nil {
		return err
	}
	if digest, err := payloadDigest(envelope.SafePayload); err != nil || envelope.PayloadDigest != digest {
		return invalid("payloadDigest")
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
		!uuidPattern.MatchString(authorization.ResourceID) {
		return fmt.Errorf("%w: authorization identity is invalid", ErrUnownedSubject)
	}
	route, ok := authorization.EventType.Route()
	if !ok {
		return fmt.Errorf("%w: authorization event type is invalid", ErrUnownedSubject)
	}
	if _, ok := route.ResourceTypes[authorization.ResourceType]; !ok {
		return fmt.Errorf("%w: authorization resource type is invalid", ErrUnownedSubject)
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
			if err := validateSafeDisplay(envelope.EventType, value); err != nil {
				return err
			}
		case "eventVersion":
			_, ok := eventVersionValue(value)
			if !ok {
				return invalid("safePayload.eventVersion")
			}
		case "progress":
			number, ok := numericValue(value)
			if !ok || number < 0 || number > 100 || number != math.Trunc(number) {
				return invalid("safePayload.progress")
			}
		case "jobState":
			text, ok := value.(string)
			if !ok || !safeEnum(text, jobStateCatalog) {
				return invalid("safePayload.jobState")
			}
		case "resourceId", "organizationId", "subjectId", "actorId", "receiptId":
			text, ok := value.(string)
			if !ok || !safeUUID(text) {
				return invalid("safePayload." + key)
			}
		case "resourceType":
			text, ok := value.(string)
			if !ok || !safeResourceType(envelope.EventType, text) {
				return invalid("safePayload.resourceType")
			}
		case "traceId", "correlationId":
			text, ok := value.(string)
			if !ok || !safeTrace(text) {
				return invalid("safePayload." + key)
			}
		case "schemaVersion":
			text, ok := value.(string)
			if !ok || text != SchemaVersion {
				return invalid("safePayload.schemaVersion")
			}
		default:
			return invalid("safePayload." + key)
		}
	}
	required := []string{"resourceId", "resourceType", "organizationId", "subjectId", "actorId", "eventVersion", "traceId", "schemaVersion", "safeDisplay"}
	for _, key := range required {
		if _, ok := envelope.SafePayload[key]; !ok {
			return invalid("safePayload." + key)
		}
	}
	if envelope.EventType == EventTypeJobProgressChanged {
		if _, ok := envelope.SafePayload["jobState"]; !ok {
			return invalid("safePayload.jobState")
		}
		if _, ok := envelope.SafePayload["progress"]; !ok {
			return invalid("safePayload.progress")
		}
	} else {
		if _, ok := envelope.SafePayload["jobState"]; ok {
			return invalid("safePayload.jobState")
		}
		if _, ok := envelope.SafePayload["progress"]; ok {
			return invalid("safePayload.progress")
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
		number, valid := eventVersionValue(value)
		if !valid || number != envelope.EventVersion {
			return invalid("safePayload.eventVersion")
		}
	}
	return nil
}

func eventVersionValue(value any) (int64, bool) {
	switch number := value.(type) {
	case json.Number:
		parsed, err := number.Int64()
		return parsed, err == nil && parsed >= 1 && parsed <= maxEventVersion
	case int:
		return int64(number), number >= 1 && int64(number) <= maxEventVersion
	case int32:
		return int64(number), number >= 1 && int64(number) <= maxEventVersion
	case int64:
		return number, number >= 1 && number <= maxEventVersion
	case uint:
		return int64(number), number >= 1 && uint64(number) <= uint64(maxEventVersion)
	case uint64:
		return int64(number), number >= 1 && number <= uint64(maxEventVersion)
	case float32:
		return eventVersionFloat(float64(number))
	case float64:
		return eventVersionFloat(number)
	default:
		return 0, false
	}
}

func eventVersionFloat(number float64) (int64, bool) {
	if math.IsNaN(number) || math.IsInf(number, 0) || number < 1 || number > float64(maxEventVersion) || number != math.Trunc(number) {
		return 0, false
	}
	return int64(number), true
}

func validateSafeDisplay(eventType EventType, value any) error {
	display, ok := value.(map[string]any)
	if !ok {
		return invalid("safePayload.safeDisplay")
	}
	if len(display) != len(allowedDisplayKeys) {
		return invalid("safePayload.safeDisplay")
	}
	for key, value := range display {
		if _, ok := allowedDisplayKeys[key]; !ok {
			return invalid("safePayload.safeDisplay." + key)
		}
		text, ok := value.(string)
		if !ok || !printable(text) || strings.ContainsRune(text, unicode.ReplacementChar) || forbiddenValue.MatchString(text) {
			return invalid("safePayload.safeDisplay." + key)
		}
		switch key {
		case "label":
			if text != string(eventType) {
				return invalid("safePayload.safeDisplay.label")
			}
		case "status":
			if variant, ok := safeDisplayVariants[eventType][text]; !ok || variant == "" {
				return invalid("safePayload.safeDisplay.status")
			}
		case "variant":
			status, ok := display["status"].(string)
			if !ok {
				return invalid("safePayload.safeDisplay.status")
			}
			expected, ok := safeDisplayVariants[eventType][status]
			if !ok || expected != text {
				return invalid("safePayload.safeDisplay.variant")
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

func safeUUID(value string) bool {
	return uuidPattern.MatchString(value) && !forbiddenValue.MatchString(value) && !strings.ContainsRune(value, unicode.ReplacementChar)
}

func safeResourceType(eventType EventType, value string) bool {
	route, ok := eventType.Route()
	if !ok {
		return false
	}
	_, ok = route.ResourceTypes[value]
	return ok
}

func safeEnum(value string, allowed map[string]struct{}) bool {
	_, ok := allowed[value]
	return ok && !forbiddenValue.MatchString(value) && !strings.ContainsRune(value, unicode.ReplacementChar)
}

func safeDigest(value string) bool {
	return digestPattern.MatchString(value) && !forbiddenValue.MatchString(value)
}

func safeTrace(value string) bool {
	return traceIDPattern.MatchString(value) && !forbiddenValue.MatchString(value)
}

func payloadDigest(payload map[string]any) (string, error) {
	canonical, err := canonicalJSON(payload)
	if err != nil {
		return "", err
	}
	sum := sha256.Sum256([]byte("nexora:event-payload:1.1\n" + canonical))
	return "sha256:" + hex.EncodeToString(sum[:]), nil
}

func canonicalJSON(value any) (string, error) {
	switch typed := value.(type) {
	case map[string]any:
		keys := make([]string, 0, len(typed))
		for key := range typed {
			keys = append(keys, key)
		}
		sort.Strings(keys)
		var builder strings.Builder
		builder.WriteByte('{')
		for index, key := range keys {
			if index > 0 {
				builder.WriteByte(',')
			}
			encodedKey, err := json.Marshal(key)
			if err != nil {
				return "", err
			}
			builder.Write(encodedKey)
			builder.WriteByte(':')
			encodedValue, err := canonicalJSON(typed[key])
			if err != nil {
				return "", err
			}
			builder.WriteString(encodedValue)
		}
		builder.WriteByte('}')
		return builder.String(), nil
	case string:
		if strings.ContainsRune(typed, unicode.ReplacementChar) {
			return "", fmt.Errorf("invalid unicode scalar")
		}
		encoded, err := json.Marshal(typed)
		return string(encoded), err
	case json.Number:
		integer, err := typed.Int64()
		if err != nil {
			return "", err
		}
		return strconv.FormatInt(integer, 10), nil
	case float64:
		if typed != math.Trunc(typed) || math.IsNaN(typed) || math.IsInf(typed, 0) {
			return "", fmt.Errorf("non-canonical number")
		}
		return strconv.FormatInt(int64(typed), 10), nil
	case int:
		return strconv.Itoa(typed), nil
	case int64:
		return strconv.FormatInt(typed, 10), nil
	case int32:
		return strconv.FormatInt(int64(typed), 10), nil
	case uint:
		return strconv.FormatUint(uint64(typed), 10), nil
	case uint64:
		return strconv.FormatUint(typed, 10), nil
	default:
		return "", fmt.Errorf("unsupported canonical value %T", value)
	}
}

func printable(value string) bool {
	return !strings.ContainsFunc(value, func(character rune) bool {
		return !unicode.IsPrint(character)
	})
}

func invalid(field string) error {
	return fmt.Errorf("%w: %s", ErrInvalidEnvelope, strconv.Quote(field))
}
