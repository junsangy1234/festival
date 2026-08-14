package com.junsang.festival.domain.diagnosis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "festival.dashboard")
public record DashboardDisplayProperties(
        int maxConcentrationPlaces,
        int maxVolatilityPlaces,
        int maxDistributionPlaces,
        int maxRelatedBasePlaces,
        int maxRelatedPlacesPerBase,
        int maxCompetingFestivals
) {
}
