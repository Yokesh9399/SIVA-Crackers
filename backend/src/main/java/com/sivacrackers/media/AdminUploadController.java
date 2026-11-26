package com.sivacrackers.media;

import com.sivacrackers.common.exception.NotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * OWNER-only image upload: {@code POST /api/admin/uploads/{products|brands|categories}} with a multipart
 * field {@code file}. Returns {@code {"url": "/uploads/…"}}; the URL is then saved with the product, brand
 * or category. Validation happens here on the server (format from content, size, dimensions).
 */
@RestController
@RequestMapping("/api/admin/uploads")
@PreAuthorize("hasRole('OWNER')")
public class AdminUploadController {

    private final ImageStorage storage;

    public AdminUploadController(ImageStorage storage) {
        this.storage = storage;
    }

    @PostMapping(path = "/{folder}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@PathVariable String folder, @RequestPart("file") MultipartFile file) {
        ImageFolder target = ImageFolder.fromPath(folder).orElseThrow(() -> new NotFoundException("Unknown image folder."));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", storage.store(target, file)));
    }
}
