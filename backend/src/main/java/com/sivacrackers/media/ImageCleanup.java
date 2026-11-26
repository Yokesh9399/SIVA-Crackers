package com.sivacrackers.media;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Removes uploaded files that nothing uses any more (after an image is replaced or its product,
 * brand or category is deleted). Runs only after the database change has committed, and only
 * deletes a file when no product, brand or category still refers to it.
 */
@Component
public class ImageCleanup {

    private final JdbcTemplate jdbc;
    private final ImageStorage storage;

    public ImageCleanup(JdbcTemplate jdbc, ImageStorage storage) {
        this.jdbc = jdbc;
        this.storage = storage;
    }

    /** Call inside the transaction that stopped using {@code urls}. */
    public void deleteIfUnusedAfterCommit(Collection<String> urls) {
        Set<String> candidates = new LinkedHashSet<>();
        for (String url : urls) {
            if (url != null && url.startsWith("/uploads/")) {
                candidates.add(url);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            candidates.forEach(this::deleteIfUnused);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                candidates.forEach(ImageCleanup.this::deleteIfUnused);
            }
        });
    }

    private void deleteIfUnused(String url) {
        Integer uses = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM product_images WHERE url = ?)
                     + (SELECT COUNT(*) FROM brands WHERE logo_url = ?)
                     + (SELECT COUNT(*) FROM categories WHERE image_url = ?)""",
                Integer.class, url, url, url);
        if (uses != null && uses == 0) {
            storage.delete(url);
        }
    }
}
