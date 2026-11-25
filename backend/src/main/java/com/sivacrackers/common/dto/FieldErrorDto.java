package com.sivacrackers.common.dto;

/** One invalid input field: which field and what is wrong (the rejected value is never echoed back). */
public record FieldErrorDto(String field, String message) {
}
