package com.sivacrackers.common.exception;

import org.springframework.http.HttpStatus;

/** 409 – e.g. an email address that is already registered. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
