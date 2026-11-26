package com.sivacrackers.media;

import java.util.Arrays;
import java.util.Optional;

/** The kinds of uploaded images, each in its own sub-folder and URL path. */
public enum ImageFolder {
    PRODUCTS("products"),
    BRANDS("brands"),
    CATEGORIES("categories");

    private final String path;

    ImageFolder(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public static Optional<ImageFolder> fromPath(String path) {
        return Arrays.stream(values()).filter(f -> f.path.equals(path)).findFirst();
    }
}
