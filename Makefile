.DEFAULT_GOAL := help

.PHONY: help validate compose-config compose-up compose-health compose-down go-check go-vet

help:
	@echo "Nexora repository commands"
	@echo "  make validate       Run deterministic foundation checks"
	@echo "  make compose-config Render Compose without starting services"
	@echo "  make compose-up     Start local dependency services"
	@echo "  make compose-health Wait for local dependency health checks"
	@echo "  make compose-down   Stop local dependency services"
	@echo "  make go-check       Run Go event-ingestion vet and tests"
	@echo "  make go-vet         Vet the Go event-ingestion service"

validate:
	pwsh -NoProfile -File tools/validate-repo.ps1

compose-config:
	docker compose -f compose.yaml config --quiet

compose-up:
	docker compose -f compose.yaml up -d

compose-health:
	docker compose -f compose.yaml up -d --wait

compose-down:
	docker compose -f compose.yaml down

go-check:
	cd services/event-ingestion && go vet ./... && go test ./...

go-vet:
	cd services/event-ingestion && go vet ./...
