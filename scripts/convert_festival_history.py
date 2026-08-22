"""문체부 지역축제 개최 계획 현황 XLSX(CSV #9)를 서비스가 읽는 CSV로 변환한다.

사용법:
    python scripts/convert_festival_history.py "<xlsx 경로>" [출력 경로]

기본 출력: src/main/resources/data/festival-history.csv
원본은 3줄짜리 병합 헤더라 열 위치를 고정으로 잡는다(2026년 공개용 기준).
"""

import csv
import re
import sys
from pathlib import Path

import openpyxl

SHEET = "조사표"
FIRST_DATA_ROW = 9

# 1-indexed 열 번호. 2026년 공개용 서식 기준.
COL_REGION = 3          # 광역자치단체명
COL_DISTRICT = 4        # 기초자치단체명
COL_NAME = 5            # 축제명
COL_PLACE_DISTRICT = 10  # 개최 장소 시군구
COL_FIRST_YEAR = 21     # 최초 개최연도
COL_BUDGET = 22         # 예산(백만원) 합계
COL_VISITORS = 27       # 방문객수(前년) 전체

HEADER = ["축제명", "시도", "시군구", "방문객수", "예산", "최초개최연도", "회차"]
DEFAULT_OUTPUT = Path("src/main/resources/data/festival-history.csv")


# "01. 서울" -> "서울", "-" -> ""
def clean(value) -> str:
    text = "" if value is None else str(value).strip()
    text = re.sub(r"^\d+\.\s*", "", text)
    return "" if text in {"-", "해당없음", "모름", "없음"} else text


# "10,459명" -> "10459". 숫자로 못 읽으면 빈 값으로 둔다(NO_DATA 처리).
def numeric(value) -> str:
    text = clean(value)
    digits = re.sub(r"[^0-9]", "", text)
    return digits if digits else ""


# 축제명에 들어 있는 "제25회"에서 회차를 뽑는다.
def round_count(name: str) -> str:
    matched = re.search(r"제?\s*(\d+)\s*회", name)
    return matched.group(1) if matched else ""


def convert(source: Path, output: Path) -> int:
    workbook = openpyxl.load_workbook(source, read_only=True, data_only=True)
    sheet = workbook[SHEET]

    output.parent.mkdir(parents=True, exist_ok=True)
    written = 0
    with output.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(HEADER)
        for row in sheet.iter_rows(min_row=FIRST_DATA_ROW, values_only=True):
            name = clean(row[COL_NAME - 1])
            if not name:
                continue
            writer.writerow([
                name,
                clean(row[COL_REGION - 1]),
                clean(row[COL_PLACE_DISTRICT - 1]) or clean(row[COL_DISTRICT - 1]),
                numeric(row[COL_VISITORS - 1]),
                numeric(row[COL_BUDGET - 1]),
                numeric(row[COL_FIRST_YEAR - 1]),
                round_count(name),
            ])
            written += 1
    return written


def main() -> None:
    if len(sys.argv) < 2:
        print(__doc__)
        raise SystemExit(1)
    source = Path(sys.argv[1])
    output = Path(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_OUTPUT
    written = convert(source, output)
    print(f"{written}건을 {output}에 저장했다.")


if __name__ == "__main__":
    main()
