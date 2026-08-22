from fastapi import APIRouter, HTTPException

from app.gemini_service import expand_recommendation
from app.java_client import ReportNotFoundError, get_forecast_report
from app.report_context import build_context, related_risk_codes
from app.schemas import ExpandRecommendationsResponse

router = APIRouter(prefix="/api/v1/reports", tags=["recommendations"])


# 방향 D 단독 호출. 전체 AI 결과는 POST /{report_id}/ai-report를 쓴다.
@router.post("/{report_id}/recommendations/expand", response_model=ExpandRecommendationsResponse)
def expand_recommendations(report_id: str) -> ExpandRecommendationsResponse:
    try:
        report = get_forecast_report(report_id)
    except ReportNotFoundError:
        raise HTTPException(status_code=404, detail=f"리포트를 찾을 수 없습니다: {report_id}")

    context = build_context(report)
    risks = report.get("risks") or []
    checklist = (report.get("operationProposal") or {}).get("items") or []

    expanded = [
        expand_recommendation(context, {**item, "relatedRiskCodes": related_risk_codes(item, risks)})
        for item in checklist
    ]

    return ExpandRecommendationsResponse(reportId=report_id, recommendations=expanded)
