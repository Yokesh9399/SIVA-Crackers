package com.sivacrackers.common;

import java.text.Normalizer;
import java.util.Locale;

/** URL slugs ("Flower Pots – Deluxe" → "flower-pots-deluxe"), matching the database check {@code ^[a-z0-9]+(-[a-z0-9]+)*$}. */
public final class Slugs {

    public static final String PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$";

    private Slugs() {
    }

    /** Slug from a name; empty if the name has no letters or digits. Cut to {@code maxLength} at a word boundary. */
    public static String from(String name, int maxLength) {
        if (name == null) {
            return "";
        }
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String slug = ascii.toLowerCase(Locale.ROOT)
                .replace("&", " and ")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > maxLength) {
            String cut = slug.substring(0, maxLength);
            int dash = cut.lastIndexOf('-');
            slug = (dash > 0 ? cut.substring(0, dash) : cut).replaceAll("-+$", "");
        }
        return slug;
    }
}
