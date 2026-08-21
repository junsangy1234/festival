package com.junsang.festival.domain.diagnosis.location;

// 기획서 5.5.2 위치 획득 2단계와 5.5.3 대체 처리에 대응하는 좌표 출처다.
public enum LocationSource {
    // 1단계: 재개최 축제를 API #8에서 자동 로드
    KOR_SERVICE,
    // 2단계: 신규 축제의 지도 pin·주소 지오코딩·관광지 선택 결과
    USER_INPUT,
    // 미입력 대체: API #6 중심 관광지 좌표 평균을 시군구 대표 좌표로 사용
    SIGNGU_CENTER,
    UNAVAILABLE
}
