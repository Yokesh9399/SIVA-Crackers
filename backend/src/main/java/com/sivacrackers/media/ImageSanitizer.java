package com.sivacrackers.media;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Checks that an uploaded file really is a well-formed JPEG, PNG or WebP image of sensible size,
 * and removes embedded metadata (camera details and GPS location in EXIF/XMP, text comments)
 * so a phone photo cannot reveal more than the picture itself. Image data is copied unchanged.
 *
 * <p>Pure byte parsing – nothing is decoded or executed.
 */
public final class ImageSanitizer {

    /** Larger pictures are rejected (protects browsers and the server from oversized images). */
    public static final int MAX_DIMENSION = 8000;

    public record Result(ImageType type, byte[] data, int width, int height) {
    }

    /** Thrown for anything that is not a valid image in an accepted format. */
    public static class InvalidImageException extends Exception {
        public InvalidImageException(String message) {
            super(message);
        }
    }

    private ImageSanitizer() {
    }

    public static Result sanitize(byte[] input) throws InvalidImageException {
        ImageType type = ImageType.detect(input)
                .orElseThrow(() -> new InvalidImageException("Only JPEG, PNG or WebP images can be uploaded."));
        Result r = switch (type) {
            case JPEG -> jpeg(input);
            case PNG -> png(input);
            case WEBP -> webp(input);
        };
        if (r.width() < 1 || r.height() < 1 || r.width() > MAX_DIMENSION || r.height() > MAX_DIMENSION) {
            throw new InvalidImageException("Images must be at most " + MAX_DIMENSION + " × " + MAX_DIMENSION + " pixels.");
        }
        return r;
    }

    // ------------------------------------------------------------------ JPEG

    private static Result jpeg(byte[] b) throws InvalidImageException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(b.length);
        out.write(0xFF);
        out.write(0xD8);
        int i = 2;
        int width = 0;
        int height = 0;
        while (true) {
            if (i + 1 >= b.length || (b[i] & 0xFF) != 0xFF) {
                throw broken();
            }
            int marker = b[i + 1] & 0xFF;
            if (marker == 0xFF) { // fill byte
                i++;
                continue;
            }
            if (marker == 0xD9) { // end of image without scan data
                throw broken();
            }
            if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) { // segments without a length
                out.write(b, i, 2);
                i += 2;
                continue;
            }
            if (i + 3 >= b.length) {
                throw broken();
            }
            int len = ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
            if (len < 2 || i + 2 + len > b.length) {
                throw broken();
            }
            boolean sof = marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC;
            if (sof) {
                if (len < 7) {
                    throw broken();
                }
                height = ((b[i + 5] & 0xFF) << 8) | (b[i + 6] & 0xFF);
                width = ((b[i + 7] & 0xFF) << 8) | (b[i + 8] & 0xFF);
            }
            if (marker == 0xDA) { // start of scan: the rest is image data
                if (width == 0) {
                    throw broken();
                }
                out.write(b, i, b.length - i);
                return new Result(ImageType.JPEG, out.toByteArray(), width, height);
            }
            // APP1 (EXIF/XMP, incl. GPS), APP13 (IPTC/Photoshop) and comments are dropped
            boolean metadata = marker == 0xE1 || marker == 0xED || marker == 0xFE;
            if (!metadata) {
                out.write(b, i, 2 + len);
            }
            i += 2 + len;
        }
    }

    // ------------------------------------------------------------------ PNG

    private static final Set<String> PNG_METADATA = Set.of("tEXt", "zTXt", "iTXt", "eXIf", "tIME");

    private static Result png(byte[] b) throws InvalidImageException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(b.length);
        out.write(b, 0, 8);
        int i = 8;
        int width = 0;
        int height = 0;
        boolean first = true;
        while (i + 12 <= b.length) {
            long len = ((long) (b[i] & 0xFF) << 24) | ((b[i + 1] & 0xFF) << 16) | ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
            if (len > b.length || i + 12 + len > b.length) {
                throw broken();
            }
            String chunk = new String(b, i + 4, 4, StandardCharsets.US_ASCII);
            if (first) {
                if (!"IHDR".equals(chunk) || len != 13) {
                    throw broken();
                }
                width = readInt(b, i + 8);
                height = readInt(b, i + 12);
                first = false;
            }
            int total = (int) (12 + len);
            if (!PNG_METADATA.contains(chunk)) {
                out.write(b, i, total);
            }
            i += total;
            if ("IEND".equals(chunk)) {
                return new Result(ImageType.PNG, out.toByteArray(), width, height);
            }
        }
        throw broken();
    }

    // ------------------------------------------------------------------ WebP

    private static Result webp(byte[] b) throws InvalidImageException {
        ByteArrayOutputStream body = new ByteArrayOutputStream(b.length);
        body.write(b, 8, 4); // "WEBP"
        int i = 12;
        int width = 0;
        int height = 0;
        int vp8xFlagsAt = -1;
        boolean first = true;
        while (i + 8 <= b.length) {
            String chunk = new String(b, i, 4, StandardCharsets.US_ASCII);
            long size = (b[i + 4] & 0xFFL) | ((b[i + 5] & 0xFFL) << 8) | ((b[i + 6] & 0xFFL) << 16) | ((b[i + 7] & 0xFFL) << 24);
            long padded = size + (size & 1);
            if (i + 8 + size > b.length) {
                throw broken();
            }
            int d = i + 8;
            if (first) {
                switch (chunk) {
                    case "VP8X" -> {
                        if (size < 10) {
                            throw broken();
                        }
                        width = 1 + ((b[d + 4] & 0xFF) | ((b[d + 5] & 0xFF) << 8) | ((b[d + 6] & 0xFF) << 16));
                        height = 1 + ((b[d + 7] & 0xFF) | ((b[d + 8] & 0xFF) << 8) | ((b[d + 9] & 0xFF) << 16));
                        vp8xFlagsAt = body.size() + 8; // position of the flags byte in the rebuilt body
                    }
                    case "VP8L" -> {
                        if (size < 5 || (b[d] & 0xFF) != 0x2F) {
                            throw broken();
                        }
                        long bits = (b[d + 1] & 0xFFL) | ((b[d + 2] & 0xFFL) << 8) | ((b[d + 3] & 0xFFL) << 16) | ((b[d + 4] & 0xFFL) << 24);
                        width = (int) (bits & 0x3FFF) + 1;
                        height = (int) ((bits >> 14) & 0x3FFF) + 1;
                    }
                    case "VP8 " -> {
                        if (size < 10 || (b[d + 3] & 0xFF) != 0x9D || (b[d + 4] & 0xFF) != 0x01 || (b[d + 5] & 0xFF) != 0x2A) {
                            throw broken();
                        }
                        width = ((b[d + 6] & 0xFF) | ((b[d + 7] & 0xFF) << 8)) & 0x3FFF;
                        height = ((b[d + 8] & 0xFF) | ((b[d + 9] & 0xFF) << 8)) & 0x3FFF;
                    }
                    default -> throw broken();
                }
                first = false;
            }
            int total = (int) Math.min(8 + padded, b.length - i);
            if (!"EXIF".equals(chunk) && !"XMP ".equals(chunk)) {
                body.write(b, i, total);
            }
            i += total;
        }
        if (first) {
            throw broken();
        }
        byte[] data = body.toByteArray();
        if (vp8xFlagsAt >= 0) {
            data[vp8xFlagsAt] &= (byte) ~0x0C; // the EXIF and XMP chunks are gone
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 8);
        out.writeBytes("RIFF".getBytes(StandardCharsets.US_ASCII));
        int riffSize = data.length;
        out.write(riffSize & 0xFF);
        out.write((riffSize >> 8) & 0xFF);
        out.write((riffSize >> 16) & 0xFF);
        out.write((riffSize >> 24) & 0xFF);
        out.writeBytes(data);
        return new Result(ImageType.WEBP, out.toByteArray(), width, height);
    }

    private static int readInt(byte[] b, int at) {
        return ((b[at] & 0xFF) << 24) | ((b[at + 1] & 0xFF) << 16) | ((b[at + 2] & 0xFF) << 8) | (b[at + 3] & 0xFF);
    }

    private static InvalidImageException broken() {
        return new InvalidImageException("This file is damaged or is not a valid image.");
    }
}
