package com.junsang.festival.domain.diagnosis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum FestivalType {
    CULTURE_ART("문화예술"),
    TRADITION_HISTORY("전통역사"),
    ECO_NATURE("생태자연"),
    SPECIALTY("특산물"),
    OTHER("기타");

    private final String label;

    FestivalType(String label) {
        this.label = label;
    }

    @JsonCreator
    public static FestivalType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value) || type.label.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 축제 유형입니다: " + value));
    }

    @JsonValue
    public String label() {
        return label;
    }
}
