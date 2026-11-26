package com.sivacrackers.media;

import java.time.Duration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves uploaded images to the website: {@code GET /uploads/{products|brands|categories}/{random}.{jpg|png|webp}}.
 * File names are never user input, unknown names give 404, and responses are locked down so an image can never
 * run as a web page (sandboxing CSP, nosniff). File names are random and never reused, so they can be cached long.
 */
@RestController
public class ImageController {

    private final ImageStorage storage;

    public ImageController(ImageStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/uploads/{folder}/{fileName}")
    public ResponseEntity<Resource> image(@PathVariable String folder, @PathVariable String fileName) {
        return storage.find(folder, fileName)
                .<ResponseEntity<Resource>>map(stored -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(stored.type().mediaType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .header("Content-Security-Policy", "default-src 'none'; sandbox")
                        .header("Content-Disposition", "inline")
                        .body(new FileSystemResource(stored.path())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
