package com.junsang.festival.domain.diagnosis.dto.response;

import java.util.List;

// M2 상단에서 독립적으로 표시하는 수평 막대 4축이다. 합산·종합 점수를 만들지 않는다.
public record DataProfileResponse(
        DataProfileMetricResponse timingFit,
        DataProfileMetricResponse relaxedPlaces,
        DataProfileMetricResponse connectivity,
        DataProfileMetricResponse categoryDiversity,
        List<String> notes
) {
    // 기획서 Part 3 · 막대 하단에 반드시 노출해야 하는 각주 3줄
    public static final List<String> REQUIRED_NOTES = List.of(
            "각 축은 독립된 데이터 사실입니다. 합산·종합하지 않습니다.",
            "축마다 자연 척도가 달라 바 길이 간 비교는 할 수 없습니다.",
            "연계 풍부도의 순위는 낮을수록 상위 연관 관광지입니다."
    );

    public static DataProfileResponse empty() {
        return new DataProfileResponse(null, null, null, null, REQUIRED_NOTES);
    }
}
