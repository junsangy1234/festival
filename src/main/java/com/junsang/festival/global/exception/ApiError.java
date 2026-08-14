package com.junsang.festival.global.exception;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String message) {
}
