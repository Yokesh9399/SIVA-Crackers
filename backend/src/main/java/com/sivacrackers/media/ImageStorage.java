package com.sivacrackers.media;

import com.sivacrackers.common.exception.BadRequestException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded images on the server's disk under {@code <upload-dir>/<folder>/<random>.<ext>}
 * and hands out site-relative URLs such as {@code /uploads/products/3f9c….webp}.
 *
 * <ul>
 *   <li>The browser's file name and content type are ignored; the format is detected from the bytes.</li>
 *   <li>File names are 32 random hex characters – never user input – so paths cannot be manipulated.</li>
 *   <li>Embedded metadata (EXIF/GPS, comments) is removed before saving ({@link ImageSanitizer}).</li>
 *   <li>The server folder location is never revealed to clients.</li>
 * </ul>
 */
@Component
public class ImageStorage {

    public static final long MAX_BYTES = 2L * 1024 * 1024;

    /** {@code /uploads/<folder>/<32 hex>.<ext>} – the only image URLs the admin API accepts. */
    private static final Pattern URL = Pattern.compile("^/uploads/(products|brands|categories)/([0-9a-f]{32})\\.(jpg|png|webp)$");
    private static final Pattern FILE_NAME = Pattern.compile("^[0-9a-f]{32}\\.(jpg|png|webp)$");

    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);

    private final Path root;
    private final SecureRandom random = new SecureRandom();

    public ImageStorage(MediaProperties props) {
        this.root = Path.of(props.uploadDir()).toAbsolutePath().normalize();
    }

    /** Validates, cleans and saves an upload. Returns its public URL. */
    public String store(ImageFolder folder, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose an image file to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("The file is too large. Images can be at most 2 MB.");
        }
        ImageSanitizer.Result image;
        try {
            image = ImageSanitizer.sanitize(file.getBytes());
        } catch (ImageSanitizer.InvalidImageException e) {
            throw new BadRequestException(e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the upload", e);
        }
        String name = HexFormat.of().formatHex(randomBytes()) + "." + image.type().extension();
        Path dir = root.resolve(folder.path());
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(name), image.data(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the image", e);
        }
        return "/uploads/" + folder.path() + "/" + name;
    }

    /** The stored file for a request path, if the name is valid and the file exists inside the upload folder. */
    public Optional<Stored> find(String folderPath, String fileName) {
        Optional<ImageFolder> folder = ImageFolder.fromPath(folderPath);
        if (folder.isEmpty() || fileName == null || !FILE_NAME.matcher(fileName).matches()) {
            return Optional.empty();
        }
        Path file = root.resolve(folder.get().path()).resolve(fileName).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return Optional.empty();
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
        return ImageType.fromExtension(ext).map(t -> new Stored(file, t));
    }

    /** True if {@code url} is an image uploaded through the admin into {@code folder}. */
    public static boolean isUploadUrl(String url, ImageFolder folder) {
        if (url == null) {
            return false;
        }
        Matcher m = URL.matcher(url);
        return m.matches() && m.group(1).equals(folder.path());
    }

    /** Deletes an uploaded file (no-op for anything that is not an upload URL or already gone). */
    public void delete(String url) {
        if (url == null) {
            return;
        }
        Matcher m = URL.matcher(url);
        if (!m.matches()) {
            return;
        }
        Path file = root.resolve(m.group(1)).resolve(m.group(2) + "." + m.group(3)).normalize();
        if (!file.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete an unused uploaded image ({})", e.getClass().getSimpleName());
        }
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return bytes;
    }

    public record Stored(Path path, ImageType type) {
    }
}
