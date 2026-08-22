package com.junsang.festival.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 브라우저에서 필요한 공개 설정값을 전달한다.
// Google Maps 브라우저 키는 공개되는 값이므로 콘솔에서 HTTP 리퍼러 제한을 걸어야 한다.
@RestController
@RequestMapping("/api/v1/client-config")
public class ClientConfigController {

    private final String googleMapsApiKey;

    public ClientConfigController(@Value("${festival.client.google-maps-api-key:}") String googleMapsApiKey) {
        this.googleMapsApiKey = googleMapsApiKey;
    }

    // 클라이언트 설정 조회
    @GetMapping
    public ClientConfigResponse getClientConfig() {
        return new ClientConfigResponse(googleMapsApiKey);
    }

    public record ClientConfigResponse(String googleMapsApiKey) {
    }
}
