package com.sivacrackers.catalog.product;

import com.sivacrackers.common.exception.InvalidRequestException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * A validated catalogue query (GET /api/public/products). Built only through {@link #of}, which
 * enforces the rules the frontend follows: page ≥ 0, size 12/24/48, a known sort, minPrice ≤ maxPrice.
 * (Per-field format rules – slug pattern, list length, search length – are annotations on the controller.)
 */
public record ProductQuery(
        List<String> searchWords,
        List<String> categories,
        List<String> brands,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean inStock,
        ProductSort sort,
        int page,
        int size) {

    public static final Set<Integer> PAGE_SIZES = Set.of(12, 24, 48);
    public static final int DEFAULT_SIZE = 12;

    public static ProductQuery of(String search, List<String> categories, List<String> brands, BigDecimal minPrice,
                                  BigDecimal maxPrice, Boolean inStock, String sort, Integer page, Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (!PAGE_SIZES.contains(s)) {
            throw new InvalidRequestException("size", "must be one of 12, 24, 48");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidRequestException("minPrice", "must not be greater than maxPrice");
        }
        List<String> words = search == null ? List.of()
                : Arrays.stream(search.trim().toLowerCase().split("\\s+")).filter(w -> !w.isEmpty()).toList();
        return new ProductQuery(words, clean(categories), clean(brands), minPrice, maxPrice,
                Boolean.TRUE.equals(inStock), ProductSort.fromParam(sort), p, s);
    }

    private static List<String> clean(List<String> slugs) {
        return slugs == null ? List.of() : slugs.stream().map(String::trim).filter(v -> !v.isEmpty()).distinct().toList();
    }
}
