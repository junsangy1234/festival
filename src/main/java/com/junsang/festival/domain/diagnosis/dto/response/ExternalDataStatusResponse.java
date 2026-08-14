package com.junsang.festival.domain.diagnosis.dto.response;

import com.junsang.festival.domain.diagnosis.data.ExternalDataStatus;

import java.time.Instant;

public record ExternalDataStatusResponse(
        String source,
        ExternalDataStatus status,
        String reason,
        String referencePeriod,
        Instant retrievedAt
) {
}
