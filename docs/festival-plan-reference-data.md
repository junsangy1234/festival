# 전국 축제 개최 계획 참고 데이터 명세

이 문서는 외부 API가 아닌 파일형 참고 데이터를 정리한다. 서버가 실시간으로 호출하지 않으므로 [외부 데이터 API 명세](external-api-reference.md)와 분리한다.

## 역할

- TourAPI 축제 검색 결과가 부족할 때 축제 후보를 보조 확인한다.
- 같은 기간의 개최 예정 축제를 보조적으로 확인한다.
- M2 경합 축제의 최종 기준은 KorService2의 기간·좌표 데이터이며, 이 파일은 그 결과를 대체하지 않는다.

## 원본 데이터

- 자료명: 2026년 전국 축제 개최 계획 자료
- 포함 정보: 축제명, 축제 유형, 개최 장소, 개최 기간, 예산
- 보관 방식: 원본 파일은 Git에 올리지 않고, 팀 공유 저장소의 원본 자료 폴더에서 관리한다.
- 갱신 방식: 새 파일을 받으면 기존 파일을 덮어쓰지 말고 `sourceYear`와 `importedAt`을 기록한 새 버전으로 등록한다.

## 서버 저장 형태

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `sourceYear` | `int` | O | 계획 기준 연도. 예: `2026` |
| `festivalName` | `String` | O | 축제명 |
| `festivalType` | `String` | X | 원본 축제 유형 |
| `venue` | `String` | X | 개최 장소 |
| `startDate` | `LocalDate` | X | 개최 시작일. 파싱 불가하면 원본값 보관 |
| `endDate` | `LocalDate` | X | 개최 종료일. 파싱 불가하면 원본값 보관 |
| `budget` | `BigDecimal` | X | 예산. 단위와 원본 표기를 함께 보관 |
| `rawPeriod` | `String` | X | 원본 개최 기간 문자열 |
| `sourceFileName` | `String` | O | 가져온 원본 파일명 |
| `importedAt` | `LocalDateTime` | O | 서버 등록 시각 |

## 가져오기 규칙

1. 파일 업로드 또는 관리자 등록 과정에서만 데이터를 갱신한다.
2. 축제명만으로 TourAPI의 `contentId`를 확정하지 않는다. 이름·개최 기간·장소가 함께 일치할 때만 후보로 제시한다.
3. 좌표가 없으면 M2의 50km 경합 축제 계산에는 사용하지 않는다.
4. 실제 개최 여부·일정 변경 가능성이 있으므로 화면에는 `계획 자료 기준`임을 표시한다.

## 사용 예시

```json
{
  "sourceYear": 2026,
  "festivalName": "예시 지역 축제",
  "festivalType": "문화예술",
  "venue": "예시시 문화광장",
  "startDate": "2026-08-20",
  "endDate": "2026-08-23",
  "budget": 50000000,
  "rawPeriod": "2026. 8. 20. ~ 8. 23.",
  "sourceFileName": "2026_전국축제_개최계획.xlsx"
}
```
