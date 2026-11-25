package com.sivacrackers.common.exception;

import org.springframework.http.HttpStatus;

/** 400 – a request that is well-formed but breaks a business rule (e.g. minPrice greater than maxPrice). */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
