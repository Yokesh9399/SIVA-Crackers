package com.sivacrackers.media;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Where uploaded images are kept ({@code app.media.upload-dir}, env APP_UPLOAD_DIR, default {@code uploads}
 * relative to the backend's working folder). The folder is git-ignored and never exposed directly:
 * files are served only through {@link ImageController}.
 */
@Validated
@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(@NotBlank @DefaultValue("uploads") String uploadDir) {
}
