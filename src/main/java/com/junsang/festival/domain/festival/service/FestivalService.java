package com.junsang.festival.domain.festival.service;

import com.junsang.festival.domain.festival.dto.request.FestivalSearchRequest;
import com.junsang.festival.domain.festival.dto.response.CompetingFestivalResponse;
import com.junsang.festival.domain.festival.dto.response.FestivalDetailResponse;
import com.junsang.festival.domain.festival.dto.response.FestivalListResponse;
import com.junsang.festival.domain.festival.dto.response.FestivalSummaryResponse;
import com.junsang.festival.global.exception.TourApiException;
import com.junsang.festival.infra.tourapi.client.TourFestivalClient;
import com.junsang.festival.infra.tourapi.dto.festival.TourApiEnvelope;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalCommonItem;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalDetailInfoItem;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalImageItem;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalIntroItem;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalSearchApiResponse;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalSearchItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

// 축제 검색·상세·경합 처리
@Service
@RequiredArgsConstructor
public class FestivalService {

    private static final String FESTIVAL_CONTENT_TYPE_ID = "15";
    private static final DateTimeFormatter TOUR_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final double COMPETING_RADIUS_KM = 50.0;
    private static final int COMPETING_PAGE_SIZE = 100;

    private final TourFestivalClient tourFestivalClient;
    private final GeoDistanceService geoDistanceService;

    // 축제 목록 검색
    public FestivalListResponse searchFestivals(FestivalSearchRequest request) {
        validatePeriod(request.startDate(), request.endDate());
        TourFestivalSearchApiResponse response = tourFestivalClient.searchFestivals(request.toTourParameters());
        List<TourFestivalSearchItem> items = items(response.response());
        return new FestivalListResponse(
                items.stream().map(FestivalSummaryResponse::from).toList(),
                valueOrDefault(response.response(), envelope -> envelope.body().pageNo(), 1),
                valueOrDefault(response.response(), envelope -> envelope.body().numOfRows(), request.size() == null ? 20 : request.size()),
                valueOrDefault(response.response(), envelope -> envelope.body().totalCount(), 0)
        );
    }

    // 축제 상세 조회
    public FestivalDetailResponse getFestival(String contentId) {
        FestivalContext context = getFestivalContext(contentId);
        List<TourFestivalDetailInfoItem> information = items(
                tourFestivalClient.getContentDetailInfo(contentParameters(contentId)).response()
        );
        List<TourFestivalImageItem> images = items(
                tourFestivalClient.getContentImages(imageParameters(contentId)).response()
        );
        return FestivalDetailResponse.from(context.common(), context.intro(), information, images);
    }

    // 기존 축제 기준 경합 조회
    public CompetingFestivalResponse getCompetingFestivals(String contentId) {
        FestivalContext context = getFestivalContext(contentId);
        LocalDate startDate = parseDate(context.intro().eventStartDate(), "축제 시작일");
        LocalDate endDate = parseDate(context.intro().eventEndDate(), "축제 종료일");
        return findCompetingFestivals(
                contentId,
                startDate,
                endDate,
                parseCoordinate(context.common().latitude(), "기준 축제 위도"),
                parseCoordinate(context.common().longitude(), "기준 축제 경도")
        );
    }

    // 좌표 기준 경합 계산
    public CompetingFestivalResponse findCompetingFestivals(
            String targetContentId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal targetLatitude,
            BigDecimal targetLongitude
    ) {
        validatePeriod(startDate, endDate);
        List<TourFestivalSearchItem> candidates = loadAllCompetingCandidates(startDate, endDate);
        List<FestivalSummaryResponse> competitors = new ArrayList<>();
        int excludedMissingCoordinates = 0;

        for (TourFestivalSearchItem candidate : candidates) {
            if (targetContentId != null && targetContentId.equals(candidate.contentId())) {
                continue;
            }
            if (!overlaps(startDate, endDate, candidate.eventStartDate(), candidate.eventEndDate())) {
                continue;
            }
            BigDecimal candidateLatitude = optionalCoordinate(candidate.latitude());
            BigDecimal candidateLongitude = optionalCoordinate(candidate.longitude());
            if (candidateLatitude == null || candidateLongitude == null) {
                excludedMissingCoordinates++;
                continue;
            }
            if (!geoDistanceService.isWithinKm(
                    targetLatitude, targetLongitude, candidateLatitude, candidateLongitude, COMPETING_RADIUS_KM
            )) {
                continue;
            }
            BigDecimal distanceKm = geoDistanceService.calculateKm(
                    targetLatitude, targetLongitude, candidateLatitude, candidateLongitude
            );
            competitors.add(FestivalSummaryResponse.from(candidate, distanceKm));
        }

        competitors.sort(java.util.Comparator.comparing(FestivalSummaryResponse::distanceKm));
        return new CompetingFestivalResponse(
                targetContentId, List.copyOf(competitors), competitors.size(), excludedMissingCoordinates
        );
    }

    /// Helper Method
    // 축제 공통·소개 정보 조회
    private FestivalContext getFestivalContext(String contentId) {
        TourFestivalCommonItem common = firstItem(
                tourFestivalClient.getContentCommon(commonParameters(contentId)).response(), contentId
        );
        TourFestivalIntroItem intro = firstItem(
                tourFestivalClient.getContentIntro(contentParameters(contentId)).response(), contentId
        );
        return new FestivalContext(common, intro);
    }

    // 축제 소개 요청 파라미터
    private Map<String, String> contentParameters(String contentId) {
        return Map.of("contentId", contentId, "contentTypeId", FESTIVAL_CONTENT_TYPE_ID);
    }

    // 축제 공통 요청 파라미터
    private Map<String, String> commonParameters(String contentId) {
        return Map.of("contentId", contentId);
    }

    // 축제 이미지 요청 파라미터
    private Map<String, String> imageParameters(String contentId) {
        return Map.of(
                "contentId", contentId,
                "imageYN", "Y",
                "subImageYN", "Y",
                "numOfRows", "100"
        );
    }

    // 경합 축제 검색 파라미터
    private Map<String, String> competingFestivalParameters(LocalDate startDate, LocalDate endDate, int pageNo) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("eventStartDate", startDate.format(TOUR_DATE_FORMATTER));
        parameters.put("eventEndDate", endDate.format(TOUR_DATE_FORMATTER));
        parameters.put("pageNo", String.valueOf(pageNo));
        parameters.put("numOfRows", String.valueOf(COMPETING_PAGE_SIZE));
        return parameters;
    }

    // 경합 축제 후보 전체 조회
    private List<TourFestivalSearchItem> loadAllCompetingCandidates(LocalDate startDate, LocalDate endDate) {
        List<TourFestivalSearchItem> candidates = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            TourFestivalSearchApiResponse response = tourFestivalClient.searchFestivals(
                    competingFestivalParameters(startDate, endDate, pageNo)
            );
            List<TourFestivalSearchItem> pageItems = items(response.response());
            candidates.addAll(pageItems);
            int totalCount = valueOrDefault(response.response(), envelope -> envelope.body().totalCount(), 0);
            if (pageItems.isEmpty() || candidates.size() >= totalCount) {
                return List.copyOf(candidates);
            }
            pageNo++;
        }
    }

    // 관광공사 목록 추출
    private <T> List<T> items(TourApiEnvelope<T> response) {
        if (response == null || response.body() == null || response.body().items() == null || response.body().items().item() == null) {
            return List.of();
        }
        return response.body().items().item();
    }

    // 관광공사 첫 항목 추출
    private <T> T firstItem(TourApiEnvelope<T> response, String contentId) {
        return items(response).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "축제를 찾을 수 없습니다: " + contentId
                ));
    }

    // 페이지 메타데이터 기본값
    private int valueOrDefault(
            TourApiEnvelope<?> response,
            java.util.function.Function<TourApiEnvelope<?>, Integer> extractor,
            int defaultValue
    ) {
        if (response == null || response.body() == null) {
            return defaultValue;
        }
        Integer value = extractor.apply(response);
        return value == null ? defaultValue : value;
    }

    // 관광공사 날짜 변환
    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value, TOUR_DATE_FORMATTER);
        } catch (Exception exception) {
            throw new TourApiException("관광공사 API의 " + fieldName + " 형식이 올바르지 않습니다.", exception);
        }
    }

    // 필수 기준 좌표 변환
    private BigDecimal parseCoordinate(String value, String fieldName) {
        BigDecimal coordinate = optionalCoordinate(value);
        if (coordinate == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, fieldName + "가 없어 거리를 계산할 수 없습니다.");
        }
        return coordinate;
    }

    // 선택 좌표 변환
    private BigDecimal optionalCoordinate(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    // 축제 기간 검증
    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    // 축제 기간 겹침 판정
    private boolean overlaps(LocalDate targetStart, LocalDate targetEnd, String candidateStart, String candidateEnd) {
        try {
            LocalDate start = LocalDate.parse(candidateStart, TOUR_DATE_FORMATTER);
            LocalDate end = LocalDate.parse(candidateEnd, TOUR_DATE_FORMATTER);
            return !end.isBefore(targetStart) && !start.isAfter(targetEnd);
        } catch (Exception exception) {
            return false;
        }
    }

    // 문자열 값 확인
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record FestivalContext(TourFestivalCommonItem common, TourFestivalIntroItem intro) {
    }
}
