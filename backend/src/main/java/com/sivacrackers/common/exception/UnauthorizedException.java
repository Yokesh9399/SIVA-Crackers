package com.sivacrackers.common.exception;

import org.springframework.http.HttpStatus;

/** 401 – e.g. wrong login details. The message never says whether the account exists. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
