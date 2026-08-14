package com.junsang.festival.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    // 관광공사 호출 실패를 502 Bad Gateway 공통 오류 응답으로 변환한다.
    @ExceptionHandler(TourApiException.class)
    public ResponseEntity<ApiError> handleTourApiException(TourApiException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError(Instant.now(), HttpStatus.BAD_GATEWAY.value(), exception.getMessage()));
    }
}
