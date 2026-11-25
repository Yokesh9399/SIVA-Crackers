package com.sivacrackers.catalog.product;

import com.sivacrackers.common.exception.InvalidRequestException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;

/**
 * The catalogue sort options – the exact values the frontend sends in {@code ?sort=}.
 * Every option ends with a unique tie-breaker so paging is stable.
 */
public enum ProductSort {

    /** Grouped by category (shop's display order), then product order. */
    DEFAULT("default", Sort.by("category.displayOrder", "id")),
    NAME_ASC("name,asc", Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))),
    NAME_DESC("name,desc", Sort.by(Sort.Order.desc("name"), Sort.Order.desc("id"))),
    PRICE_ASC("price,asc", Sort.by(Sort.Order.asc("price"), Sort.Order.asc("name"), Sort.Order.asc("id"))),
    PRICE_DESC("price,desc", Sort.by(Sort.Order.desc("price"), Sort.Order.asc("name"), Sort.Order.asc("id"))),
    NEWEST("newest", Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

    private final String param;
    private final Sort sort;

    ProductSort(String param, Sort sort) {
        this.param = param;
        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }

    /** Parses ?sort=…; missing means DEFAULT; anything not in the list is a 400. */
    public static ProductSort fromParam(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        return Arrays.stream(values())
                .filter(s -> s.param.equals(value.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("sort", "must be one of " + allowed()));
    }

    private static String allowed() {
        return Arrays.stream(values()).map(s -> s.param).collect(Collectors.joining(", "));
    }
}
