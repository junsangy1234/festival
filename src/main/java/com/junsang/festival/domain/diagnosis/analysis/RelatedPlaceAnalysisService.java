package com.junsang.festival.domain.diagnosis.analysis;

import com.junsang.festival.infra.tourapi.dto.related.TourRelatedPlaceItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 연관 관광지 분석
@Service
public class RelatedPlaceAnalysisService {

    // 기준 관광지별 관계 정리
    public RelatedPlaceAnalysisResult analyze(List<TourRelatedPlaceItem> items) {
        Map<String, List<TourRelatedPlaceItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(
                        TourRelatedPlaceItem::basePlaceCode,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        List<RelatedPlaceAnalysisResult.BasePlaceRelations> basePlaces = grouped.values().stream()
                .map(group -> new RelatedPlaceAnalysisResult.BasePlaceRelations(
                        group.getFirst().basePlaceCode(),
                        group.getFirst().basePlaceName(),
                        group.stream()
                                .sorted(java.util.Comparator.comparingInt(TourRelatedPlaceItem::rank))
                                .map(this::toRelatedPlace)
                                .toList()
                ))
                .toList();
        List<Integer> topTenRanks = items.stream()
                .filter(item -> item.rank() <= 10)
                .map(TourRelatedPlaceItem::rank)
                .toList();
        BigDecimal averageRank = topTenRanks.isEmpty() ? null : BigDecimal.valueOf(
                        topTenRanks.stream().mapToInt(Integer::intValue).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);
        int categoryDiversity = (int) items.stream()
                .map(item -> item.largeCategory() + "/" + item.mediumCategory() + "/" + item.smallCategory())
                .filter(category -> !"//".equals(category))
                .distinct()
                .count();
        return new RelatedPlaceAnalysisResult(basePlaces, averageRank, categoryDiversity);
    }

    private RelatedPlaceAnalysisResult.RelatedPlace toRelatedPlace(TourRelatedPlaceItem item) {
        return new RelatedPlaceAnalysisResult.RelatedPlace(
                item.relatedPlaceCode(),
                item.relatedPlaceName(),
                item.relatedAreaName(),
                item.relatedSignguName(),
                item.largeCategory(),
                item.mediumCategory(),
                item.smallCategory(),
                item.rank()
        );
    }
}
