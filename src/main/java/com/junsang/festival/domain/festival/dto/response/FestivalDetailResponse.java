package com.junsang.festival.domain.festival.dto.response;

import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalCommonItem;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalDetailInfoItem;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalImageItem;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalIntroItem;

import java.util.List;

public record FestivalDetailResponse(
        String contentId,
        String title,
        String areaCode,
        String sigunguCode,
        String legalDongRegionCode,
        String legalDongSignguCode,
        String address,
        String addressDetail,
        String longitude,
        String latitude,
        String firstImage,
        String thumbnailImage,
        String telephone,
        String homepage,
        String overview,
        String eventStartDate,
        String eventEndDate,
        String eventPlace,
        String playtime,
        String program,
        String organizer,
        String organizerTelephone,
        String host,
        String hostTelephone,
        String eventHomepage,
        String usageTime,
        String festivalDuration,
        List<FestivalInformationResponse> information,
        List<FestivalImageResponse> images
) {
    // 관광공사 공통·소개·안내·이미지 DTO를 하나의 축제 상세 응답으로 변환한다.
    public static FestivalDetailResponse from(
            TourFestivalCommonItem common,
            TourFestivalIntroItem intro,
            List<TourFestivalDetailInfoItem> information,
            List<TourFestivalImageItem> images
    ) {
        return new FestivalDetailResponse(
                common.contentId(), common.title(), common.areaCode(), common.sigunguCode(),
                common.legalDongRegionCode(), common.legalDongSignguCode(),
                common.address(), common.addressDetail(), common.longitude(), common.latitude(),
                common.firstImage(), common.thumbnailImage(), common.tel(), common.homepage(), common.overview(),
                intro.eventStartDate(), intro.eventEndDate(), intro.eventPlace(), intro.playtime(), intro.program(),
                intro.sponsor1(), intro.sponsor1tel(), intro.sponsor2(), intro.sponsor2tel(),
                intro.eventhomepage(), intro.usetimefestival(), intro.spendtimefestival(),
                information.stream().map(FestivalInformationResponse::from).toList(),
                images.stream().map(FestivalImageResponse::from).toList()
        );
    }
}
