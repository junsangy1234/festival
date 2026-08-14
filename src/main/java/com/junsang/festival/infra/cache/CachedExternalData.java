package com.junsang.festival.infra.cache;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;

// 외부 API 원본 응답과 리포트 근거에 필요한 조회 메타데이터를 함께 전달한다.
public record CachedExternalData(
        String source,
        JsonNode payload,
        Map<String, String> requestParameters,
        Instant retrievedAt,
        boolean cacheHit
) {
}
