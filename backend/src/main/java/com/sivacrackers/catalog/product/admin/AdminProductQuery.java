package com.sivacrackers.catalog.product.admin;

import com.sivacrackers.catalog.product.ProductSort;
import com.sivacrackers.catalog.product.StockStatus;
import com.sivacrackers.common.exception.InvalidRequestException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Filters of the admin product list. Unlike the public catalogue it includes hidden and inactive
 * products and products of inactive categories / brands.
 */
public record AdminProductQuery(
        List<String> searchWords, Long categoryId, Long brandId, StockStatus availability, Visibility visibility,
        ProductSort sort, int page, int size) {

    public static final List<Integer> PAGE_SIZES = List.of(10, 25, 50);

    /** VISIBLE = active and shown; HIDDEN = active but hidden from the website; INACTIVE = discontinued. */
    public enum Visibility { VISIBLE, HIDDEN, INACTIVE }

    public static AdminProductQuery of(String search, Long categoryId, Long brandId, String availability,
                                       String visibility, String sort, Integer page, Integer size) {
        List<String> words = search == null ? List.of()
                : Arrays.stream(search.trim().split("\\s+")).filter(w -> !w.isBlank()).limit(5).toList();
        int pageSize = size == null ? 25 : size;
        if (page != null && page < 0) {
            throw new InvalidRequestException("page", "must not be negative");
        }
        if (!PAGE_SIZES.contains(pageSize)) {
            throw new InvalidRequestException("size", "must be one of " + PAGE_SIZES);
        }
        return new AdminProductQuery(words, categoryId, brandId,
                parse(StockStatus.class, "availability", availability),
                parse(Visibility.class, "visibility", visibility),
                ProductSort.fromParam(sort), page == null ? 0 : page, pageSize);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(field, "must be one of " + Arrays.toString(type.getEnumConstants()));
        }
    }
}
