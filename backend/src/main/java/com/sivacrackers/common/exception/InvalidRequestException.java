package com.sivacrackers.common.exception;

import com.sivacrackers.common.dto.FieldErrorDto;
import java.util.List;
import org.springframework.http.HttpStatus;

/** 400 "Validation failed" for one or more named request fields (same shape as annotation-based validation). */
public class InvalidRequestException extends ApiException {

    private final transient List<FieldErrorDto> fieldErrors;

    public InvalidRequestException(String field, String message) {
        super(HttpStatus.BAD_REQUEST, "Validation failed");
        this.fieldErrors = List.of(new FieldErrorDto(field, message));
    }

    public List<FieldErrorDto> getFieldErrors() {
        return fieldErrors;
    }
}
