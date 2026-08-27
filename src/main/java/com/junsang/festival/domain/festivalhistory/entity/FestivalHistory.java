package com.junsang.festival.domain.festivalhistory.entity;

import com.junsang.festival.domain.festivalhistory.dto.FestivalHistoryRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

// 과거 축제 개최 실적 저장
@Entity
@Table(name = "festival_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalHistory {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 255)
    private String festivalName;

    @Column(length = 100)
    private String regionName;

    @Column(length = 100)
    private String signguName;

    @Column(precision = 15, scale = 2)
    private BigDecimal lastYearVisitors;

    @Column(precision = 15, scale = 2)
    private BigDecimal budgetMillionWon;

    private Integer firstHeldYear;
    private Integer roundCount;

    public static FestivalHistory from(FestivalHistoryRecord record) {
        FestivalHistory history = new FestivalHistory();
        history.id = UUID.randomUUID().toString();
        history.festivalName = record.festivalName();
        history.regionName = record.regionName();
        history.signguName = record.signguName();
        history.lastYearVisitors = record.lastYearVisitors();
        history.budgetMillionWon = record.budgetMillionWon();
        history.firstHeldYear = record.firstHeldYear();
        history.roundCount = record.roundCount();
        return history;
    }

    public FestivalHistoryRecord toRecord() {
        return new FestivalHistoryRecord(
                festivalName, regionName, signguName, lastYearVisitors,
                budgetMillionWon, firstHeldYear, roundCount
        );
    }
}
