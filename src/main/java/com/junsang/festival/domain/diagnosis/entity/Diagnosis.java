package com.junsang.festival.domain.diagnosis.entity;

import com.junsang.festival.domain.diagnosis.DiagnosisStatus;
import com.junsang.festival.domain.diagnosis.FestivalScale;
import com.junsang.festival.domain.diagnosis.FestivalType;
import com.junsang.festival.domain.diagnosis.RecurrenceType;
import com.junsang.festival.domain.diagnosis.dto.request.DiagnosisRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

// 축제 진단 입력·상태 저장
@Entity
@Table(name = "diagnoses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diagnosis {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String festivalName;

    @Column(nullable = false, length = 2)
    private String areaCode;

    @Column(nullable = false, length = 5)
    private String signguCode;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FestivalType festivalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FestivalScale scale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceType recurrenceType;

    @Column(length = 50)
    private String existingFestivalContentId;

    @Column(length = 255)
    private String festivalAddress;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiagnosisStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    // 진단 엔티티 생성
    public static Diagnosis create(DiagnosisRequest request) {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.id = UUID.randomUUID().toString();
        diagnosis.festivalName = request.festivalName();
        diagnosis.areaCode = request.areaCode();
        diagnosis.signguCode = request.signguCode();
        diagnosis.startDate = request.startDate();
        diagnosis.endDate = request.endDate();
        diagnosis.festivalType = request.festivalType();
        diagnosis.scale = request.scale();
        diagnosis.recurrenceType = request.recurrenceType();
        diagnosis.existingFestivalContentId = request.existingFestivalContentId();
        diagnosis.festivalAddress = request.festivalAddress();
        diagnosis.latitude = request.latitude();
        diagnosis.longitude = request.longitude();
        diagnosis.status = DiagnosisStatus.PENDING;
        diagnosis.createdAt = Instant.now();
        return diagnosis;
    }

    // 처리 시작
    public void markRunning() {
        status = DiagnosisStatus.RUNNING;
    }

    // 처리 완료
    public void markCompleted() {
        status = DiagnosisStatus.COMPLETED;
    }

    // 부분 완료
    public void markPartial() {
        status = DiagnosisStatus.PARTIAL;
    }

    // 처리 실패
    public void markFailed() {
        status = DiagnosisStatus.FAILED;
    }
}
