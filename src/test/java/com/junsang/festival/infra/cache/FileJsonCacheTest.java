package com.junsang.festival.infra.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class FileJsonCacheTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void reusesFreshCacheWithoutCallingLoaderAgain() {
        FileJsonCache cache = new FileJsonCache(temporaryDirectory.toString());
        AtomicInteger loadCount = new AtomicInteger();
        ObjectMapper objectMapper = new ObjectMapper();

        CachedExternalData first = cache.getOrLoad(
                "concentration-forecast",
                Map.of("areaCd", "51", "signguCd", "51130"),
                Duration.ofHours(24),
                () -> loadPayload(loadCount, objectMapper)
        );
        CachedExternalData second = cache.getOrLoad(
                "concentration-forecast",
                Map.of("signguCd", "51130", "areaCd", "51"),
                Duration.ofHours(24),
                () -> loadPayload(loadCount, objectMapper)
        );

        assertThat(loadCount).hasValue(1);
        assertThat(first.cacheHit()).isFalse();
        assertThat(second.cacheHit()).isTrue();
        assertThat(second.payload().path("value").asInt()).isEqualTo(55);
    }

    private JsonNode loadPayload(AtomicInteger loadCount, ObjectMapper objectMapper) {
        loadCount.incrementAndGet();
        return objectMapper.createObjectNode().put("value", 55);
    }
}
