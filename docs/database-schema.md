# Festival 데이터베이스 설계 문서

> **Last updated:** 2026-08-27
> **Migration source of truth:** `src/main/resources/db/migration/V*.sql`  
> **Database:** PostgreSQL (운영) / H2 PostgreSQL mode (로컬·테스트)  
> **Document rule:** DB 테이블·컬럼·제약조건·시드 데이터가 추가·수정·삭제될 때마다, 같은 변경에서 Flyway 마이그레이션과 이 문서를 함께 갱신한다.

---

## 1. 문서 개요 및 표기

이 문서는 축제날씨 서비스의 현재 데이터베이스 스키마를 정리한다. 실제 스키마 변경의 기준은 Flyway 마이그레이션이며, 이 문서는 구조와 의도를 빠르게 파악하기 위한 최신 설명서다.

| 태그 | 의미 |
| --- | --- |
| 🔑 PK | Primary Key |
| 🔗 FK | Foreign Key |
| 🟣 UQ | Unique 제약 |
| 📍 IDX | Index |
| ✅ CHECK | 값 범위 제약 |

### DB 변경 작업 규칙

1. 새 엔티티 또는 저장할 데이터가 생기면 먼저 Flyway migration을 작성한다.
2. 같은 변경에서 엔티티와 migration, 이 문서의 테이블·관계·인덱스 설명을 함께 갱신한다.
3. 운영에 적용된 migration 파일은 수정하지 않고, 다음 버전 migration을 추가한다.
4. 외부 관광공사 API를 단순 중계하는 데이터는 DB에 저장하지 않는다. 캐시 또는 서비스 핵심 데이터로 관리할 필요가 생겼을 때만 테이블을 추가한다.

---

## 2. 현재 테이블

### `diagnoses`

사용자가 생성한 축제 진단 요청의 입력값과 처리 상태를 저장한다. 이 테이블의 `id`를 프론트와 외부에 `reportId`로 노출해 M2 대시보드와 M3 리포트 URL을 연결한다. 정제 결과와 정책 매칭 결과는 현재 프로세스 메모리에서 reportId로 보관하며 DB 컬럼은 추가하지 않았다.

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | VARCHAR(36) | 🔑 PK | UUID 기반 진단 ID |
| `festival_name` | VARCHAR(100) | NOT NULL | 화면·리포트 표시 축제명 |
| `area_code` | VARCHAR(2) | NOT NULL | 집중률 API에 전달하는 광역시도 코드 |
| `signgu_code` | VARCHAR(5) | NOT NULL | 집중률 API에 전달하는 시군구 코드 |
| `start_date`, `end_date` | DATE | NOT NULL | 축제 개최 기간 |
| `festival_type` | VARCHAR(30) | NOT NULL | Java enum 이름 기준 축제 유형 |
| `scale` | VARCHAR(20) | NOT NULL | Java enum 이름 기준 축제 규모 |
| `recurrence_type` | VARCHAR(20) | NOT NULL | 신규·재개최 유형 |
| `existing_festival_content_id` | VARCHAR(50) | NULL | 재개최 축제의 관광공사 `contentId` |
| `festival_address` | VARCHAR(255) | NULL | 신규 축제장 주소 |
| `latitude` | DECIMAL(10,7) | NULL | 신규 축제장 위도 |
| `longitude` | DECIMAL(10,7) | NULL | 신규 축제장 경도 |
| `status` | VARCHAR(20) | NOT NULL | `PENDING`, `RUNNING`, `COMPLETED`, `PARTIAL`, `FAILED` |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | 진단 요청 생성 시각 |

적용된 migration은 `V1__create_diagnoses.sql`부터 `V6__create_festival_histories.sql`까지다. `V4`는 축제 진단의 지역 입력을 관광지 집중률 API에 맞는 `areaCode`와 `signguCode`로 분리하고, `V5`는 신규 축제장 주소·좌표를 추가하며, `V6`는 과거 축제 실적을 저장하는 테이블을 추가한다.

### `festival_histories`

문체부 CSV #9의 과거 축제 개최 실적을 저장한다. 애플리케이션 시작 시 `FestivalHistoryImporter`가 설정된 CSV를 읽어 테이블을 갱신하고, 진단 시 축제명으로 작년 방문객 수·예산·최초 개최연도·회차를 조회한다.

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | VARCHAR(36) | 🔑 PK | UUID 기반 이력 ID |
| `festival_name` | VARCHAR(255) | NOT NULL, 📍 IDX | CSV 축제명 |
| `region_name` | VARCHAR(100) | NULL | 시도명 |
| `signgu_name` | VARCHAR(100) | NULL | 시군구명 |
| `last_year_visitors` | DECIMAL(15,2) | NULL | 전년 방문객 수 |
| `budget_million_won` | DECIMAL(15,2) | NULL | 예산(백만원) |
| `first_held_year` | INTEGER | NULL | 최초 개최연도 |
| `round_count` | INTEGER | NULL | 개최 회차 |

CSV가 없거나 비어 있으면 기존 DB 데이터를 삭제하지 않고 적재를 건너뛴다. CSV가 있으면 배포 기준 데이터로 보고 기존 이력 전체를 교체한다.

### 관계

```text
diagnoses
  └─ 현재 정제 결과는 프로세스 메모리 DiagnosisResultStore에서 reportId로 연결
```

---

## 3. 예정 도메인

| 도메인 | 저장 목적 | 현재 상태 |
| --- | --- | --- |
| `Diagnosis` | 사용자가 요청한 축제 진단의 기본 입력값과 상태 보관 | 구현 완료 |
| `DiagnosisReport` | 진단 결과, 점수, 운영 권고안 및 생성 시점 보관 | 미구현 |
| `FestivalHistory` | 문체부 CSV #9 과거 축제 실적 보관 | 구현 완료 |
| 파일 원본 캐시 | 반복 호출되는 관광공사 원본 JSON의 TTL 캐시 | `FileJsonCache` 구현 완료, DB 테이블 없음 |

정제 결과를 서버 재시작 후에도 보존해야 한다면 새 Flyway migration으로 결과 테이블을 추가한다. 현재는 외부 원본 파일 캐시로 재조회 비용을 줄이고, 서버 재시작 뒤 M2 조회 시 정제를 다시 수행한다.

M2 화면용 목록 개수 제한은 DB 설정이 아니라 `application.yaml`의 `festival.dashboard.*` 설정으로 관리한다.
