package com.junsang.festival.domain.diagnosis.data;

import java.time.Instant;

public record ExternalDataResult<T>(
        ExternalDataStatus status,
        String reason,
        String referencePeriod,
        Instant retrievedAt,
        T data
) {
    public static <T> ExternalDataResult<T> available(String referencePeriod, T data) {
        return new ExternalDataResult<>(ExternalDataStatus.AVAILABLE, null, referencePeriod, Instant.now(), data);
    }

    public static <T> ExternalDataResult<T> noData(String reason, String referencePeriod) {
        return new ExternalDataResult<>(ExternalDataStatus.NO_DATA, reason, referencePeriod, Instant.now(), null);
    }

    public static <T> ExternalDataResult<T> outOfRange(String reason, String referencePeriod) {
        return new ExternalDataResult<>(ExternalDataStatus.OUT_OF_FORECAST_RANGE, reason, referencePeriod, Instant.now(), null);
    }

    public static <T> ExternalDataResult<T> failed(String reason, String referencePeriod) {
        return new ExternalDataResult<>(ExternalDataStatus.FAILED, reason, referencePeriod, Instant.now(), null);
    }
}
