.DEFAULT_GOAL := help

.PHONY: help validate compose-config compose-up compose-down

help:
	@echo "Nexora repository commands"
	@echo "  make validate       Run deterministic foundation checks"
	@echo "  make compose-config Render Compose without starting services"
	@echo "  make compose-up     Start local dependency services"
	@echo "  make compose-down   Stop local dependency services"

validate:
	pwsh -NoProfile -File tools/validate-repo.ps1

compose-config:
	docker compose -f compose.yaml config --quiet

compose-up:
	docker compose -f compose.yaml up -d

compose-down:
	docker compose -f compose.yaml down
