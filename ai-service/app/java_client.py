import time

import httpx

from app.config import settings

# ponytail: 인메모리 TTL 캐시. 프로세스 재시작 시 소실, 다중 인스턴스 간 공유 안 됨.
# 트래픽이 늘어 문제되면 Redis 등 공유 캐시로 교체.
_CACHE_TTL_SECONDS = 30
_cache: dict[str, tuple[float, dict]] = {}


class ReportNotFoundError(Exception):
    pass


def get_forecast_report(report_id: str) -> dict:
    cached = _cache.get(report_id)
    if cached and time.time() - cached[0] < _CACHE_TTL_SECONDS:
        return cached[1]

    url = f"{settings.java_backend_base_url}/api/v1/reports/{report_id}/forecast-report"
    response = httpx.get(url, timeout=10.0)
    if response.status_code == 404:
        raise ReportNotFoundError(report_id)
    response.raise_for_status()

    data = response.json()
    _cache[report_id] = (time.time(), data)
    return data
