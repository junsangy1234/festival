package com.junsang.festival.domain.diagnosis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum RecurrenceType {
    NEW("신규"),
    RECURRING("재개최");

    private final String label;

    RecurrenceType(String label) {
        this.label = label;
    }

    @JsonCreator
    public static RecurrenceType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value) || type.label.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 개최 유형입니다: " + value));
    }

    @JsonValue
    public String label() {
        return label;
    }
}
