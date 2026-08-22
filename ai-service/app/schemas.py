from typing import Literal

from pydantic import BaseModel, Field

DEFAULT_DISCLAIMER = "AI 참고 답변 · 확실한 정보는 담당 부서 문의 권장"
# 기획서 6.2 방어 3 · 추정치 표시마다 붙는 문구
ESTIMATE_DISCLAIMER = "AI 추정 · 참고용 · 실측 아님"
# 기획서 6.2 방어 5 · 실무자 판단 우선
OPERATOR_PRIORITY_NOTE = (
    "AI 추정 방문 인원은 참고 정보입니다. 실제 배치 결정은 담당 부서·유관 기관 협의 후 진행 권고."
)

Confidence = Literal["high", "medium", "low"]


# 방향 D · 운영 조정 제안 AI 확장 (기획서 6.5)
class ExpandedRecommendation(BaseModel):
    recommendationCode: str
    disclaimer: str = DEFAULT_DISCLAIMER
    coreAction: str
    parkingDistribution: str | None = None
    staffing: str | None = None
    precedingAction: str | None = None
    relatedRiskCodes: list[str] = Field(default_factory=list)
    source: Literal["AI", "RULE_FALLBACK"]


class ExpandRecommendationsResponse(BaseModel):
    reportId: str
    recommendations: list[ExpandedRecommendation]


# 방향 B · 관광지 방문 인원 추정 (기획서 6.2)
# confidence=low면 추정치를 만들지 않고 시군구 단위 실측만 남긴다(방어 6).
class PlaceVisitorEstimate(BaseModel):
    placeName: str
    min: int | None = None
    max: int | None = None
    confidence: Confidence
    reasoning: str
    displayText: str
    disclaimer: str = ESTIMATE_DISCLAIMER
    source: Literal["AI", "UNAVAILABLE"]


# 방향 A · 리스크 심각도 3단계 판정 (기획서 6.3)
class RiskSeverityJudgement(BaseModel):
    riskCode: str
    severity: Literal["critical", "warning", "info"]
    reason: str
    # 재현성 유지 · 규칙 엔진 매칭 원문을 그대로 병기한다.
    ruleMatchLog: str
    source: Literal["AI", "RULE_FALLBACK"]


# 방향 C · 리포트 상단 브리핑 (기획서 6.4)
class ReportBriefing(BaseModel):
    text: str | None = None
    disclaimer: str = DEFAULT_DISCLAIMER
    source: Literal["AI", "UNAVAILABLE"]


class AiReportResponse(BaseModel):
    reportId: str
    briefing: ReportBriefing
    riskSeverities: list[RiskSeverityJudgement]
    placeEstimates: list[PlaceVisitorEstimate]
    recommendations: list[ExpandedRecommendation]
    operatorPriorityNote: str = OPERATOR_PRIORITY_NOTE
    # 폴백이 발생했을 때 그 이유. 비어 있으면 전부 AI 결과다.
    warnings: list[str] = Field(default_factory=list)
