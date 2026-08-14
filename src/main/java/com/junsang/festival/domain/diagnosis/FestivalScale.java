package com.junsang.festival.domain.diagnosis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum FestivalScale {
    SMALL("소규모"),
    MEDIUM("중규모"),
    LARGE("대규모");

    private final String label;

    FestivalScale(String label) {
        this.label = label;
    }

    @JsonCreator
    public static FestivalScale from(String value) {
        return Arrays.stream(values())
                .filter(scale -> scale.name().equalsIgnoreCase(value) || scale.label.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 축제 규모입니다: " + value));
    }

    @JsonValue
    public String label() {
        return label;
    }
}
