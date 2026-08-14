package com.junsang.festival.infra.tourapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tour.api")
public record TourApiProperties(
        String baseUrl,
        String serviceKey,
        String mobileOs,
        String mobileApp
) {
}
