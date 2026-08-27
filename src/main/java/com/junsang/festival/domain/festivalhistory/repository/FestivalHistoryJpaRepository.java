package com.junsang.festival.domain.festivalhistory.repository;

import com.junsang.festival.domain.festivalhistory.entity.FestivalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalHistoryJpaRepository extends JpaRepository<FestivalHistory, String> {
}
