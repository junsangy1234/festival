package com.junsang.festival.domain.diagnosis.location;

import com.junsang.festival.domain.diagnosis.FestivalScale;
import com.junsang.festival.domain.diagnosis.FestivalType;
import com.junsang.festival.domain.diagnosis.RecurrenceType;
import com.junsang.festival.domain.diagnosis.dto.request.DiagnosisRequest;
import com.junsang.festival.domain.diagnosis.entity.Diagnosis;
import com.junsang.festival.domain.festival.service.FestivalService;
import com.junsang.festival.infra.tourapi.dto.hub.TourHubAttractionItem;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 기획서 5.5.2·5.5.3 · 좌표 확보 2단계와 미확보 시 대체 처리를 확인한다.
class FestivalLocationServiceTest {

    private final FestivalService festivalService = Mockito.mock(FestivalService.class);
    private final FestivalLocationService service = new FestivalLocationService(festivalService);

    private static final List<TourHubAttractionItem> HUB_ATTRACTIONS = List.of(
            hub("유성온천지구", "127.34", "36.35"),
            hub("유림공원", "127.36", "36.37")
    );

    @Test
    void 신규_축제는_입력한_좌표를_그대로_쓴다() {
        FestivalLocation location = service.resolve(
                diagnosis(new BigDecimal("36.3607306"), new BigDecimal("127.3577063")), HUB_ATTRACTIONS
        );

        assertThat(location.source()).isEqualTo(LocationSource.USER_INPUT);
        assertThat(location.precise()).isTrue();
        assertThat(location.notice()).isNull();
    }

    @Test
    void 좌표_미입력이면_시군구_중심으로_근사하고_안내를_남긴다() {
        FestivalLocation location = service.resolve(diagnosis(null, null), HUB_ATTRACTIONS);

        assertThat(location.source()).isEqualTo(LocationSource.SIGNGU_CENTER);
        assertThat(location.precise()).isFalse();
        assertThat(location.latitude()).isEqualByComparingTo(new BigDecimal("36.36"));
        assertThat(location.notice()).contains("R-VOL-005");
    }

    @Test
    void 대체할_좌표도_없으면_위치_미확보로_처리한다() {
        FestivalLocation location = service.resolve(diagnosis(null, null), List.of());

        assertThat(location.source()).isEqualTo(LocationSource.UNAVAILABLE);
        assertThat(location.hasCoordinates()).isFalse();
    }

    private Diagnosis diagnosis(BigDecimal latitude, BigDecimal longitude) {
        return Diagnosis.create(new DiagnosisRequest(
                "제17회 유성국화축제",
                "30",
                "30200",
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(10),
                FestivalType.ECO_NATURE,
                FestivalScale.LARGE,
                RecurrenceType.NEW,
                null,
                "대전광역시 유성구 어은로 27",
                latitude,
                longitude
        ));
    }

    private static TourHubAttractionItem hub(String name, String longitude, String latitude) {
        return new TourHubAttractionItem(
                "202503", "30", "30200", name + "-cd", name,
                "자연", "자연관광지", "공원", 1,
                new BigDecimal(longitude), new BigDecimal(latitude)
        );
    }
}
