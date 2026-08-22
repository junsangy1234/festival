"""기획서 Part 6 · Gemini로 4개 방향(B·A·C·D)을 처리한다.

기획서 v6.2는 Claude 단일화를 적었지만, 운영 결정에 따라 Gemini를 사용한다.
모든 호출은 실패 시 규칙 원문 또는 미표시로 폴백해 재현성을 유지한다(6.8).
"""

import json
import logging

from google import genai
from google.genai import types

from app.config import settings
from app.schemas import (
    DEFAULT_DISCLAIMER,
    ESTIMATE_DISCLAIMER,
    ExpandedRecommendation,
    PlaceVisitorEstimate,
    ReportBriefing,
    RiskSeverityJudgement,
)

logger = logging.getLogger(__name__)

_COMMON_RULES = """반드시 지켜야 할 것:
- 정중하되 단정하지 않게. "~권고", "~검토", "~우려" 같은 검토형 표현을 쓰고 명령형을 피하세요.
- 지역 특정 업체명·시설명·구체 수치는 확인된 것이 아니면 지어내지 말고 [확인 요망]으로 표시하세요.
- "완벽한", "확실한", "반드시 성공" 같은 근거 없는 정량 약속 표현을 쓰지 마세요.
"""


# 폴백 사유를 화면까지 올리기 위한 수집기. list.append는 스레드 안전해 병렬 호출에서도 그대로 쓴다.
def _record(errors: list | None, message: str) -> None:
    if errors is not None and message not in errors:
        errors.append(message)


# 응답 스키마를 강제해 구조화 JSON을 받는다. 실패하면 None을 돌려 호출부가 폴백하도록 한다.
# tool_name은 Gemini 호출에 쓰이지 않고 실패 로그 식별용으로만 남긴다.
def _call(system: str, user: str, tool_name: str, schema: dict, errors: list | None = None) -> dict | None:
    if not settings.gemini_api_key:
        _record(errors, "GEMINI_API_KEY가 설정되지 않아 규칙 원문으로 표시합니다.")
        return None
    try:
        client = genai.Client(api_key=settings.gemini_api_key)
        response = client.models.generate_content(
            model=settings.gemini_model,
            contents=user,
            config=types.GenerateContentConfig(
                system_instruction=system,
                response_mime_type="application/json",
                response_schema=schema,
            ),
        )
        if not response.text:
            _record(errors, "Gemini 응답이 비어 있어 규칙 원문으로 표시합니다.")
            return None
        return json.loads(response.text)
    except Exception as exception:
        logger.exception("Gemini 호출 실패: %s", tool_name)
        _record(errors, _reason(exception))
        return None


# 실무자가 바로 조치할 수 있게 대표적인 실패 원인을 한국어로 바꾼다.
def _reason(exception: Exception) -> str:
    message = str(exception)
    if "API_KEY_INVALID" in message or "API key not valid" in message:
        return "GEMINI_API_KEY가 올바르지 않습니다. .env의 키를 확인하세요."
    if "PERMISSION_DENIED" in message:
        return "이 키로는 Gemini API를 쓸 수 없습니다. Google AI Studio에서 키 권한을 확인하세요."
    if "RESOURCE_EXHAUSTED" in message or "429" in message or "quota" in message.lower():
        # 무료 티어는 분당 한도와 하루 한도가 따로 있다. 하루 한도면 기다려도 안 풀린다.
        if "PerDay" in message or "per day" in message.lower():
            return (
                "Gemini 무료 티어의 하루 요청 한도를 모두 썼습니다. "
                "내일 초기화되거나, Google AI Studio에서 유료 플랜으로 올려야 AI 결과가 나옵니다."
            )
        return "Gemini 분당 요청 한도를 초과했습니다. 1분 뒤 다시 시도하세요."
    if "NOT_FOUND" in message or "404" in message:
        return f"모델 이름을 찾을 수 없습니다: {settings.gemini_model}"
    return f"Gemini 호출 실패: {message[:200]}"


# ---------------------------------------------------------------- 방향 D · 제안 확장

_EXPAND_SYSTEM = (
    "당신은 지자체 축제 실무자를 돕는 운영 조정 제안 작성자입니다.\n"
    "규칙 엔진이 매칭한 원문 제안을 실무자가 바로 실행할 수 있는 4단 구조로 확장하세요.\n"
    + _COMMON_RULES
    + "- 각 필드는 1~3문장으로 간결하게. 적용되지 않는 필드는 '해당 사항 없음'이라고 쓰세요.\n"
    "- 선행 조치는 개최일 기준 D-day를 명시하세요 (예: D-7).\n"
)

_EXPAND_SCHEMA = {
    "type": "object",
    "properties": {
        "coreAction": {"type": "string", "description": "핵심 조치"},
        "parkingDistribution": {"type": "string", "description": "주차 분산 방안"},
        "staffing": {"type": "string", "description": "인력 배치 방안"},
        "precedingAction": {"type": "string", "description": "선행 조치, D-day 명시"},
    },
    "required": ["coreAction", "parkingDistribution", "staffing", "precedingAction"],
}


def _recommendation_fallback(recommendation: dict) -> ExpandedRecommendation:
    return ExpandedRecommendation(
        recommendationCode=recommendation["recommendationCode"],
        coreAction=f"{recommendation['title']}: {recommendation['defaultAction']}",
        relatedRiskCodes=recommendation.get("relatedRiskCodes") or [],
        source="RULE_FALLBACK",
    )


def expand_recommendation(festival_context: dict, recommendation: dict, errors: list | None = None) -> ExpandedRecommendation:
    """규칙 매칭 제안 1건을 실무자 톤 4단 구조로 확장한다. 실패 시 원본 규칙 문구로 폴백한다."""
    user = (
        f"축제명: {festival_context.get('festivalName')}\n"
        f"개최기간: {festival_context.get('startDate')} ~ {festival_context.get('endDate')}\n"
        f"원문 제안 코드: {recommendation['recommendationCode']}\n"
        f"원문 제안 제목: {recommendation['title']}\n"
        f"원문 제안 조치: {recommendation['defaultAction']}\n"
        f"난이도: {recommendation.get('difficulty')}\n"
        f"카테고리: {recommendation.get('category')}\n"
    )
    result = _call(_EXPAND_SYSTEM, user, "expanded_recommendation", _EXPAND_SCHEMA, errors)
    if not result:
        return _recommendation_fallback(recommendation)
    return ExpandedRecommendation(
        recommendationCode=recommendation["recommendationCode"],
        disclaimer=DEFAULT_DISCLAIMER,
        coreAction=result["coreAction"],
        parkingDistribution=result.get("parkingDistribution"),
        staffing=result.get("staffing"),
        precedingAction=result.get("precedingAction"),
        relatedRiskCodes=recommendation.get("relatedRiskCodes") or [],
        source="AI",
    )


# ------------------------------------------------- 방향 B · 관광지 방문 인원 추정

_ESTIMATE_SYSTEM = (
    "당신은 관광 데이터 분석가입니다. 공공 API에 없는 관광지 단위 방문 인원을 "
    "시군구 실측·집중률·유형·계절을 근거로 범위 추정합니다.\n"
    + _COMMON_RULES
    + "- 단일 숫자가 아니라 범위로 답하고, max는 min의 1.2배 이상이 되게 하세요(±20% 오차 표기).\n"
    "- reasoning에는 '시군구 실측 × 상대 규모 × 보정 계수' 형태의 계산 근거를 1~2문장으로 쓰세요.\n"
)

_ESTIMATE_SCHEMA = {
    "type": "object",
    "properties": {
        "min": {"type": "integer", "description": "추정 방문 인원 하한"},
        "max": {"type": "integer", "description": "추정 방문 인원 상한"},
        "reasoning": {"type": "string", "description": "계산 근거 1~2문장"},
    },
    "required": ["min", "max", "reasoning"],
}


# 방어 2 · confidence는 AI가 아니라 입력 데이터 충족 개수로 결정한다.
def estimate_confidence(has_regional_actual: bool, has_history: bool, has_concentration: bool) -> str:
    satisfied = sum([has_regional_actual, has_history, has_concentration])
    if satisfied == 3:
        return "high"
    if satisfied == 2:
        return "medium"
    return "low"


def _unavailable_estimate(place_name: str, confidence: str) -> PlaceVisitorEstimate:
    # 방어 6 · confidence=low면 관광지 단위 값을 아예 표시하지 않는다.
    return PlaceVisitorEstimate(
        placeName=place_name,
        confidence=confidence,
        reasoning="추정 조건(시군구 실측·재개최 실적·집중률) 충족이 부족해 관광지 단위 추정을 표시하지 않습니다.",
        displayText="-",
        source="UNAVAILABLE",
    )


def estimate_place_visitors(context: dict, place: dict, errors: list | None = None) -> PlaceVisitorEstimate:
    """관광지 1곳의 방문 인원 범위를 추정한다. 조건 미달이거나 호출 실패면 미표시로 폴백한다."""
    confidence = estimate_confidence(
        context.get("regionalVisitorAverage") is not None,
        context.get("lastYearVisitors") is not None,
        place.get("peakRate") is not None,
    )
    if confidence == "low":
        return _unavailable_estimate(place["placeName"], confidence)

    user = (
        f"축제명: {context.get('festivalName')}\n"
        f"개최기간: {context.get('startDate')} ~ {context.get('endDate')}\n"
        f"축제 규모: {context.get('scale')} / 유형: {context.get('festivalType')}\n"
        f"시군구 방문자 실측 평균(API #7): {context.get('regionalVisitorAverage')}\n"
        f"작년 축제 방문객수(CSV #9): {context.get('lastYearVisitors')}\n"
        f"관광지명: {place['placeName']}\n"
        f"관광지 집중률 최고값: {place.get('peakRate')} (자기평균 대비 상승폭 {place.get('increasePoint')}%p)\n"
        f"최대 발생일: {place.get('peakDate')}\n"
    )
    result = _call(_ESTIMATE_SYSTEM, user, "place_visitor_estimate", _ESTIMATE_SCHEMA, errors)
    if not result:
        return _unavailable_estimate(place["placeName"], "low")

    minimum, maximum = int(result["min"]), int(result["max"])
    return PlaceVisitorEstimate(
        placeName=place["placeName"],
        min=minimum,
        max=maximum,
        confidence=confidence,
        reasoning=result["reasoning"],
        displayText=f"{minimum:,}~{maximum:,}명 ({ESTIMATE_DISCLAIMER})",
        source="AI",
    )


# ------------------------------------------------- 방향 A · 리스크 심각도 판정

_SEVERITY_SYSTEM = (
    "당신은 지자체 축제 리스크 심사자입니다. 규칙 엔진이 이미 매칭한 리스크의 "
    "심각도만 축제 컨텍스트를 반영해 3단계로 판정합니다. 매칭 자체를 바꾸지 마세요.\n"
    + _COMMON_RULES
    + "- critical=즉시 대응, warning=검토 권고, info=참고 정보입니다.\n"
    "- reason은 1~2문장으로 판정 근거만 쓰세요.\n"
)

_SEVERITY_SCHEMA = {
    "type": "object",
    "properties": {
        "severity": {"type": "string", "enum": ["critical", "warning", "info"]},
        "reason": {"type": "string", "description": "판정 근거 1~2문장"},
    },
    "required": ["severity", "reason"],
}

_RULE_SEVERITY = {"CRITICAL": "critical", "WARNING": "warning", "INFO": "info"}


def judge_risk_severity(context: dict, risk: dict, errors: list | None = None) -> RiskSeverityJudgement:
    """활성 리스크 1건의 심각도를 판정한다. 실패 시 규칙 정의 등급을 그대로 쓴다."""
    rule_log = f"{risk['riskCode']} 매칭됨 · {risk.get('metricKey')}={risk.get('metricValue')}"
    rule_severity = _RULE_SEVERITY.get((risk.get("severity") or "").upper(), "warning")

    user = (
        f"축제명: {context.get('festivalName')}\n"
        f"축제 규모: {context.get('scale')} / 유형: {context.get('festivalType')}\n"
        f"시군구 방문자 실측 평균(API #7): {context.get('regionalVisitorAverage')}\n"
        f"작년 축제 방문객수(CSV #9): {context.get('lastYearVisitors')}\n"
        f"리스크 코드: {risk['riskCode']}\n"
        f"리스크 내용: {risk.get('title')} — {risk.get('description')}\n"
        f"규칙 정의 등급: {risk.get('severity')}\n"
        f"근거: {risk.get('evidence')}\n"
    )
    result = _call(_SEVERITY_SYSTEM, user, "risk_severity", _SEVERITY_SCHEMA, errors)
    if not result:
        return RiskSeverityJudgement(
            riskCode=risk["riskCode"],
            severity=rule_severity,
            reason="AI 판정을 사용할 수 없어 규칙 정의 등급을 그대로 표시합니다.",
            ruleMatchLog=rule_log,
            source="RULE_FALLBACK",
        )
    return RiskSeverityJudgement(
        riskCode=risk["riskCode"],
        severity=result["severity"],
        reason=result["reason"],
        ruleMatchLog=rule_log,
        source="AI",
    )


# ------------------------------------------------- 방향 C · 리포트 상단 브리핑

_BRIEFING_SYSTEM = (
    "당신은 지자체 축제 담당 실무자에게 리포트 요약을 전달하는 분석가입니다.\n"
    + _COMMON_RULES
    + "- 3~5문장으로 쓰세요.\n"
    "- 형식: '이번 개최는 [규모]. 핵심 리스크 [N건]. 주의 대상 [관광지]. 참고 [연계 축제]. 권고 조치 요약.'\n"
)

_BRIEFING_SCHEMA = {
    "type": "object",
    "properties": {"text": {"type": "string", "description": "3~5문장 브리핑"}},
    "required": ["text"],
}


def build_briefing(context: dict, errors: list | None = None) -> ReportBriefing:
    """4개 뷰·리스크·재개최 실적을 3~5문장으로 요약한다. 실패 시 브리핑을 표시하지 않는다."""
    user = (
        f"축제명: {context.get('festivalName')}\n"
        f"개최기간: {context.get('startDate')} ~ {context.get('endDate')} ({context.get('diagnosisTiming')})\n"
        f"축제 규모: {context.get('scale')} / 유형: {context.get('festivalType')}\n"
        f"작년 방문객수(CSV #9): {context.get('lastYearVisitors')}\n"
        f"핵심 팩트: {context.get('keyFacts')}\n"
        f"활성 리스크: {context.get('riskSummaries')}\n"
        f"급상승 예상 관광지: {context.get('volatilityPlaces')}\n"
        f"동기간 인근 축제: {context.get('nearbyFestivals')}\n"
    )
    result = _call(_BRIEFING_SYSTEM, user, "report_briefing", _BRIEFING_SCHEMA, errors)
    if not result:
        return ReportBriefing(text=None, source="UNAVAILABLE")
    return ReportBriefing(text=result["text"], source="AI")
