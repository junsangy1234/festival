"""API 키가 없을 때 Claude 호출이 죽지 않고 규칙 원문·미표시로 폴백하는지 확인하는 최소 체크."""
from app import claude_service
from app.claude_service import estimate_confidence
from app.config import settings


def _no_api_key():
    settings.anthropic_api_key = ""


def test_recommendation_falls_back_to_rule_text_without_api_key():
    _no_api_key()
    recommendation = {
        "recommendationCode": "O-INF-001",
        "title": "셔틀 증차 및 주차 분산 안내",
        "defaultAction": "급상승 관광지 주변 셔틀을 증차하고 주차 분산 안내를 제공한다.",
        "difficulty": "하",
        "category": "인프라",
        "relatedRiskCodes": ["R-VOL-001"],
    }

    result = claude_service.expand_recommendation({"festivalName": "테스트 축제"}, recommendation)

    assert result.source == "RULE_FALLBACK"
    assert "셔틀 증차 및 주차 분산 안내" in result.coreAction
    assert result.parkingDistribution is None
    assert result.relatedRiskCodes == ["R-VOL-001"]


def test_risk_severity_falls_back_to_rule_severity():
    _no_api_key()
    risk = {
        "riskCode": "R-VOL-001",
        "severity": "CRITICAL",
        "title": "구룡사 급상승 예상",
        "description": "상승폭 +48%p",
        "metricKey": "VOLATILITY_PEAK_INCREASE",
        "metricValue": 48,
    }

    result = claude_service.judge_risk_severity({"festivalName": "테스트 축제"}, risk)

    assert result.source == "RULE_FALLBACK"
    assert result.severity == "critical"
    assert "R-VOL-001 매칭됨" in result.ruleMatchLog


def test_briefing_is_hidden_without_api_key():
    _no_api_key()

    result = claude_service.build_briefing({"festivalName": "테스트 축제"})

    assert result.source == "UNAVAILABLE"
    assert result.text is None


def test_low_confidence_estimate_is_not_displayed():
    _no_api_key()
    # 시군구 실측·재개최 실적·집중률 중 1개만 충족 -> low -> 방어 6에 따라 미표시
    context = {"regionalVisitorAverage": None, "lastYearVisitors": None}
    place = {"placeName": "구룡사", "peakRate": 95.58}

    result = claude_service.estimate_place_visitors(context, place)

    assert result.confidence == "low"
    assert result.source == "UNAVAILABLE"
    assert result.min is None
    assert result.displayText == "-"


def test_confidence_levels():
    assert estimate_confidence(True, True, True) == "high"
    assert estimate_confidence(True, False, True) == "medium"
    assert estimate_confidence(False, False, True) == "low"
