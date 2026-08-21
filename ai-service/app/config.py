from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env")

    # 기획서 v6.2에서 Gemini를 제거하고 Anthropic Claude Sonnet 4.5로 단일화했다.
    anthropic_api_key: str = ""
    anthropic_model: str = "claude-sonnet-4-5"
    java_backend_base_url: str = "http://localhost:8080"


settings = Settings()
