# 축제날씨 개발·Docker 명령어 모음
# 사용법: make <명령어>
#
# Windows에서는 GNU make가 cmd.exe로 레시피를 실행하므로
# 셸 문법(if [ ... ], set -a 등)을 쓰지 않고 어느 셸에서나 같은 명령만 쓴다.
# TOUR_API_KEY 같은 값은 application.yaml이 루트 .env를 직접 읽는다.

.DEFAULT_GOAL := help

ifeq ($(OS),Windows_NT)
# Git Bash에서 실행해도 레시피가 항상 같은 셸에서 돌도록 cmd.exe로 고정한다.
SHELL := cmd.exe
.SHELLFLAGS := /c
GRADLE := .\gradlew.bat
else
GRADLE := ./gradlew
endif

PORT ?= 8080
AI_PORT ?= 8000

.PHONY: help init-env run run-ai stop console test test-ai build up rebuild down restart ps logs logs-app logs-db clean reset db compose-config

help: ## 사용할 수 있는 명령어 표시
	@echo make init-env  - .env.example 복사해 .env 생성
	@echo make run       - H2 local 프로필로 앱 실행, 포트 8080
	@echo make run-ai    - ai-service 실행, 포트 $(AI_PORT)
	@echo make stop      - 포트 $(PORT) 점유 프로세스 종료
	@echo make console   - 테스트 콘솔 diagnosis-test.html 열기
	@echo make test      - Java 테스트
	@echo make test-ai   - ai-service 테스트
	@echo make up        - Docker 전체 실행 / make down  - 종료
	@echo make logs      - 로그 스트리밍    / make ps    - 상태 확인
	@echo make db        - PostgreSQL 콘솔  / make reset - DB 초기화 후 재실행

ifeq ($(OS),Windows_NT)
init-env: ## .env.example을 복사해 .env 생성 (기존 .env는 유지)
	@if not exist .env copy .env.example .env
	@echo .env 파일을 확인하고 TOUR_API_KEY를 설정하세요.

console: ## 테스트 콘솔 열기
	@start http://localhost:8080/diagnosis-test.html

stop: ## 8080 포트를 잡고 있는 프로세스 종료
	@powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort $(PORT) -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $$_ -Force }; exit 0"
	@echo 포트 $(PORT) 정리 완료
else
init-env: ## .env.example을 복사해 .env 생성 (기존 .env는 유지)
	@test -f .env || cp .env.example .env
	@echo ".env 파일을 확인하고 TOUR_API_KEY를 설정하세요."

console: ## 테스트 콘솔 열기
	@open http://localhost:8080/diagnosis-test.html || xdg-open http://localhost:8080/diagnosis-test.html

stop: ## 8080 포트를 잡고 있는 프로세스 종료
	-@lsof -ti tcp:$(PORT) | xargs -r kill -9
	@echo "포트 $(PORT) 정리 완료"
endif

run: ## H2를 사용하는 local 프로필로 애플리케이션 실행
	$(GRADLE) bootRun --args="--spring.profiles.active=local"

run-ai: ## ai-service 실행 (Part 6)
	cd ai-service && python -m uvicorn app.main:app --port $(AI_PORT) --reload

test: ## Java 전체 테스트 실행
	$(GRADLE) test

test-ai: ## ai-service 테스트 실행
	cd ai-service && python -m pytest -q

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
