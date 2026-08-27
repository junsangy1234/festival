package com.junsang.festival.domain.festivalhistory.repository;

import com.junsang.festival.domain.festivalhistory.dto.FestivalHistoryRecord;
import com.junsang.festival.domain.festivalhistory.entity.FestivalHistory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalHistoryImporter {

    private final FestivalHistoryRepository csvRepository;
    private final FestivalHistoryJpaRepository jpaRepository;

    @PostConstruct
    void importCsv() {
        csvRepository.load();
        List<FestivalHistoryRecord> records = csvRepository.records();
        if (records.isEmpty()) {
            log.info("문체부 축제 이력 CSV가 없어 DB 적재를 건너뜁니다.");
            return;
        }

        jpaRepository.deleteAllInBatch();
        jpaRepository.saveAll(records.stream().map(FestivalHistory::from).toList());
        log.info("문체부 축제 이력 {}건을 DB에 적재했습니다.", records.size());
    }
}
