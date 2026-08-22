"""기획서 Part 6 · AI 4방향(B·A·C·D)을 병렬 호출해 한 번에 돌려준다(6.7 병렬 호출)."""

from concurrent.futures import ThreadPoolExecutor

from fastapi import APIRouter, HTTPException

from app.gemini_service import (
    build_briefing,
    estimate_place_visitors,
    expand_recommendation,
    judge_risk_severity,
)
from app.java_client import ReportNotFoundError, get_forecast_report
from app.report_context import build_context, related_risk_codes, top_volatility_places
from app.schemas import AiReportResponse

router = APIRouter(prefix="/api/v1/reports", tags=["ai"])

_MAX_WORKERS = 8


@router.post("/{report_id}/ai-report", response_model=AiReportResponse)
def create_ai_report(report_id: str) -> AiReportResponse:
    try:
        report = get_forecast_report(report_id)
    except ReportNotFoundError:
        raise HTTPException(status_code=404, detail=f"리포트를 찾을 수 없습니다: {report_id}")

    context = build_context(report)
    risks = report.get("risks") or []
    checklist = (report.get("operationProposal") or {}).get("items") or []
    places = top_volatility_places(report)

    # 폴백이 났을 때 그 이유를 한곳에 모아 화면까지 올린다.
    errors: list[str] = []

    with ThreadPoolExecutor(max_workers=_MAX_WORKERS) as pool:
        briefing_future = pool.submit(build_briefing, context, errors)
        severity_futures = [pool.submit(judge_risk_severity, context, risk, errors) for risk in risks]
        estimate_futures = [pool.submit(estimate_place_visitors, context, place, errors) for place in places]
        recommendation_futures = [
            pool.submit(
                expand_recommendation,
                context,
                {**item, "relatedRiskCodes": related_risk_codes(item, risks)},
                errors,
            )
            for item in checklist
        ]

        response = AiReportResponse(
            reportId=report_id,
            briefing=briefing_future.result(),
            riskSeverities=[future.result() for future in severity_futures],
            placeEstimates=[future.result() for future in estimate_futures],
            recommendations=[future.result() for future in recommendation_futures],
        )

    response.warnings = errors
    return response
