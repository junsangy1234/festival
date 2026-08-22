# DTO API 계약 문서

이 문서는 축제날씨 서비스의 Request/Response DTO 형태와 API 예시를 정리한다. DTO를 추가하거나 필드를 변경할 때는 반드시 이 문서도 함께 갱신한다.

> **Last updated:** 2026-08-14

---

## DTO 패키지 구조

DTO는 도메인별로 `domain/<domain>/dto/request`, `domain/<domain>/dto/response`에 둔다. 외부 API 요청·응답 변환 DTO는 `infra/<provider>/dto/<feature>`에 둔다.

도메인 객체나 외부 제공자 DTO를 API 응답 DTO로 변환해야 하면 DTO에 `static from(...)` 팩토리 메서드를 둔다. 단순 목록을 감싸는 DTO처럼 별도 변환 대상이 없는 경우에는 생성자로 만든다.

외부 API DTO는 프론트엔드 계약이 아니다. 관광공사 필드명과 구조를 보존하는 용도이고, 서비스 계층에서 우리 도메인 응답 DTO로 변환한다.

- `TourFestivalClient`는 축제 API를 `infra/tourapi/dto/festival`의 타입 DTO로 반환한다.
- 실제 진단에서 사용하는 외부 API는 기능별 클라이언트(`TourConcentrationClient`, `TourRelatedPlaceClient`, `TourRegionalVisitorClient`, `TourFestivalClient`)로만 둔다.

```text
domain/
├─ diagnosis/
│  ├─ controller/DiagnosisController
│  ├─ service/DiagnosisService
│  ├─ analysis/
│  ├─ entity/Diagnosis
│  ├─ repository/DiagnosisRepository
│  ├─ calculation/
│  ├─ collection/
│  ├─ risk/
│  ├─ recommendation/
│  ├─ policy/
│  ├─ region/
│  └─ dto/
│     ├─ request/DiagnosisRequest
│     └─ response/DiagnosisDashboardResponse
├─ festival/dto/
│  ├─ request/
│  │  └─ FestivalSearchRequest
│  └─ response/
│     ├─ FestivalSummaryResponse
│     ├─ FestivalListResponse
│     ├─ FestivalDetailResponse
│     ├─ FestivalInformationResponse
│     ├─ FestivalImageResponse
│     └─ CompetingFestivalResponse

infra/tourapi/dto/festival/
├─ TourApiEnvelope, TourApiBody, TourApiItems
├─ TourFestivalSearchApiResponse, TourFestivalSearchItem
├─ TourFestivalCommonApiResponse, TourFestivalCommonItem
├─ TourFestivalIntroApiResponse, TourFestivalIntroItem
├─ TourFestivalDetailInfoApiResponse, TourFestivalDetailInfoItem
└─ TourFestivalImageApiResponse, TourFestivalImageItem
```

## 목차

- [DiagnosisRequest](#diagnosisrequest)
- [Diagnosis region code APIs](#diagnosis-region-code-apis)
- [Diagnosis APIs](#diagnosis-apis)
- [FestivalSearchRequest / FestivalListResponse](#festivalsearchrequest--festivallistresponse)
- [FestivalDetailResponse](#festivaldetailresponse)
- [CompetingFestivalResponse](#competingfestivalresponse)
- [ApiError](#apierror)

---

## DiagnosisRequest

### 사용처

축제 진단 생성 API에서 사용하는 입력 DTO다. 입력값은 DB에 저장되고, 4개 데이터 뷰·리스크 규칙의 계산 결과를 같은 `reportId`에 연결한다.

### Request 예시

```json
{
  "festivalName": "원주 여름 음악축제",
  "areaCode": "51",
  "signguCode": "51130",
  "startDate": "2026-08-20",
  "endDate": "2026-08-23",
  "festivalType": "문화예술",
  "scale": "중규모",
  "recurrenceType": "신규",
  "existingFestivalContentId": null,
  "festivalAddress": "강원특별자치도 원주시 지정면 소금산로 12",
  "latitude": 37.3682,
  "longitude": 127.8166
}
```

| 필드 | 필수 | 의미 |
| --- | --- | --- |
| `festivalName` | 예 | 화면과 리포트에 표시할 축제명 |
| `areaCode` | 예 | 광역시도 코드. 집중률 API의 `areaCd`에 전달 |
| `signguCode` | 예 | 시군구 코드. 집중률 API의 `signguCd`에 전달 |
| `startDate` | 예 | 개최 시작일, `yyyy-MM-dd`. 30일 예측 범위 밖도 요청 가능하며 집중률 상태로 구분 |
| `endDate` | 예 | 개최 종료일, `yyyy-MM-dd`. 시작일보다 빠를 수 없고 축제 기간은 최대 14일 |
| `festivalType` | 예 | `문화예술`, `전통역사`, `생태자연`, `특산물`, `기타`. 영문 enum 코드도 허용 |
| `scale` | 예 | `소규모`, `중규모`, `대규모`. 영문 enum 코드도 허용 |
| `recurrenceType` | 예 | `신규`, `재개최`. 영문 enum 코드도 허용 |
| `existingFestivalContentId` | 조건부 | 재개최 축제일 때 기존 관광공사 축제 `contentId` |
| `festivalAddress` | 신규 축제 필수 | 축제장 주소. 재개최은 국문 관광정보 API로 보완 가능 |
| `latitude` | 신규 축제 필수 | 축제장 위도. `-90~90` 범위 |
| `longitude` | 신규 축제 필수 | 축제장 경도. `-180~180` 범위 |

현재 DTO에서 다음 기본 검증을 수행한다.

- 종료일은 시작일보다 빠를 수 없다.
- 시작일이 집중률 API의 향후 30일 범위 밖이어도 요청을 거절하지 않고 `OUT_OF_FORECAST_RANGE`로 반환한다.
- 축제 기간은 최대 14일이다.
- 재개최 축제는 `existingFestivalContentId`가 필요하다.
- 신규 축제는 `existingFestivalContentId`를 입력할 수 없다.
- 신규 축제는 축제장 주소·위도·경도를 입력해야 한다.

## Diagnosis region code APIs

진단 입력 화면은 지역명을 보여주고, 관광지 집중률·연관 관광지·방문자 API가 요구하는 `areaCode`, `signguCode`를 선택값으로 사용한다. 기존 관광 권역·법정동 코드 API와 혼용하지 않는다.

| API | 용도 |
| --- | --- |
| `GET /api/v1/diagnosis-regions` | 진단용 시도 목록 |
| `GET /api/v1/diagnosis-regions/{areaCode}/districts` | 선택 시도의 진단용 시군구 목록 |

### 시도 목록 예시

```json
{
  "regions": [
    { "areaCode": "51", "name": "강원특별자치도" }
  ]
}
```

### 시군구 목록 예시

```json
{
  "areaCode": "51",
  "areaName": "강원특별자치도",
  "districts": [
    { "signguCode": "51130", "name": "원주시" }
  ]
}
```

코드표 원본은 한국관광공사 관광지 집중률 API 매뉴얼의 `한국관광공사_OpenAPI_관광지_시군구_코드정보_v1.0.xlsx`이며, 배포 시 외부 파일 의존을 없애기 위해 `src/main/resources/diagnosis-region-codes.csv`로 포함했다.

## 외부 API 파일 캐시

집중률 예측 API처럼 동일한 지역을 반복 조회할 수 있는 외부 데이터는 원본 JSON을 파일에 저장한다. 캐시는 `FileJsonCache`가 담당하며, 호출 파라미터를 정렬해 만든 SHA-256 키로 파일을 구분한다. 따라서 파라미터를 전달한 순서가 달라도 같은 캐시를 재사용한다.

| 설정 | 기본값 | 의미 |
| --- | --- | --- |
| `festival.cache.root-directory` | `./cache` | JSON 캐시 파일을 저장할 루트 경로 |
| `festival.cache.concentration-forecast-ttl` | `PT24H` | 향후 집중률 예측 데이터 유효 시간 |
| `festival.cache.related-places-ttl` | `P30D` | 연관 관광지 데이터 유효 시간 |
| `festival.cache.regional-visitors-ttl` | `PT24H` | 시군구 일별 방문자 데이터 유효 시간 |
| `festival.cache.festival-search-ttl` | `PT1H` | 경합 축제 후보 검색 데이터 유효 시간 |
| `festival.external-data.related-base-year-month` | `202503` | 연관 관광지 API의 설정 기반 `baseYm` |
| `festival.external-data.visitor-reference-years-back` | `1` | 미래 축제와 비교할 과거 방문자 기준 연도 차이 |

Docker 환경에서는 `FESTIVAL_CACHE_ROOT=/app/cache`로 지정하며, Docker volume에 연결해 컨테이너를 다시 만들어도 캐시를 유지한다.

## Diagnosis APIs

### 사용처

사용자 입력을 저장하고 M2 대시보드 결과를 제공한다.

| API | 설명 | 현재 상태 |
| --- | --- | --- |
| `POST /api/v1/reports` | 진단 요청 생성 | 입력 저장 후 수집·정제·정책 매칭까지 실행하고 최종 처리 상태 반환 |
| `GET /api/v1/reports/{reportId}` | 입력값·처리 상태 조회 | 구현 완료 |
| `GET /api/v1/reports/{reportId}/dashboard` | M2 대시보드 결과 조회 | 실제 데이터, 데이터별 상태, 지도 표현, 리스크·운영 제안 반환 |
| `GET /api/v1/reports/{reportId}/forecast-report` | M3 리포트(6개 섹션) 조회 | 히어로·A4 요약본·데이터 요약·리스크·운영 제안·근거 반환 |

개발 중에는 `GET /diagnosis-test.html`로 간단한 임시 테스트 화면을 열 수 있다. 이 화면은 `POST /api/v1/reports` 후 받은 `reportId`로 대시보드 API를 즉시 호출해 원본 JSON 응답을 표시한다.

MVP 데이터 스택은 API #1·#2·#6·#7·#8 + CSV #9다. API #3·#4·#5는 완전 폐기해 호출하지 않는다.
M2 대시보드 응답은 아래 영역으로 구성한다. 종합 지수·순위·등급은 만들지 않고 각 축의 원본 값만 반환한다.

- `profile`: 수평 막대 4축(시기적합도 0~100, 여유 관광지 수 0~10, 연계 풍부도 1~50 순위, 카테고리 다양성)과 필수 각주 3줄(`notes`)
- `festivalLocation`: 축제장 좌표와 출처(`KOR_SERVICE`/`USER_INPUT`/`SIGNGU_CENTER`/`UNAVAILABLE`)·정밀도·안내 문구
- `map`: 지도 표현용 축제장 마커, 관광지 마커(API #6 좌표 × API #1 배지), 인근 축제 마커, 최인접 반경 1km
- `concentration`: 개최기간 관광 흐름(View 01)
- `volatility`: 관광지 변동 상황(View 02)
- `distribution`: 여유 관광지(View 03)
- `regionalVisitors`: 시군구 날짜별 현지인·외지인·외국인·전체 방문자와 기간 전후 평균
- `relatedPlaces`: 기준 관광지별 연관 관광지 순위와 카테고리
- `competing`: 동기간 인근 축제(View 04). API #8 정보에 CSV #9 실측(방문객수·예산)과 연계 태그를 조인
- `festivalHistory`: CSV #9 재개최 실적(작년 방문객수·예산·최초 개최연도)
- `dataStatuses`: 외부 데이터별 `AVAILABLE`, `NO_DATA`, `OUT_OF_FORECAST_RANGE`, `FAILED` 상태와 사유·기준 시점
- `risks`, `recommendations`: YAML 정책에 매칭된 리스크와 기본 운영 제안

`ConcentrationAnalysisService`는 API #1 원본을 아래와 같이 정제한다.

- 개최기간 전체 평균 집중률
- 날짜별 관광지 집중률과 해당 관광지의 자기평균(26일 예측 평균) 대비 변화
- 관광지별 개최기간 최고 상승폭·최고 상승일·`STABLE`/`WARNING`/`SURGING` 배지
- 개최기간 평균이 자기평균보다 `-5%p` 이하이고 집중률 `40` 이하인 여유 관광지 후보

여유 후보는 집중률 조건으로 만들고, API #2의 연관 순위·카테고리는 `relatedPlaces`에서 기준 관광지별 목록으로 별도 반환한다. 프론트는 관광지명이 일치할 때 두 결과를 함께 표시할 수 있다.

### M2 응답 핵심 구조

```json
{
  "reportId": "uuid",
  "status": "PARTIAL",
  "diagnosis": {},
  "festivalLocation": {
    "latitude": 36.3607306,
    "longitude": 127.3577063,
    "source": "USER_INPUT",
    "precise": true,
    "address": "대전광역시 유성구 어은로 27",
    "notice": null
  },
  "map": {
    "site": {},
    "nearestRadiusKm": 1.0,
    "places": [],
    "nearbyFestivals": [],
    "notice": null
  },
  "dataStatuses": [
    {
      "source": "concentration",
      "status": "OUT_OF_FORECAST_RANGE",
      "reason": "관광지 집중률은 조회일 기준 향후 26일(D+0~D+25)만 제공합니다.",
      "referencePeriod": "2026-10-01~2026-10-03",
      "retrievedAt": "2026-08-14T08:00:00Z"
    }
  ],
  "profile": {},
  "concentration": { "dailyConcentrations": [] },
  "volatility": { "summary": null, "places": [] },
  "distribution": { "summary": null, "places": [] },
  "regionalVisitors": { "dailyVisitors": [] },
  "relatedPlaces": { "basePlaces": [] },
  "competing": {
    "festivals": [],
    "excludedMissingCoordinatesCount": 0
  },
  "festivalHistory": null,
  "risks": [],
  "recommendations": []
}
```

`dataStatuses.status`는 다음 의미다.

| 값 | 의미 |
| --- | --- |
| `AVAILABLE` | 정상 조회·정제 완료 |
| `NO_DATA` | API 호출은 성공했지만 조건에 맞는 데이터 없음 |
| `OUT_OF_FORECAST_RANGE` | 축제 시작일이 집중률 예측 범위 밖 |
| `FAILED` | 호출·응답 변환·캐시 처리 실패 |

`RiskResponse`는 `riskCode`, `severity`, `priority`, `title`, `description`, `metricKey`, `metricValue`, `evidence`, `recommendationCodes`를 반환한다. `RecommendationResponse`는 `recommendationCode`, `priority`, `category`, `difficulty`, `title`, `defaultAction`, `relatedRiskCodes`, `evidenceValues`를 반환한다.

### M2 목록 제한

대시보드 응답은 원본 전체 목록을 무제한으로 반환하지 않는다. 화면의 첫 표시에는 아래 기본 개수만 반환하고, `totalCount` 계열 필드로 전체 건수를 함께 준다. 추후 더보기 화면이 필요하면 reportId 기반 상세·페이지네이션 API를 별도로 추가한다.

| 영역 | 기본 반환 수 | 전체 건수 필드 |
| --- | ---: | --- |
| `concentration.dailyConcentrations[].places` | 변동성 상위 10개 관광지 | `totalPlaceCount` |
| `volatility.places` | 10개 | `totalCount` |
| `distribution.places` | 6개 | `totalCount` |
| `relatedPlaces.basePlaces` | 기준 관광지 10개 | `totalBasePlaceCount` |
| `relatedPlaces.basePlaces[].relatedPlaces` | 기준 관광지별 10개 | `totalRelatedPlaceCount` |
| `competing.festivals` | 10개 | `totalCount`, `displayedCount` |

제한값은 `application.yaml`의 `festival.dashboard.*`에서 변경한다.

### 생성 Response 예시

```json
{
  "reportId": "uuid",
  "status": "COMPLETED",
  "festivalName": "원주 여름 음악축제",
  "areaCode": "51",
  "signguCode": "51130",
  "festivalType": "문화예술",
  "scale": "중규모",
  "recurrenceType": "신규"
}
```

## 리스크·운영 제안 정책

리스크 조건은 [risk-rules.yml](/Users/youjunsang/Programming/project/festival/src/main/resources/policies/risk-rules.yml), 운영 제안은 [recommendation-catalog.yml](/Users/youjunsang/Programming/project/festival/src/main/resources/policies/recommendation-catalog.yml)에서 관리한다.

- 정제 서비스는 `DiagnosisMetric`으로 `metricKey`, 수치값, 관광지명·날짜 같은 근거 속성을 전달한다.
- 리스크 규칙은 `riskCode`, `severity`, `priority`, `metricKey`, 제한된 비교 연산자, `threshold`, 근거 필드, 연결 제안 코드, `enabled`를 가진다.
- 운영 제안은 `recommendationCode`, `priority`, 제목, 기본 행동 문구, 연결 리스크, `enabled`를 가진다.
- 알 수 없는 지표, 데이터가 없는 지표, `enabled: false` 규칙은 안전하게 건너뛴다.
- 운영 제안 응답에는 이후 Claude에 전달할 수 있도록 기본 행동 문구와 근거 수치를 포함하지만 Java 백엔드는 Claude를 호출하지 않는다.
- 위치가 필요한 규칙(R-VOL-005·O-INF-003)은 `festivalLocation.precise`가 `false`면 지표 자체를 만들지 않아 자동으로 건너뛴다.

### M3 리포트 구조

`GET /api/v1/reports/{reportId}/forecast-report`는 세로 스크롤 6개 섹션을 그대로 돌려준다.

| 섹션 | 필드 | 내용 |
| --- | --- | --- |
| §1 히어로 | `hero` | 축제명·개최기간·`diagnosisTiming`(D-X)·`forecastDataActive`·시점별 활용 데이터·`briefing`(AI 방향 C가 채움) |
| §2 A4 요약본 | `summarySheet` | 핵심 팩트, 리스크 3건, 운영 제안 3건, 데이터 출처·한계 문구 |
| §3 데이터 요약 | `dataSummary` | 수평 막대 4축과 4개 데이터 뷰 |
| §4 리스크 | `risks` | 규칙 엔진 매칭 결과 |
| §5 운영 조정 제안 | `operationProposal` | 실무자 판단 우선 문구와 제안 체크리스트 |
| §6 근거 | `evidence` | 데이터별 상태·기준 시점 문구·축제장 위치 안내·데이터 한계 문구 |

종합 점수·등급·5각 축 분해는 반환하지 않는다.

## 클라이언트 설정

`GET /api/v1/client-config` → `{"googleMapsApiKey": "..."}`

테스트 콘솔이 Google Maps JS API를 동적으로 불러올 때 쓴다. 값은 `GOOGLE_MAPS_API_KEY` 환경변수에서 온다.
브라우저 키는 공개되는 값이므로 Google Cloud 콘솔에서 HTTP 리퍼러 제한을 걸어야 한다. 값이 비어 있으면 지도 자리에 안내 문구만 표시한다.

## AI 서비스 (ai-service)

Part 6의 AI 4방향은 별도 FastAPI 서비스가 담당하고, Anthropic Claude Sonnet 4.5를 사용한다.
환경변수는 저장소 루트 `.env` 하나만 쓴다(`ANTHROPIC_API_KEY`, `ANTHROPIC_MODEL`, `JAVA_BACKEND_BASE_URL`). 로컬 실행은 `make run-ai`.

| API | 방향 | 폴백 |
| --- | --- | --- |
| `POST /api/v1/reports/{reportId}/ai-report` | B·A·C·D 병렬 실행 | 방향별 폴백 |
| `POST /api/v1/reports/{reportId}/recommendations/expand` | D 단독 | 규칙 원문 |

- 방향 B(관광지 방문 인원 추정)의 `confidence`는 AI가 아니라 입력 데이터 충족 개수로 결정하고, `low`면 추정치를 표시하지 않는다.
- 방향 A(리스크 심각도 판정)는 규칙 매칭 원문 로그(`ruleMatchLog`)를 함께 반환해 재현성을 유지한다.
- 호출 실패 시 방향 D는 규칙 원문, 방향 A는 규칙 정의 등급, 방향 B·C는 미표시로 폴백한다.

## FestivalSearchRequest / FestivalListResponse

### 사용처

`GET /api/v1/festivals`

기간을 기준으로 축제 목록을 조회한다. `legalDongRegionCode`, `legalDongSignguCode`는 선택 입력으로 직접 전달할 수 있는 법정동 코드이며, 최신 축제 데이터의 지역 필터에 사용한다.

### Request 예시

```text
GET /api/v1/festivals?startDate=20260901&endDate=20260930&legalDongRegionCode=48&legalDongSignguCode=880&size=20
```

| 파라미터 | 필수 | 의미 |
| --- | --- | --- |
| `startDate`, `endDate` | 예 | 조회 기간. `yyyyMMdd` 형식 |
| `areaCode`, `sigunguCode` | 아니오 | 관광 권역 코드 필터 |
| `legalDongRegionCode`, `legalDongSignguCode` | 아니오 | 법정동 지역·시군구 코드 필터 |
| `pageNo` | 아니오 | 페이지 번호, 기본값 1 |
| `size` | 아니오 | 페이지 크기, 기본값 20 |

`endDate`가 `startDate`보다 이르면 `400 Bad Request`를 반환한다.

### Response 예시

```json
{
  "festivals": [
    {
      "contentId": "2746930",
      "title": "감악산 꽃별 여행",
      "eventStartDate": "20260918",
      "eventEndDate": "20261011",
      "areaCode": "",
      "sigunguCode": "",
      "legalDongRegionCode": "48",
      "legalDongSignguCode": "880",
      "address": "경상남도 거창군 신원면 연수사길 452",
      "longitude": "127.9175083031",
      "latitude": "35.5896217023",
      "firstImage": "https://...",
      "telephone": "055-940-8227"
    }
  ],
  "pageNo": 1,
  "size": 20,
  "totalCount": 1
}
```

| 필드 | 의미 |
| --- | --- |
| `festivals` | 축제 요약 목록 |
| `contentId` | 관광공사가 발급한 축제 콘텐츠 식별자 |
| `eventStartDate`, `eventEndDate` | 축제 기간, `yyyyMMdd` 형식 |
| `areaCode`, `sigunguCode` | 관광 권역 코드. 최신 축제 데이터에서는 빈 문자열일 수 있음 |
| `legalDongRegionCode`, `legalDongSignguCode` | 축제의 법정동 시도·시군구 코드 |
| `address`, `addressDetail` | 축제 주소 |
| `longitude`, `latitude` | 축제 위치 좌표 |
| `firstImage`, `thumbnailImage` | 대표·썸네일 이미지 URL |
| `telephone` | 축제 문의 전화번호 |
| `pageNo`, `size`, `totalCount` | 관광공사 검색 결과의 페이지 정보 |

---

## FestivalDetailResponse

### 사용처

`GET /api/v1/festivals/{contentId}`

축제 기본 정보, 행사 정보, 상세 안내와 이미지를 통합해 반환한다.

### Response 예시

```json
{
  "contentId": "2746930",
  "title": "감악산 꽃별 여행",
  "eventStartDate": "20260918",
  "eventEndDate": "20261011",
  "eventPlace": "거창 별바람언덕",
  "program": "공연 및 체험 프로그램",
  "overview": "축제 소개",
  "information": [
    { "name": "행사소개", "content": "..." }
  ],
  "images": [
    {
      "imageUrl": "https://...",
      "thumbnailUrl": "https://...",
      "name": "축제 이미지"
    }
  ]
}
```

| 필드 묶음 | 포함 필드 |
| --- | --- |
| 기본 정보 | `contentId`, `title`, 주소, 좌표, 대표 이미지, 전화번호, 홈페이지, `overview` |
| 지역 코드 | `areaCode`, `sigunguCode`, `legalDongRegionCode`, `legalDongSignguCode` |
| 행사 정보 | `eventStartDate`, `eventEndDate`, `eventPlace`, `playtime`, `program`, `usageTime`, `festivalDuration` |
| 운영 주체 | `organizer`, `organizerTelephone`, `host`, `hostTelephone`, `eventHomepage` |
| 상세 목록 | `information[].name`, `information[].content`, `images[].imageUrl`, `images[].thumbnailUrl`, `images[].name` |

축제 기본·소개·상세 안내·이미지 API를 각각 호출하므로, 일부 원본 데이터가 없는 경우 `information` 또는 `images`는 빈 배열일 수 있다.

---

## CompetingFestivalResponse

### 사용처

`GET /api/v1/festivals/competing?contentId={contentId}`

대상 축제와 기간이 겹치고 기준 좌표에서 직선거리 50km 이내인 축제를 반환한다. 대상 축제는 결과에서 제외한다.

### Response 예시

```json
{
  "targetContentId": "2746930",
  "festivals": [],
  "count": 0,
  "excludedMissingCoordinatesCount": 0
}
```

| 필드 | 의미 |
| --- | --- |
| `targetContentId` | 경쟁 축제를 계산한 기준 축제의 콘텐츠 ID |
| `festivals` | 기간이 하루 이상 겹치고 50km 이내인 다른 축제 요약 목록. 각 항목에 `distanceKm` 포함 |
| `count` | 경쟁 축제 수 |
| `excludedMissingCoordinatesCount` | 좌표가 없어 거리 판정에서 제외된 후보 수 |

후보 검색 단계에서는 지역을 제한하지 않는다. 날짜 조건으로 전체 후보를 페이지 끝까지 조회한 뒤 Haversine 공식으로 거리를 계산하고, 50km 초과 후보와 좌표 없는 후보를 제외한다. 거리는 km 단위 소수점 한 자리로 반환한다.

---

## ApiError

### 사용처

관광공사 호출 실패 시 공통으로 반환한다.

### Response 예시

```json
{
  "timestamp": "2026-08-11T05:30:00Z",
  "status": 502,
  "message": "관광공사 API 호출에 실패했습니다. HTTP 500"
}
```
