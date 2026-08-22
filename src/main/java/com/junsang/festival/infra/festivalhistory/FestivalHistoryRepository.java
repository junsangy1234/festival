package com.junsang.festival.infra.festivalhistory;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// CSV #9(문체부 지역축제 개최 계획 현황)을 메모리로 읽어 축제명으로 실적을 찾는다.
// 파일이 없으면 조회 결과가 비어 있을 뿐 진단은 그대로 진행한다(데이터 없음 처리).
@Slf4j
@Component
public class FestivalHistoryRepository {

    // 부분 매칭에 쓸 최소 글자 수. 너무 짧은 이름으로 엉뚱한 축제가 붙는 것을 막는다.
    private static final int MINIMUM_PARTIAL_MATCH_LENGTH = 4;

    private final ResourceLoader resourceLoader;
    private final String location;
    private final Map<String, FestivalHistoryRecord> recordsByName = new HashMap<>();

    public FestivalHistoryRepository(
            ResourceLoader resourceLoader,
            @Value("${festival.external-data.festival-history-csv:classpath:data/festival-history.csv}") String location
    ) {
        this.resourceLoader = resourceLoader;
        this.location = location;
    }

    @PostConstruct
    void load() {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            log.info("문체부 지역축제 CSV(#9)가 없어 재개최 실적 조인을 건너뜁니다: {}", location);
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return;
            }
            List<String> headers = splitCsvLine(stripBom(headerLine));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                toRecord(headers, splitCsvLine(line))
                        .ifPresent(record -> recordsByName.put(normalize(record.festivalName()), record));
            }
            log.info("문체부 지역축제 CSV(#9) {}건을 읽었습니다.", recordsByName.size());
        } catch (Exception exception) {
            log.warn("문체부 지역축제 CSV(#9)를 읽지 못했습니다: {}", exception.getMessage());
        }
    }

    // 축제명으로 재개최 실적을 찾는다. 연도·회차 접두사와 공백·괄호는 무시한다.
    public Optional<FestivalHistoryRecord> findByFestivalName(String festivalName) {
        if (festivalName == null || festivalName.isBlank()) {
            return Optional.empty();
        }
        String key = normalize(festivalName);
        FestivalHistoryRecord exact = recordsByName.get(key);
        return exact != null ? Optional.of(exact) : findUniquePartialMatch(key);
    }

    // 문체부 정식 명칭에 수식어가 붙는 경우가 있다("얼음나라 화천 산천어축제" vs API #8 "화천산천어축제").
    // 후보가 정확히 하나일 때만 인정해 다른 축제의 실적이 잘못 붙는 것을 막는다.
    private Optional<FestivalHistoryRecord> findUniquePartialMatch(String key) {
        if (key.length() < MINIMUM_PARTIAL_MATCH_LENGTH) {
            return Optional.empty();
        }
        List<FestivalHistoryRecord> matches = recordsByName.entrySet().stream()
                .filter(entry -> entry.getKey().length() >= MINIMUM_PARTIAL_MATCH_LENGTH)
                .filter(entry -> entry.getKey().contains(key) || key.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .limit(2)
                .toList();
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    // 로딩된 실적 건수. 데이터 상태 표시에 쓴다.
    public int size() {
        return recordsByName.size();
    }

    private Optional<FestivalHistoryRecord> toRecord(List<String> headers, List<String> values) {
        String name = column(headers, values, "축제명");
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new FestivalHistoryRecord(
                name,
                column(headers, values, "시도"),
                column(headers, values, "시군구"),
                decimal(column(headers, values, "방문객수")),
                decimal(column(headers, values, "예산")),
                integer(column(headers, values, "최초개최")),
                integer(column(headers, values, "회차"))
        ));
    }

    // 문체부 원본의 열 이름이 회차마다 조금씩 달라 부분 일치로 찾는다.
    private String column(List<String> headers, List<String> values, String keyword) {
        for (int index = 0; index < headers.size() && index < values.size(); index++) {
            if (headers.get(index).replace(" ", "").contains(keyword)) {
                return values.get(index).trim();
            }
        }
        return null;
    }

    private List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (char character : line.toCharArray()) {
            if (character == '"') {
                quoted = !quoted;
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        values.add(current.toString());
        return values;
    }

    // 문체부 축제명은 "2026 ...", "제18회 ...", "2026년 30회 ..."처럼 해마다 앞머리가 달라진다.
    // 연도·회차 표기를 걷어낸 뒤 공백·괄호·구분기호를 지우고 비교한다.
    private String normalize(String value) {
        String normalized = value;
        for (int round = 0; round < 2; round++) {
            normalized = normalized.replaceAll("^\\s*20\\d{2}\\s*년?\\s*", "");
            normalized = normalized.replaceAll("^\\s*제?\\s*\\d+\\s*회\\s*", "");
        }
        return normalized.replaceAll("[\\s()（）\\-_·,]", "").toLowerCase();
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer integer(String value) {
        BigDecimal decimal = decimal(value);
        return decimal == null ? null : decimal.intValue();
    }

    private String stripBom(String value) {
        return value.startsWith("﻿") ? value.substring(1) : value;
    }
}
