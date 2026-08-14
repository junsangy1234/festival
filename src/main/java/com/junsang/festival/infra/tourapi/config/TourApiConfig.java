package com.junsang.festival.infra.tourapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TourApiConfig {

    // 관광공사 기본 URL을 사용하는 공통 RestClient 빈을 등록한다.
    @Bean
    RestClient tourApiRestClient(TourApiProperties properties) {
        return RestClient.builder().baseUrl(properties.baseUrl()).build();
    }
}
