package com.junsang.festival.domain.festivalhistory.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FestivalHistoryRepositoryTest {

    private FestivalHistoryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FestivalHistoryRepository(
                new DefaultResourceLoader(), "classpath:data/festival-history-sample.csv"
        );
        repository.load();
    }

    @Test
    void 회차_접두사가_붙은_축제명도_실적을_찾는다() {
        var record = repository.findByFestivalName("제17회 유성국화축제");

        assertThat(record).isPresent();
        assertThat(record.get().lastYearVisitors()).isEqualByComparingTo(BigDecimal.valueOf(704156));
        assertThat(record.get().budgetMillionWon()).isEqualByComparingTo(BigDecimal.valueOf(614));
        assertThat(record.get().firstHeldYear()).isEqualTo(2009);
    }

    @Test
    void 연도_접두사가_붙은_축제명도_실적을_찾는다() {
        // 문체부 CSV는 "2026 화천산천어축제", API #8은 "화천산천어축제"처럼 앞머리가 다르다.
        assertThat(repository.findByFestivalName("2026 화천산천어축제")).isPresent();
        assertThat(repository.findByFestivalName("2026년 22회 화천산천어축제")).isPresent();
    }

    @Test
    void 수식어가_붙은_정식명칭도_후보가_하나면_찾는다() {
        // CSV "2026얼음나라 화천 산천어축제" vs API #8 "화천산천어축제"
        var record = repository.findByFestivalName("화천산천어축제");

        assertThat(record).isPresent();
        assertThat(record.get().lastYearVisitors()).isEqualByComparingTo(java.math.BigDecimal.valueOf(1870000));
    }

    @Test
    void 없는_축제명은_비어_있다() {
        assertThat(repository.findByFestivalName("존재하지 않는 축제")).isEmpty();
    }

    @Test
    void 파일이_없으면_조회만_비어_있고_예외는_없다() {
        FestivalHistoryRepository missing = new FestivalHistoryRepository(
                new DefaultResourceLoader(), "classpath:data/no-such-file.csv"
        );
        missing.load();

        assertThat(missing.size()).isZero();
    }
}
