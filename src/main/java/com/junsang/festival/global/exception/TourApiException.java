package com.junsang.festival.global.exception;

public class TourApiException extends RuntimeException {

    // 원인 예외 없이 관광공사 연동 오류 메시지를 생성한다.
    public TourApiException(String message) {
        super(message);
    }

    // 원인 예외를 포함한 관광공사 연동 오류를 생성한다.
    public TourApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
