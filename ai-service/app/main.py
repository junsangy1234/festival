from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import ai_report, recommendations

app = FastAPI(title="festival-ai-service")

# ponytail: 프론트 도메인이 아직 미정이라 전체 허용. 프론트 배포 확정되면 origin 제한.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(recommendations.router)
app.include_router(ai_report.router)


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}
