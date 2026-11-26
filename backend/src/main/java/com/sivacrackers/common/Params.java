package com.sivacrackers.common;

import com.sivacrackers.common.exception.InvalidRequestException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Small helpers for validating query parameters with friendly 400 messages. */
public final class Params {

    private Params() {
    }

    public static <E extends Enum<E>> E enumOrNull(Class<E> type, String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(field, "must be one of " + Arrays.toString(type.getEnumConstants()));
        }
    }

    public static int page(Integer page) {
        if (page != null && page < 0) {
            throw new InvalidRequestException("page", "must not be negative");
        }
        return page == null ? 0 : page;
    }

    public static int size(Integer size, List<Integer> allowed, int fallback) {
        int s = size == null ? fallback : size;
        if (!allowed.contains(s)) {
            throw new InvalidRequestException("size", "must be one of " + allowed);
        }
        return s;
    }

    public static String search(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() > 100 ? t.substring(0, 100) : t;
    }
}
