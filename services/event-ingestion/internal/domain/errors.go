package domain

import "errors"

var (
	ErrUnauthorized = errors.New("event ingestion unauthorized")
	ErrRateLimited  = errors.New("event ingestion rate limited")
	ErrPublish      = errors.New("event ingestion publish failed")
)
