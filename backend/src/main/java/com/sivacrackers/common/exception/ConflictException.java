package com.sivacrackers.common.exception;

import com.sivacrackers.common.dto.FieldErrorDto;
import java.util.List;
import org.springframework.http.HttpStatus;

/** 409 – e.g. an email address, product code or name that is already used, or a delete that is not allowed. */
public class ConflictException extends ApiException {

    private final transient List<FieldErrorDto> fieldErrors;

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
        this.fieldErrors = List.of();
    }

    /** Conflict caused by one form field (e.g. "slug" already used); shown next to that field. */
    public ConflictException(String field, String message) {
        super(HttpStatus.CONFLICT, message);
        this.fieldErrors = List.of(new FieldErrorDto(field, message));
    }

    @Override
    public List<FieldErrorDto> getFieldErrors() {
        return fieldErrors;
    }
}
