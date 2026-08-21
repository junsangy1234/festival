package com.junsang.festival.domain.diagnosis.location;

import com.junsang.festival.domain.diagnosis.RecurrenceType;
import com.junsang.festival.domain.diagnosis.entity.Diagnosis;
import com.junsang.festival.domain.festival.dto.response.FestivalDetailResponse;
import com.junsang.festival.domain.festival.service.FestivalService;
import com.junsang.festival.infra.tourapi.dto.hub.TourHubAttractionItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// 기획서 5.5.2 — 축제장 좌표를 2단계(재개최 자동 로드 → 신규 선택 입력)로 확보하고,
// 둘 다 없으면 5.5.3에 따라 시군구 대표 좌표로 근사한다.
@Service
@RequiredArgsConstructor
public class FestivalLocationService {

    private final FestivalService festivalService;

    // 축제장 위치 확보
    public FestivalLocation resolve(Diagnosis diagnosis, List<TourHubAttractionItem> hubAttractions) {
        if (diagnosis.getRecurrenceType() == RecurrenceType.RECURRING) {
            FestivalLocation loaded = fromExistingFestival(diagnosis);
            if (loaded != null) {
                return loaded;
            }
        }
        if (diagnosis.getLatitude() != null && diagnosis.getLongitude() != null) {
            return new FestivalLocation(
                    diagnosis.getLatitude(),
                    diagnosis.getLongitude(),
                    LocationSource.USER_INPUT,
                    true,
                    diagnosis.getFestivalAddress(),
                    null
            );
        }
        return signguCenter(hubAttractions);
    }

    // 1단계 · 재개최 축제는 API #8의 mapx/mapy·addr1을 자동 로드한다.
    private FestivalLocation fromExistingFestival(Diagnosis diagnosis) {
        if (diagnosis.getExistingFestivalContentId() == null) {
            return null;
        }
        try {
            FestivalDetailResponse festival = festivalService.getFestival(diagnosis.getExistingFestivalContentId());
            if (isBlank(festival.latitude()) || isBlank(festival.longitude())) {
                return null;
            }
            return new FestivalLocation(
                    new BigDecimal(festival.latitude()),
                    new BigDecimal(festival.longitude()),
                    LocationSource.KOR_SERVICE,
                    true,
                    festival.address(),
                    null
            );
        } catch (RuntimeException exception) {
            return null;
        }
    }

    // 5.5.3 · 미입력 시 API #6 중심 관광지 좌표 평균을 시군구 대표 좌표로 사용한다.
    private FestivalLocation signguCenter(List<TourHubAttractionItem> hubAttractions) {
        List<TourHubAttractionItem> located = hubAttractions.stream()
                .filter(TourHubAttractionItem::hasCoordinates)
                .toList();
        if (located.isEmpty()) {
            return FestivalLocation.unavailable();
        }
        return new FestivalLocation(
                average(located.stream().map(TourHubAttractionItem::latitude).toList()),
                average(located.stream().map(TourHubAttractionItem::longitude).toList()),
                LocationSource.SIGNGU_CENTER,
                false,
                null,
                "축제장 위치가 입력되지 않아 시군구 중심 좌표를 사용했습니다. "
                        + "R-VOL-005·O-INF-003 판정은 건너뛰며 경합 거리는 근사치입니다."
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BigDecimal average(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 7, RoundingMode.HALF_UP);
    }
}
