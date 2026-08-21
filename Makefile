# 축제날씨 개발·Docker 명령어 모음
# 사용법: make <명령어>

.DEFAULT_GOAL := help

.PHONY: help init-env run run-ai test build up rebuild down restart ps logs logs-app logs-db clean reset db compose-config

help: ## 사용할 수 있는 명령어 표시
	@awk 'BEGIN {FS = ":.*##"}; /^[a-zA-Z_-]+:.*##/ {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

init-env: ## .env.example을 복사해 .env 생성 (기존 .env는 유지)
	@test -f .env || cp .env.example .env
	@echo ".env 파일을 확인하고 TOUR_API_KEY를 설정하세요."

run: ## H2를 사용하는 local 프로필로 애플리케이션 실행
	@if [ -f .env ]; then set -a; . ./.env; set +a; fi; ./gradlew bootRun --args='--spring.profiles.active=local'

run-ai: ## 루트 .env를 읽어 ai-service를 로컬 실행 (Part 6)
	@if [ -f .env ]; then set -a; . ./.env; set +a; fi; cd ai-service && python -m uvicorn app.main:app --port $${AI_SERVICE_PORT:-8000}

test: ## 전체 테스트 실행
	./gradlew test

build: ## Docker 앱 이미지 빌드
	docker compose build app

up: ## 앱과 PostgreSQL을 백그라운드로 실행
	docker compose up -d --build --remove-orphans

rebuild: ## 앱 이미지를 다시 빌드한 뒤 전체 서비스 실행
	docker compose up -d --build --force-recreate --remove-orphans

down: ## 앱과 PostgreSQL 컨테이너 종료·삭제 (DB 데이터는 유지)
	docker compose down --remove-orphans

restart: ## 컨테이너 재시작 (DB 데이터는 유지)
	docker compose down --remove-orphans && docker compose up -d --build --remove-orphans

ps: ## 컨테이너 상태 확인
	docker compose ps

logs: ## 전체 서비스 로그 스트리밍
	docker compose logs -f

logs-app: ## Spring Boot 앱 로그 스트리밍
	docker compose logs -f app

logs-db: ## PostgreSQL 로그 스트리밍
	docker compose logs -f postgres

db: ## PostgreSQL 콘솔 접속
	docker compose exec postgres psql -U festival -d festival

compose-config: ## .env를 반영한 Docker Compose 설정 확인
	docker compose config

clean: ## 컨테이너와 DB 볼륨까지 삭제 (주의: 로컬 DB 초기화)
	docker compose down -v --remove-orphans

reset: ## DB를 초기화하고 앱과 PostgreSQL을 새로 실행
	docker compose down -v --remove-orphans && docker compose up -d --build --remove-orphans
