from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

# 환경변수는 저장소 루트 .env 하나로 통일한다. 실행 위치와 무관하게 절대 경로로 찾는다.
# 컨테이너에는 .env가 없고 docker compose가 환경변수를 직접 주입하므로 파일이 없어도 그대로 동작한다.
ROOT_ENV_FILE = Path(__file__).resolve().parents[2] / ".env"


class Settings(BaseSettings):
    # extra=ignore: 루트 .env에는 TOUR_API_KEY 같은 백엔드 전용 값도 함께 들어 있다.
    model_config = SettingsConfigDict(env_file=ROOT_ENV_FILE, extra="ignore")

    # 기획서 v6.2에서 Gemini를 제거하고 Anthropic Claude Sonnet 4.5로 단일화했다.
    anthropic_api_key: str = ""
    anthropic_model: str = "claude-sonnet-4-5"
    java_backend_base_url: str = "http://localhost:8080"


settings = Settings()
