package com.junsang.festival.domain.festival.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Haversine 직선거리 계산
@Service
public class GeoDistanceService {

    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final double DISTANCE_EPSILON_KM = 1.0e-9;

    // 거리 계산
    public BigDecimal calculateKm(
            BigDecimal latitude1,
            BigDecimal longitude1,
            BigDecimal latitude2,
            BigDecimal longitude2
    ) {
        return BigDecimal.valueOf(calculateRawKm(latitude1, longitude1, latitude2, longitude2))
                .setScale(1, RoundingMode.HALF_UP);
    }

    // 반경 이내 판정
    public boolean isWithinKm(
            BigDecimal latitude1,
            BigDecimal longitude1,
            BigDecimal latitude2,
            BigDecimal longitude2,
            double radiusKm
    ) {
        return calculateRawKm(latitude1, longitude1, latitude2, longitude2) <= radiusKm + DISTANCE_EPSILON_KM;
    }

    // 원본 거리 계산
    private double calculateRawKm(
            BigDecimal latitude1,
            BigDecimal longitude1,
            BigDecimal latitude2,
            BigDecimal longitude2
    ) {
        double lat1 = Math.toRadians(latitude1.doubleValue());
        double lat2 = Math.toRadians(latitude2.doubleValue());
        double latitudeDelta = Math.toRadians(latitude2.subtract(latitude1).doubleValue());
        double longitudeDelta = Math.toRadians(longitude2.subtract(longitude1).doubleValue());

        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return EARTH_RADIUS_KM * centralAngle;
    }
}
