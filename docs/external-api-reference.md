# 외부 데이터 API 명세

축제날씨에서 서버가 호출하는 외부 API 8개를 정리한다. `핵심`은 현재 M2/M3 구현에 바로 연결하는 데이터이고, `확장`은 화면 또는 정책이 추가될 때 연결한다. API가 아닌 축제 개최 계획 파일은 [참고 데이터 명세](festival-plan-reference-data.md)로 분리한다.

## 공통 호출 규칙

- 인증키는 환경변수 `TOUR_API_KEY`로 관리한다. 코드·문서·Git에 실제 키를 넣지 않는다.
- 공공데이터포털 API는 `serviceKey` 쿼리 파라미터를 사용한다.
- 공통 파라미터는 `MobileOS=ETC`, `MobileApp=FestivalWeather`, `_type=json`이다.
- 목록 API는 `pageNo`, `numOfRows`, `totalCount`를 기준으로 마지막 페이지까지 호출한다.
- 원본 API 응답은 캐시하고, 화면용 수치는 서비스 계층에서 가공한다.
- 지역 코드는 `areaCd`(광역 시·도), `signguCd` 또는 `signguCode`(시·군·구)다. API마다 필드 철자가 다르므로 그대로 맞춘다.
- 진단 입력 화면은 `GET /api/v1/diagnosis-regions`와 `GET /api/v1/diagnosis-regions/{areaCode}/districts`로 이 코드표를 선택한다. 관광 권역·법정동 코드와 혼용하지 않는다.

## 전체 목록

| 번호 | 데이터 | 상태 | 현재 사용처 |
|---:|---|---|---|
| 1 | 관광지 집중률 방문자 추이 예측 | 핵심 | 개최기간 추이, 변동성, 여유 관광지 |
| 2 | 관광지별 연관 관광지 | 핵심 | 연관 관광지·카테고리·분산 후보 |
| 3 | 지역별 문화 자원 수요 | 확장 | 지역 수요 보조 지표 |
| 4 | 지역별 관광 서비스 수요 | 확장 | 지역 수요 보조 지표 |
| 5 | 지역별 관광 다양성 | 확장 | 관광객·소비·국제 다양성 지표 |
| 6 | 기초 지자체 중심 관광지 | 확장 | 지역 중심 관광지 참고 |
| 7 | 지역별 방문자 수 | 핵심 | 축제 개최 시·군·구 방문 수요 추이 |
| 8 | 국문 관광정보 서비스 | 핵심 | 기존 축제 선택, 경합 축제 조회 |

## 현재 서버 연동 상태

| 번호 | 구현 | 페이지네이션 | 원본 캐시 | 비고 |
|---:|---|---|---|---|
| 1 | `TourConcentrationClient` | `totalCount`까지 반복 | `concentration-forecast` | 시작일이 조회일 기준 향후 30일 밖이면 `OUT_OF_FORECAST_RANGE` |
| 2 | `TourRelatedPlaceClient` | `totalCount`까지 반복 | `related-tourist-places` | `baseYm`은 `festival.external-data.related-base-year-month` 설정 사용 |
| 3~6 | API 명세만 보관 | 미연결 | 미연결 | 현재 M2 핵심 흐름에서 사용하지 않음 |
| 7 | `TourRegionalVisitorClient` | `totalCount`까지 반복 | `local-region-visitors` | 관광지별 값이 아니라 시군구 전체 방문자 값 |
| 8 | `TourFestivalClient` | 경합 후보 검색 시 `totalCount`까지 반복 | `festival-search` | 기간 후보 조회 후 Haversine 50km 필터 적용 |

캐시 키는 API 종류와 실제 요청 조건 전체를 사용한다. 인증키는 캐시 키·파일·로그에 포함하지 않는다. API 실패, 정상 응답이지만 데이터 없음, 집중률 예측 범위 밖은 각각 `FAILED`, `NO_DATA`, `OUT_OF_FORECAST_RANGE`로 구분한다.

---

## 1. 관광지 집중률 방문자 추이 예측 정보

**상태: 핵심 · 실제 호출 및 응답 확인 완료**

축제 개최일을 포함한 향후 30일의 관광지별 집중률을 가져온다. 집중률은 실제 방문자 수가 아니라, 해당 관광지에서 가장 붐비는 시기를 100으로 둔 상대 지수다.

```http
GET https://apis.data.go.kr/B551011/TatsCnctrRateService/tatsCnctrRatedList
```

| 필수 파라미터 | 예시 | 설명 |
|---|---|---|
| `pageNo`, `numOfRows` | `1`, `2000` | 페이지 정보 |
| `areaCd`, `signguCd` | `51`, `51130` | 개최 지역 |
| `tAtsNm` | `구룡사` | 선택. 특정 관광지명 필터 |

| 응답 필드 | 변환 | 사용 |
|---|---|---|
| `baseYmd` | `yyyyMMdd → LocalDate` | 날짜별 추이·축제 기간 필터 |
| `tAtsNm` | `String` | 관광지명 |
| `cnctrRate` | `String → BigDecimal` | 집중률·상승폭·여유 관광지 판정 |
| `areaCd`, `signguCd` | `String` | 지역 식별 |

가공 결과는 관광지별 30일 평균, 축제 기간 평균, 최고 상승일·상승폭, 여유 관광지 후보이다.

### 성공 응답 예시

```json
{
  "response": {
    "header": { "resultCode": "0000", "resultMsg": "OK" },
    "body": {
      "totalCount": 1650,
      "numOfRows": 2000,
      "pageNo": 1,
      "items": {
        "item": [
          {
            "baseYmd": "20260822",
            "areaCd": "51",
            "areaNm": "강원특별자치도",
            "signguCd": "51130",
            "signguNm": "원주시",
            "tAtsNm": "구룡사",
            "cnctrRate": "95.58"
          }
        ]
      }
    }
  }
}
```

---

## 2. 관광지별 연관 관광지 정보

**상태: 핵심 · 실제 호출 및 응답 확인 완료**

시·군·구를 기준으로 기준 관광지와 연관 관광지의 연결 관계를 가져온다. 연관 순위와 카테고리를 M2의 연관 관광지·분산 후보 설명에 사용한다.

```http
GET https://apis.data.go.kr/B551011/TarRlteTarService1/areaBasedList1
```

| 필수 파라미터 | 예시 | 설명 |
|---|---|---|
| `pageNo`, `numOfRows` | `1`, `1000` | 페이지 정보 |
| `baseYm` | `202503` | 기준 연월, `yyyyMM` |
| `areaCd`, `signguCd` | `11`, `11530` | 개최 지역 |

| 응답 필드 | 변환 | 사용 |
|---|---|---|
| `tAtsCd`, `tAtsNm` | `String` | 기준 관광지별 관계 그룹화 |
| `rlteTatsCd`, `rlteTatsNm` | `String` | 연관 관광지 표시 |
| `rlteRegnNm`, `rlteSignguNm` | `String` | 연관 관광지 지역 설명 |
| `rlteCtgryLclsNm` | `String` | 관광지·음식·숙박 대분류 |
| `rlteCtgryMclsNm`, `rlteCtgrySclsNm` | `String` | 세부 카테고리 |
| `rlteRank` | `String → int` | 연관 순위·정렬 |

실제 응답 기준으로 `totalCount=298`처럼 여러 페이지가 생긴다. 항상 마지막 페이지까지 조회한다. `searchKeyword1`은 특정 관광지를 사용자가 검색하는 기능이 필요할 때만 추가한다.

### 성공 응답 예시

```json
{
  "response": {
    "header": { "resultCode": "0000", "resultMsg": "OK" },
    "body": {
      "items": {
        "item": [
          {
            "baseYm": "202503",
            "tAtsCd": "ef011abde85cb1e5bf4622fe0f8dd85f",
            "tAtsNm": "가리봉시장",
            "areaCd": "11",
            "signguCd": "11530",
            "rlteTatsCd": "057579775b527ad1eb381c56981342a9",
            "rlteTatsNm": "서울드래곤시티",
            "rlteRegnNm": "서울특별시",
            "rlteSignguNm": "용산구",
            "rlteCtgryLclsNm": "숙박",
            "rlteCtgryMclsNm": "숙박",
            "rlteCtgrySclsNm": "호텔",
            "rlteRank": "1"
          }
        ]
      },
      "numOfRows": 10,
      "pageNo": 1,
      "totalCount": 298
    }
  }
}
```

---

## 3. 지역별 문화 자원 수요

**상태: 확장 · 명세 확인 완료**

지역의 문화 자원에 대한 수요 지수를 제공한다. 현재 M2 핵심 카드에는 넣지 않고, 지역 수요 판단을 확장할 때 사용한다.

```http
GET https://apis.data.go.kr/B551011/AreaTarResDemService/areaCulResDemList
```

| 주요 요청값 | 설명 |
|---|---|
| `baseYm` | 기준 연월, `yyyyMM` |
| `areaCd`, `signguCd` | 지역 코드 |
| `culResDemIxCd` | 선택. 문화 자원 수요 지표 코드 |
| `pageNo`, `numOfRows` | 페이지 정보 |

| 핵심 응답값 | 사용 |
|---|---|
| `culResDemIxCd`, `culResDemIxNm`, `culResDemIxVal` | 문화 자원 수요 지표 코드·명·값 |
| `baseYm`, 지역 코드·명 | 기준 시점과 지역 식별 |

`culResDemIxCd`를 생략하면 해당 지역·연월의 모든 제공 지표를 조회한다. 서버는 코드값을 임의로 해석하지 않고 `culResDemIxNm`을 함께 저장한다. 특정 지표를 정책에 사용할 때만 정책 설정에 코드·명·사용 목적을 함께 등록한다.

### 성공 응답 예시

```json
{
  "response": {
    "header": { "resultCode": "0000", "resultMsg": "OK" },
    "body": {
      "items": {
        "item": [
          {
            "baseYm": "예시 기준 연월",
            "areaCd": "예시 광역 지역 코드",
            "areaNm": "예시 광역 지역명",
            "signguCd": "예시 시군구 코드",
            "signguNm": "예시 시군구명",
            "culResDemIxCd": "예시 culResDemIxCd",
            "culResDemIxNm": "예시 culResDemIxNm",
            "culResDemIxVal": "예시 culResDemIxVal"
          }
        ]
      },
      "numOfRows": 10,
      "pageNo": 1,
      "totalCount": 1
    }
  }
}
```

---

## 4. 지역별 관광 서비스 수요

**상태: 확장 · 명세 확인 완료**

지역의 관광 서비스 수요를 제공한다. SNS 언급, 관광 소비, 내비게이션 목적지 검색 같은 하위 지표를 바탕으로 한다.

```http
GET https://apis.data.go.kr/B551011/AreaTarResDemService/areaTarSvcDemList
```

| 주요 요청값 | 설명 |
|---|---|
| `baseYm` | 기준 연월, `yyyyMM` |
| `areaCd`, `signguCd` | 지역 코드 |
| `tarSvcDemIxCd` | 선택. 관광 서비스 수요 지표 코드 |
| `pageNo`, `numOfRows` | 페이지 정보 |

| 핵심 응답값 | 사용 |
|---|---|
| `tarSvcDemIxCd`, `tarSvcDemIxNm`, `tarSvcDemIxVal` | 관광 서비스 수요 지표 코드·명·값 |
| `baseYm`, 지역 코드·명 | 기준 시점과 지역 식별 |

`tarSvcDemIxCd`를 생략하면 해당 지역·연월의 모든 제공 지표를 조회한다. 서버는 코드값을 임의로 해석하지 않고 `tarSvcDemIxNm`을 함께 저장한다. 특정 지표를 정책에 사용할 때만 정책 설정에 코드·명·사용 목적을 함께 등록한다.

### 성공 응답 예시

```json
{
  "response": {
    "header": { "resultCode": "0000", "resultMsg": "OK" },
    "body": {
      "items": {
        "item": [
          {
            "baseYm": "예시 기준 연월",
            "areaCd": "예시 광역 지역 코드",
            "areaNm": "예시 광역 지역명",
            "signguCd": "예시 시군구 코드",
            "signguNm": "예시 시군구명",
            "tarSvcDemIxCd": "예시 tarSvcDemIxCd",
            "tarSvcDemIxNm": "예시 tarSvcDemIxNm",
            "tarSvcDemIxVal": "예시 tarSvcDemIxVal"
          }
        ]
      },
      "numOfRows": 10,
      "pageNo": 1,
      "totalCount": 1
    }
  }
}
```

---

## 5. 지역별 관광 다양성

**상태: 확장 · 명세 확인 완료**

관광객 다양성, 관광 소비 다양성, 국제적 다양성 지표를 제공한다. 지역 관광 수요의 구성 특성을 설명하는 보조 데이터다.

```http
GET https://apis.data.go.kr/B551011/AreaTarDivService/areaTouDivList
```

| 주요 요청값 | 설명 |
|---|---|
| `baseYm` | 기준 연월, `yyyyMM` |
| `areaCd`, `signguCd` | 지역 코드 |
| `touDivIxCd` | 선택. 관광객 다양성 지표 코드 |
| `pageNo`, `numOfRows` | 페이지 정보 |

| 핵심 응답값 | 사용 |
|---|---|
| `touDivIxCd`, `touDivIxNm`, `touDivIxVal` | 관광객 다양성 지표 코드·명·값 |
| `baseYm`, 지역 코드·명 | 기준 시점과 지역 식별 |

`touDivIxCd`를 생략하면 해당 지역·연월의 모든 제공 지표를 조회한다. 서버는 코드값을 임의로 해석하지 않고 `touDivIxNm`을 함께 저장한다. 특정 지표를 정책에 사용할 때만 정책 설정에 코드·명·사용 목적을 함께 등록한다.

### 성공 응답 예시

```json
{
  "response": {
    "header": { "resultCode": "0000", "resultMsg": "OK" },
    "body": {
      "items": {
        "item": [
          {
            "baseYm": "예시 기준 연월",
            "areaCd": "예시 광역 지역 코드",
            "areaNm": "예시 광역 지역명",
            "signguCd": "예시 시군구 코드",
            "signguNm": "예시 시군구명",
            "touDivIxCd": "예시 touDivIxCd",
            "touDivIxNm": "예시 touDivIxNm",
            "touDivIxVal": "예시 touDivIxVal"
          }
        ]
      },
      "numOfRows": 10,
      "pageNo": 1,
      "totalCount": 1
    }
  }
}
```

`areaExpDivList`, `areaIntlDivList`는 별도 확장 오퍼레이션이다. 현재 제공받은 명세와 구현 범위에는 포함하지 않는다.

---

## 6. 기초 지자체 중심 관광지 정보

**상태: 확장 · 명세 확인 완료**

티맵 내비게이션 데이터를 기반으로, 해당 시·군·구에서 다른 관광지와 연계 방문이 많은 중심 관광지 최대 100위를 제공한다. 실제 방문자 수가 아닌 연결 중심성 데이터다.

```http
GET http://apis.data.go.kr/B551011/LocgoHubTarService1/areaBasedList1
```

| 주요 요청값 | 설명 |
|---|---|
| `baseYm` | 기준 연월, `yyyyMM` |
| `areaCd`, `signguCd` | 지역 코드 |
| `pageNo`, `numOfRows` | 페이지 정보 |

| 활용 | 설명 |
|---|---|
| `hubTatsCd`, `hubTatsNm`, `hubRank` | 중심 관광지 코드·명·순위 |
| `hubCtgryLclsNm`, `hubCtgryMclsNm` | 중심 관광지 카테고리 |
| `mapX`, `mapY` | 중심 관광지 좌표 |

### 성공 응답 예시

```json
{
  "response": {
    "header": { "resultCode": "0000", "resultMsg": "OK" },
    "body": {
      "items": {
        "item": [
          {
            "baseYm": "예시 기준 연월",
            "mapX": "예시 X좌표",
            "mapY": "예시 Y좌표",
            "areaCd": "예시 광역 지역 코드",
            "areaNm": "예시 광역 지역명",
            "signguCd": "예시 시군구 코드",
            "signguNm": "예시 시군구명",
            "hubTatsCd": "예시 hubTatsCd",
            "hubTatsNm": "예시 hubTatsNm",
            "hubCtgryLclsNm": "예시 hubCtgryLclsNm",
            "hubCtgryMclsNm": "예시 hubCtgryMclsNm",
            "hubRank": "예시 hubRank"
          }
        ]
      },
      "numOfRows": 10,
      "pageNo": 1,
      "totalCount": 1
    }
  }
}
```

---

## 7. 빅데이터 지역별 방문자 수

**상태: 핵심 · 실제 호출 및 응답 확인 완료**

이동통신 데이터를 기반으로 지역의 일별 현지인·외지인·외국인 방문자 수를 제공한다. **개별 관광지 방문자 수가 아니라 지역 전체 방문자 수**다.

```http
GET https://apis.data.go.kr/B551011/DataLabService/locgoRegnVisitrDDList
GET https://apis.data.go.kr/B551011/DataLabService/metcoRegnVisitrDDList
```

| 오퍼레이션 | 단위 | 현재 사용 |
|---|---|---|
| `locgoRegnVisitrDDList` | 시·군·구 | 사용 |
| `metcoRegnVisitrDDList` | 광역 시·도 | 필요 시만 사용 |

| 주요 요청값 | 설명 |
|---|---|
| `startYmd`, `endYmd` | 조회 기간, `yyyyMMdd` |
| `pageNo`, `numOfRows` | 페이지 정보 |

| 응답 필드 | 변환 | 사용 |
|---|---|---|
| `signguCode`, `signguNm` | `String` | 시·군·구 식별 |
| `baseYmd` | `yyyyMMdd → LocalDate` | 일별 추이 |
| `touDivCd`, `touDivNm` | `String` | 현지인·외지인·외국인 구분 |
| `touNum` | 숫자 → `BigDecimal` | 방문자 수 |
| `daywkDivCd`, `daywkDivNm` | `String` | 요일 설명 |

같은 날의 방문자 유형별 `touNum`을 합산해 전체 방문자 수를 만든다. 광역·기초 지자체 값은 집계 기준이 달라 합산하지 않는다.

### 성공 응답 예시 - 기초 지자체

```json
{
  "response": {
    "header": { "resultCode": "0000", "resultMsg": "OK" },
    "body": {
      "items": {
        "item": [
          {
            "signguCode": "11110",
            "signguNm": "종로구",
            "daywkDivCd": "4",
            "daywkDivNm": "목요일",
            "touDivCd": "1",
            "touDivNm": "현지인(a)",
            "touNum": "176473.5",
            "baseYmd": "20210513"
          },
          {
            "signguCode": "11110",
            "signguNm": "종로구",
            "touDivCd": "2",
            "touDivNm": "외지인(b)",
            "touNum": "317425.5",
            "baseYmd": "20210513"
          }
        ]
      },
      "numOfRows": 10,
      "pageNo": 1,
      "totalCount": 772
    }
  }
}
```

---

## 8. 국문 관광정보 서비스

**상태: 핵심 · 기본 연동 완료**

기존 축제 검색·선택, 기존 축제 정보 조회, 동기간 경합 축제 후보 조회에 사용한다.

```http
GET https://apis.data.go.kr/B551011/KorService2/searchFestival2
GET https://apis.data.go.kr/B551011/KorService2/detailCommon2
GET https://apis.data.go.kr/B551011/KorService2/detailIntro2
GET https://apis.data.go.kr/B551011/KorService2/detailInfo2
GET https://apis.data.go.kr/B551011/KorService2/detailImage2
```

### 요청 파라미터

#### 축제 검색 - `searchFestival2`

| 파라미터 | 우리 서비스 기준 | 예시 | 설명 |
|---|---:|---|---|
| `eventStartDate` | O | `20260820` | 검색 시작일, `yyyyMMdd` |
| `eventEndDate` | O | `20260823` | 검색 종료일, `yyyyMMdd` |
| `areaCode` | X | `32` | 광역 지역 코드 필터 |
| `sigunguCode` | X | `230` | 시·군·구 코드 필터 |
| `lDongRegnCd` | X | `32` | 법정동 광역 코드 필터. 경합 축제 검색 시 우선 사용 |
| `lDongSignguCd` | X | `230` | 법정동 시·군·구 코드 필터 |
| `pageNo`, `numOfRows` | O | `1`, `100` | 페이지 정보 |

- 지역 필터는 법정동 코드(`lDongRegnCd`, `lDongSignguCd`)가 있으면 우선 사용하고, 없으면 일반 지역 코드(`areaCode`, `sigunguCode`)를 사용한다.
- 기간이 겹치는 축제만 별도 필터링하고, 최종 경합 축제는 기준 좌표에서 50km 이내 조건을 추가 적용한다.

#### 축제 상세 - `detailCommon2` 등

| 파라미터 | 값 | 설명 |
|---|---|---|
| `contentId` | 예: `3310483` | `searchFestival2`에서 받은 축제 콘텐츠 ID |
| `contentTypeId` | `15` | 축제 콘텐츠 유형 |
| `pageNo`, `numOfRows` | `1`, `10` | 반복·이미지 정보 등 목록 조회 시 사용 |

| 핵심 응답 필드 | 변환 | 사용 |
|---|---|---|
| `contentid` | `String` | 기존 축제 식별자 |
| `contenttypeid` | `String` | 축제 유형 확인. 축제는 `15` |
| `title` | `String` | 축제명 |
| `eventstartdate`, `eventenddate` | `yyyyMMdd → LocalDate` | 기간 겹침 판정 |
| `mapx`, `mapy` | `String → BigDecimal` | 축제 좌표·50km 거리 판정 |
| `addr1`, `addr2` | `String` | 주소 |
| `firstimage`, `firstimage2` | `String` | 축제 이미지 |

재개최 축제는 `contentId`로 좌표와 기본 정보를 확보한다. 신규 축제는 사용자가 입력한 좌표를 기준점으로 사용한다. 경합 축제는 기간이 겹치고 기준점 50km 이내인 축제만 남긴다.

### 성공 응답 예시 - `searchFestival2`

```json
{
  "response": {
    "header": { "resultCode": "0000", "resultMsg": "OK" },
    "body": {
      "items": {
        "item": [
          {
            "contentid": "3310483",
            "contenttypeid": "15",
            "title": "가족 예술축제 [특톡]",
            "addr1": "서울특별시 양천구 남부순환로64길 2 (신월동)",
            "addr2": "서울문화예술교육센터 양천",
            "eventstartdate": "20260502",
            "eventenddate": "20260503",
            "mapx": "126.8321457256",
            "mapy": "37.5282200876",
            "firstimage": "https://tong.visitkorea.or.kr/.../4096496_image2_1.jpg"
          }
        ]
      },
      "numOfRows": 1,
      "pageNo": 1,
      "totalCount": 1
    }
  }
}
```

---

## 현재 구현 범위

1. #1 집중률 API의 전체 페이지 조회, 원본 캐시, M2 개최기간 추이·변동성·여유 관광지 정제를 완료했다.
2. #7 `locgoRegnVisitrDDList`의 전체 페이지 조회와 시군구 방문 수요 정제를 완료했다.
3. #2 `areaBasedList1`의 전체 페이지 조회와 기준 관광지별 그룹·순위 정렬을 완료했다.
4. #8 `searchFestival2` 후보 전체 페이지 조회와 기간 겹침·Haversine 50km 필터를 완료했다.
5. #3~#6은 현재 M2 응답에서 사용하지 않으며 정책·화면 확장 시 연결한다.
