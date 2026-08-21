"""M3 리포트 JSON에서 AI 4방향이 공통으로 쓰는 축제 컨텍스트를 뽑는다."""


def build_context(report: dict) -> dict:
    hero = report.get("hero") or {}
    summary = report.get("summarySheet") or {}
    data = report.get("dataSummary") or {}
    history = data.get("festivalHistory") or {}
    visitors = data.get("regionalVisitors") or {}
    volatility = (data.get("volatility") or {}).get("places") or []
    nearby = (data.get("nearbyFestivals") or {}).get("festivals") or []
    risks = report.get("risks") or []

    return {
        "festivalName": summary.get("festivalName") or hero.get("festivalName"),
        "startDate": summary.get("startDate") or hero.get("startDate"),
        "endDate": summary.get("endDate") or hero.get("endDate"),
        "festivalType": summary.get("festivalType"),
        "scale": summary.get("scale"),
        "diagnosisTiming": hero.get("diagnosisTiming"),
        "regionalVisitorAverage": visitors.get("festivalPeriodAverage"),
        "lastYearVisitors": history.get("lastYearVisitors"),
        "keyFacts": summary.get("keyFacts") or [],
        "riskSummaries": [f"{risk.get('riskCode')} {risk.get('title')}" for risk in risks],
        "volatilityPlaces": [
            f"{place.get('placeName')} +{place.get('increasePoint')}%p" for place in volatility[:5]
        ],
        "nearbyFestivals": [festival.get("festivalName") for festival in nearby[:5]],
    }


# 방향 B는 뷰 02 배지 리스트 TOP N만 추정한다(기획서 뷰 02).
def top_volatility_places(report: dict, limit: int = 10) -> list[dict]:
    places = ((report.get("dataSummary") or {}).get("volatility") or {}).get("places") or []
    return places[:limit]


# M3 체크리스트 항목엔 relatedRiskCodes가 이미 들어 있지만, 비어 있으면 리스크에서 역매핑한다.
def related_risk_codes(item: dict, risks: list[dict]) -> list[str]:
    if item.get("relatedRiskCodes"):
        return item["relatedRiskCodes"]
    return [
        risk["riskCode"]
        for risk in risks
        if item["recommendationCode"] in (risk.get("recommendationCodes") or [])
    ]
