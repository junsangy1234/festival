package com.junsang.festival.domain.diagnosis.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.junsang.festival.domain.diagnosis.FestivalScale;
import com.junsang.festival.domain.diagnosis.FestivalType;
import com.junsang.festival.domain.diagnosis.RecurrenceType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;

public record DiagnosisRequest(
        @NotBlank @Size(max = 100) String festivalName,
        @NotBlank @Size(max = 2) String areaCode,
        @NotBlank @Size(max = 5) String signguCode,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        @NotNull FestivalType festivalType,
        @NotNull FestivalScale scale,
        @NotNull RecurrenceType recurrenceType,
        String existingFestivalContentId,
        @Size(max = 255) String festivalAddress,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude
) {
    @AssertTrue(message = "endDate는 startDate보다 빠를 수 없습니다.")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "축제 기간은 최대 14일입니다.")
    public boolean isFestivalDurationValid() {
        return startDate == null || endDate == null || endDate.isBefore(startDate)
                || ChronoUnit.DAYS.between(startDate, endDate) <= 13;
    }

    @AssertTrue(message = "재개최 축제는 existingFestivalContentId가 필요하고, 신규 축제는 입력할 수 없습니다.")
    public boolean isExistingFestivalValid() {
        if (recurrenceType == null) {
            return true;
        }
        boolean hasContentId = existingFestivalContentId != null && !existingFestivalContentId.isBlank();
        return recurrenceType == RecurrenceType.RECURRING ? hasContentId : !hasContentId;
    }

    @AssertTrue(message = "신규 축제는 festivalAddress, latitude, longitude가 필요합니다.")
    public boolean isNewFestivalLocationValid() {
        if (recurrenceType != RecurrenceType.NEW) {
            return true;
        }
        return festivalAddress != null && !festivalAddress.isBlank()
                && latitude != null
                && longitude != null;
    }

}
