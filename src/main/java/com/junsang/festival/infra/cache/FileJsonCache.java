package com.junsang.festival.infra.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

// 외부 API 원본 응답을 JSON 파일로 보관하고 TTL 안에서는 재사용한다.
@Component
public class FileJsonCache {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Path rootDirectory;

    public FileJsonCache(@Value("${festival.cache.root-directory:./cache}") String rootDirectory) {
        this.rootDirectory = Path.of(rootDirectory);
    }

    // 캐시가 유효하면 원본 응답을 재사용하고, 없거나 만료되면 loader로 새 데이터를 조회한다.
    public CachedExternalData getOrLoad(
            String source,
            Map<String, String> requestParameters,
            Duration ttl,
            Supplier<JsonNode> loader
    ) {
        Map<String, String> normalizedParameters = Map.copyOf(new TreeMap<>(requestParameters));
        Path cacheFile = cacheFile(source, normalizedParameters);
        CacheEntry cached = readIfFresh(cacheFile, ttl);
        if (cached != null) {
            return new CachedExternalData(
                    cached.source(), cached.payload(), cached.requestParameters(), cached.retrievedAt(), true
            );
        }

        JsonNode payload = loader.get();
        Instant retrievedAt = Instant.now();
        CacheEntry entry = new CacheEntry(source, normalizedParameters, retrievedAt, payload);
        write(cacheFile, entry);
        return new CachedExternalData(source, payload, normalizedParameters, retrievedAt, false);
    }

    // 캐시 파일의 조회 시각을 기준으로 TTL 안에 있는 항목만 읽는다.
    private CacheEntry readIfFresh(Path cacheFile, Duration ttl) {
        if (!Files.exists(cacheFile)) {
            return null;
        }

        try {
            CacheEntry entry = objectMapper.readValue(cacheFile.toFile(), CacheEntry.class);
            return entry.retrievedAt().plus(ttl).isAfter(Instant.now()) ? entry : null;
        } catch (IOException exception) {
            return null;
        }
    }

    // 새 원본 응답과 호출 파라미터를 캐시 파일에 저장한다.
    private void write(Path cacheFile, CacheEntry entry) {
        try {
            Files.createDirectories(cacheFile.getParent());
            objectMapper.writeValue(cacheFile.toFile(), entry);
        } catch (IOException exception) {
            throw new IllegalStateException("외부 API 캐시를 저장하지 못했습니다: " + cacheFile, exception);
        }
    }

    // API 이름과 정렬된 파라미터를 해시해 파일 경로로 안전하게 사용한다.
    private Path cacheFile(String source, Map<String, String> requestParameters) {
        String canonicalParameters = requestParameters.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        return rootDirectory.resolve(source).resolve(sha256(canonicalParameters) + ".json");
    }

    // 파라미터 문자열을 고정 길이 파일명으로 변환한다.
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("캐시 키를 생성하지 못했습니다.", exception);
        }
    }

    private record CacheEntry(
            String source,
            Map<String, String> requestParameters,
            Instant retrievedAt,
            JsonNode payload
    ) {
    }
}
