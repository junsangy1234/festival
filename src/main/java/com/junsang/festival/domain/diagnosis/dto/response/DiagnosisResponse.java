package com.junsang.festival.domain.diagnosis.dto.response;

import com.junsang.festival.domain.diagnosis.DiagnosisStatus;
import com.junsang.festival.domain.diagnosis.FestivalScale;
import com.junsang.festival.domain.diagnosis.FestivalType;
import com.junsang.festival.domain.diagnosis.RecurrenceType;
import com.junsang.festival.domain.diagnosis.entity.Diagnosis;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

public record DiagnosisResponse(
        String reportId,
        DiagnosisStatus status,
        String festivalName,
        String areaCode,
        String signguCode,
        LocalDate startDate,
        LocalDate endDate,
        FestivalType festivalType,
        FestivalScale scale,
        RecurrenceType recurrenceType,
        String existingFestivalContentId,
        String festivalAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant createdAt
) {
    public static DiagnosisResponse from(Diagnosis diagnosis) {
        return new DiagnosisResponse(
                diagnosis.getId(),
                diagnosis.getStatus(),
                diagnosis.getFestivalName(),
                diagnosis.getAreaCode(),
                diagnosis.getSignguCode(),
                diagnosis.getStartDate(),
                diagnosis.getEndDate(),
                diagnosis.getFestivalType(),
                diagnosis.getScale(),
                diagnosis.getRecurrenceType(),
                diagnosis.getExistingFestivalContentId(),
                diagnosis.getFestivalAddress(),
                diagnosis.getLatitude(),
                diagnosis.getLongitude(),
                diagnosis.getCreatedAt()
        );
    }
}
