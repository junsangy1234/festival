package com.junsang.festival.domain.diagnosis.region.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisRegionCodeServiceTest {

    private final DiagnosisRegionCodeService service = new DiagnosisRegionCodeService();

    @Test
    void returnsTourismDataLabCodesForGangwonAndWonju() {
        service.initialize();

        assertThat(service.getRegions().regions())
                .anySatisfy(region -> {
                    assertThat(region.areaCode()).isEqualTo("51");
                    assertThat(region.name()).isEqualTo("강원특별자치도");
                });
        assertThat(service.getDistricts("51").districts())
                .anySatisfy(district -> {
                    assertThat(district.signguCode()).isEqualTo("51130");
                    assertThat(district.name()).isEqualTo("원주시");
                });
    }
}
