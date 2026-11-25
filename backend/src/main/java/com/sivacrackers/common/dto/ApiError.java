package com.sivacrackers.common.dto;

import java.time.Instant;
import java.util.List;

/**
 * The single error format returned by every endpoint (validation, 401, 403, 404, 409, 429, 500).
 * The frontend's apiClient reads {@code status} and {@code message}.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDto> fieldErrors) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of());
    }

    public static ApiError of(int status, String error, String message, String path, List<FieldErrorDto> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, List.copyOf(fieldErrors));
    }
}
