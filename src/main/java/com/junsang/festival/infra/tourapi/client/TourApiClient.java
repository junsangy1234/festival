package com.junsang.festival.infra.tourapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.junsang.festival.global.exception.TourApiException;
import com.junsang.festival.infra.tourapi.config.TourApiProperties;
import com.junsang.festival.infra.tourapi.dto.TourApiOperation;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

// 관광공사 OpenAPI와의 공통 통신, 인증키 처리, 원본 JSON 파싱을 담당한다.
@Component
public class TourApiClient {

    private final RestClient restClient;
    private final TourApiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 공통 RestClient와 관광공사 설정값을 주입한다.
    public TourApiClient(RestClient tourApiRestClient, TourApiProperties properties) {
        this.restClient = tourApiRestClient;
        this.properties = properties;
    }

    // 관광공사 API를 호출하고 원본 JSON 트리로 반환한다.
    public JsonNode request(String operationKey, Map<String, String> requestParameters) {
        String responseBody = requestResponseBody(operationKey, requestParameters);
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new TourApiException("관광공사 API 응답을 처리하지 못했습니다.", exception);
        }
    }

    // 관광공사 API를 호출하고 요청한 타입의 DTO로 역직렬화한다.
    public <T> T request(String operationKey, Map<String, String> requestParameters, Class<T> responseType) {
        String responseBody = requestResponseBody(operationKey, requestParameters);
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (Exception exception) {
            throw new TourApiException("관광공사 API 응답을 처리하지 못했습니다.", exception);
        }
    }

    // 인증·공통 파라미터를 적용해 관광공사 API의 원본 응답 문자열을 가져온다.
    private String requestResponseBody(String operationKey, Map<String, String> requestParameters) {
        if (!StringUtils.hasText(properties.serviceKey())) {
            throw new TourApiException("TOUR_API_KEY 환경변수를 설정해야 관광공사 API를 호출할 수 있습니다.");
        }

        TourApiOperation operation = TourApiOperation.fromKey(operationKey);
        Map<String, String> queryParameters = buildQueryParameters(requestParameters);

        try {
            return restClient.get()
                    .uri(buildUri(operation, queryParameters))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new TourApiException("관광공사 API 호출에 실패했습니다. HTTP " + response.getStatusCode().value());
                    })
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            throw new TourApiException("관광공사 API 호출에 실패했습니다. HTTP " + exception.getStatusCode().value(), exception);
        } catch (TourApiException exception) {
            throw exception;
        }
    }

    // 호출자가 바꾸면 안 되는 공통 파라미터를 강제하고 기본 페이징 값을 채운다.
    private Map<String, String> buildQueryParameters(Map<String, String> requestParameters) {
        Map<String, String> parameters = new LinkedHashMap<>(requestParameters);
        parameters.remove("serviceKey");
        parameters.remove("MobileOS");
        parameters.remove("MobileApp");
        parameters.put("serviceKey", normalizeServiceKey(properties.serviceKey()));
        parameters.put("MobileOS", properties.mobileOs());
        parameters.put("MobileApp", properties.mobileApp());
        parameters.putIfAbsent("_type", "json");
        parameters.putIfAbsent("pageNo", "1");
        parameters.putIfAbsent("numOfRows", "100");
        return parameters;
    }

    // URL 인코딩된 키가 들어오면 실제 인증키 문자열로 디코딩한다.
    private String normalizeServiceKey(String serviceKey) {
        return serviceKey.contains("%")
                ? URLDecoder.decode(serviceKey, StandardCharsets.UTF_8)
                : serviceKey;
    }

    // API 경로와 인코딩된 쿼리 파라미터를 합쳐 최종 요청 URI를 만든다.
    private URI buildUri(TourApiOperation operation, Map<String, String> queryParameters) {
        String query = queryParameters.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        String baseUrl = properties.baseUrl().replaceAll("/+$", "");
        return URI.create(baseUrl + operation.path() + "?" + query);
    }

    // URI 쿼리 값으로 안전하게 사용할 수 있도록 문자열을 인코딩한다.
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
