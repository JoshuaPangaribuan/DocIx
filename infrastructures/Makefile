# DocIx Infrastructure Management Makefile

# Include environment variables
include .env.dev

# Colors for output
GREEN=\033[0;32m
YELLOW=\033[1;33m
RED=\033[0;31m
NC=\033[0m # No Color

.DEFAULT_GOAL := help

# Help target
help: ## Show this help message
	@echo "DocIx Infrastructure Management"
	@echo "Available commands:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# Development Environment Commands
dev-up: ## Start development infrastructure
	@echo "$(YELLOW)Starting DocIx development infrastructure...$(NC)"
	@docker-compose -f docker-compose.dev.yml --env-file .env.dev up -d
	@echo "$(GREEN)Development infrastructure started successfully$(NC)"
	@echo "$(YELLOW)Services available at:$(NC)"
	@echo "  - MinIO Console: http://localhost:9001"
	@echo "  - RabbitMQ Management: http://localhost:15672"
	@echo "  - Elasticsearch: http://localhost:9200"

dev-down: ## Stop development infrastructure
	@echo "$(YELLOW)Stopping DocIx development infrastructure...$(NC)"
	@docker-compose -f docker-compose.dev.yml down
	@echo "$(GREEN)Development infrastructure stopped successfully$(NC)"

dev-restart: ## Restart development infrastructure
	@echo "$(YELLOW)Restarting DocIx development infrastructure...$(NC)"
	@docker-compose -f docker-compose.dev.yml down
	@docker-compose -f docker-compose.dev.yml --env-file .env.dev up -d
	@echo "$(GREEN)Development infrastructure restarted successfully$(NC)"

dev-logs: ## View development infrastructure logs
	@docker-compose -f docker-compose.dev.yml logs -f

dev-clean: ## Clean development infrastructure (remove volumes)
	@echo "$(RED)Cleaning DocIx development infrastructure...$(NC)"
	@docker-compose -f docker-compose.dev.yml down --volumes --remove-orphans
	@echo "$(GREEN)Development infrastructure cleaned successfully$(NC)"

# Production Environment Commands
prod-up: ## Start production infrastructure
	@echo "$(YELLOW)Starting DocIx production infrastructure...$(NC)"
	@docker-compose -f docker-compose.yml --env-file .env.prod up -d
	@echo "$(GREEN)Production infrastructure started successfully$(NC)"

prod-down: ## Stop production infrastructure
	@echo "$(YELLOW)Stopping DocIx production infrastructure...$(NC)"
	@docker-compose -f docker-compose.yml down
	@echo "$(GREEN)Production infrastructure stopped successfully$(NC)"

prod-restart: ## Restart production infrastructure
	@echo "$(YELLOW)Restarting DocIx production infrastructure...$(NC)"
	@docker-compose -f docker-compose.yml down
	@docker-compose -f docker-compose.yml --env-file .env.prod up -d
	@echo "$(GREEN)Production infrastructure restarted successfully$(NC)"

prod-logs: ## View production infrastructure logs
	@docker-compose -f docker-compose.yml logs -f

# Elasticsearch specific commands
start-elasticsearch: ## Start Elasticsearch cluster
	@echo "$(YELLOW)Loading Elasticsearch...$(NC)"
	@docker compose -f ./elasticsearch-cluster/es-docker-compose.yaml up -d
	@echo "$(GREEN)Elasticsearch loaded successfully$(NC)"

stop-elasticsearch: ## Stop Elasticsearch cluster
	@echo "$(YELLOW)Stopping Elasticsearch...$(NC)"
	@docker compose -f ./elasticsearch-cluster/es-docker-compose.yaml down
	@echo "$(GREEN)Elasticsearch stopped successfully$(NC)"

restart-elasticsearch: ## Restart Elasticsearch cluster
	@echo "$(YELLOW)Restarting Elasticsearch...$(NC)"
	@docker compose -f ./elasticsearch-cluster/es-docker-compose.yaml down
	@docker compose -f ./elasticsearch-cluster/es-docker-compose.yaml up -d
	@echo "$(GREEN)Elasticsearch restarted successfully$(NC)"

es-clean: ## Clean Elasticsearch (remove volumes)
	@echo "$(RED)Cleaning Elasticsearch...$(NC)"
	@docker compose -f ./elasticsearch-cluster/es-docker-compose.yaml down --volumes
	@echo "$(GREEN)Elasticsearch cleaned successfully$(NC)"

# Application Commands
build-app: ## Build DocIx application
	@echo "$(YELLOW)Building DocIx application...$(NC)"
	@cd .. && ./gradlew build
	@echo "$(GREEN)Application built successfully$(NC)"

docker-build: ## Build Docker image for DocIx application
	@echo "$(YELLOW)Building DocIx Docker image...$(NC)"
	@cd .. && docker build -t docix:latest .
	@echo "$(GREEN)Docker image built successfully$(NC)"

deploy: docker-build prod-up ## Build and deploy application to production
	@echo "$(GREEN)DocIx application deployed successfully$(NC)"

# Health Check Commands
health-check: ## Check health of all services
	@echo "$(YELLOW)Checking health of DocIx services...$(NC)"
	@echo "Checking MinIO..."
	@curl -f http://localhost:9000/minio/health/live > /dev/null 2>&1 && echo "$(GREEN)✓ MinIO: Healthy$(NC)" || echo "$(RED)✗ MinIO: Unhealthy$(NC)"
	@echo "Checking RabbitMQ..."
	@curl -f http://localhost:15672 > /dev/null 2>&1 && echo "$(GREEN)✓ RabbitMQ: Healthy$(NC)" || echo "$(RED)✗ RabbitMQ: Unhealthy$(NC)"
	@echo "Checking Elasticsearch..."
	@curl -f http://localhost:9200/_cluster/health > /dev/null 2>&1 && echo "$(GREEN)✓ Elasticsearch: Healthy$(NC)" || echo "$(RED)✗ Elasticsearch: Unhealthy$(NC)"

# Backup Commands
backup: ## Backup all data volumes
	@echo "$(YELLOW)Creating backup of DocIx data...$(NC)"
	@mkdir -p backups/$(shell date +%Y%m%d_%H%M%S)
	@echo "$(GREEN)Backup completed$(NC)"

# Cleanup Commands
clean-all: ## Clean all Docker resources (DESTRUCTIVE)
	@echo "$(RED)Warning: This will remove all DocIx Docker resources!$(NC)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		echo ""; \
		docker-compose -f docker-compose.yml down --volumes --remove-orphans; \
		docker-compose -f docker-compose.dev.yml down --volumes --remove-orphans; \
		docker system prune -f; \
		echo "$(GREEN)All resources cleaned$(NC)"; \
	else \
		echo ""; \
		echo "$(YELLOW)Operation cancelled$(NC)"; \
	fi

# Monitoring Commands
stats: ## Show Docker container stats
	@docker stats --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"

# Development Utilities
dev-setup: ## Setup development environment
	@echo "$(YELLOW)Setting up DocIx development environment...$(NC)"
	@cp .env.dev.example .env.dev 2>/dev/null || echo "Using existing .env.dev"
	@echo "$(GREEN)Development environment setup completed$(NC)"
	@echo "$(YELLOW)Please review and update .env.dev file with your configuration$(NC)"

init-buckets: ## Initialize MinIO buckets
	@echo "$(YELLOW)Initializing MinIO buckets...$(NC)"
	@docker exec docix-minio-dev mc alias set local http://localhost:9000 minioadmin minioadmin
	@docker exec docix-minio-dev mc mb local/docix-documents --ignore-existing
	@echo "$(GREEN)MinIO buckets initialized$(NC)"

.PHONY: help dev-up dev-down dev-restart dev-logs dev-clean prod-up prod-down prod-restart prod-logs start-elasticsearch stop-elasticsearch restart-elasticsearch es-clean build-app docker-build deploy health-check backup clean-all stats dev-setup init-buckets
