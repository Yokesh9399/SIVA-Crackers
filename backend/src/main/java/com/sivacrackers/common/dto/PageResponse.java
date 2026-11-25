package com.sivacrackers.common.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * One page of results, in the exact shape the frontend already uses:
 * {@code { content, page, size, totalElements, totalPages }} with a 0-based page number.
 * totalPages is never 0 (an empty result is "page 1 of 1"), as in the Phase 2 mock API.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                Math.max(1, page.getTotalPages()));
    }
}
