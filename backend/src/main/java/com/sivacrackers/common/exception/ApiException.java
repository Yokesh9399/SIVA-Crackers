package com.sivacrackers.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected, user-facing errors. The message is shown to the client,
 * so it must never contain secrets, stack traces or internal details.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** Errors for named request fields, shown next to the form fields (empty for most errors). */
    public java.util.List<com.sivacrackers.common.dto.FieldErrorDto> getFieldErrors() {
        return java.util.List.of();
    }
}
