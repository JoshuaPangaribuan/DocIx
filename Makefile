start-elasticsearch:
	@echo "Loading Elasticsearch..."
	@docker compose -f ./elasticsearch-cluster/es-docker-compose.yaml up -d
	@echo "Elasticsearch loaded successfully"

stop-elasticsearch:
	@echo "Stopping Elasticsearch..."
	@docker compose -f ./elasticsearch-cluster/es-docker-compose.yaml down
	@echo "Elasticsearch stopped successfully"

restart-elasticsearch:
	@echo "Restarting Elasticsearch..."
	@docker compose -f ./elasticsearch-cluster/es-docker-compose.yaml down
	@docker compose -f ./elasticsearch-cluster/es-docker-compose.yaml up -d
	@echo "Elasticsearch restarted successfully"

es-clean:
	@echo "Cleaning Elasticsearch..."
	@docker compose -f ./elasticsearch-cluster/es-docker-compose.yaml down --volumes
	@echo "Elasticsearch cleaned successfully"

PHONY: start-elasticsearch stop-elasticsearch restart-elasticsearch es-clean

