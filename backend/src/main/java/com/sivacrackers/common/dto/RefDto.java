package com.sivacrackers.common.dto;

/** A short reference to a category or brand inside a product: {@code { id, name, slug }}. */
public record RefDto(Long id, String name, String slug) {
}
