package com.sivacrackers.common.exception;

import org.springframework.http.HttpStatus;

/** 404 – e.g. an unknown product, category or brand slug. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
