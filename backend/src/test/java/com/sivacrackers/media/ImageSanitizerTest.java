package com.sivacrackers.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageSanitizerTest {

    private static String text(byte[] b) {
        return new String(b, StandardCharsets.ISO_8859_1);
    }

    @Test
    @DisplayName("JPEG: EXIF (GPS) segment removed, picture still decodes with the same size")
    void jpegExifStripped() throws Exception {
        byte[] input = TestImages.jpegWithExif();
        assertThat(text(input)).contains(TestImages.SECRET_METADATA);
        var r = ImageSanitizer.sanitize(input);
        assertThat(r.type()).isEqualTo(ImageType.JPEG);
        assertThat(r.width()).isEqualTo(4);
        assertThat(r.height()).isEqualTo(3);
        assertThat(text(r.data())).doesNotContain(TestImages.SECRET_METADATA).doesNotContain("Exif");
        assertThat(ImageIO.read(new ByteArrayInputStream(r.data())).getWidth()).isEqualTo(4);
    }

    @Test
    @DisplayName("PNG: text metadata chunk removed, picture still decodes")
    void pngTextStripped() throws Exception {
        var r = ImageSanitizer.sanitize(TestImages.pngWithText());
        assertThat(r.type()).isEqualTo(ImageType.PNG);
        assertThat(text(r.data())).doesNotContain(TestImages.SECRET_METADATA);
        assertThat(ImageIO.read(new ByteArrayInputStream(r.data())).getHeight()).isEqualTo(3);
    }

    @Test
    @DisplayName("WebP: EXIF chunk removed, EXIF flag cleared, RIFF size corrected")
    void webpExifStripped() throws Exception {
        byte[] input = TestImages.webpWithExif(640, 480);
        var r = ImageSanitizer.sanitize(input);
        assertThat(r.type()).isEqualTo(ImageType.WEBP);
        assertThat(r.width()).isEqualTo(640);
        assertThat(r.height()).isEqualTo(480);
        byte[] out = r.data();
        assertThat(text(out)).doesNotContain(TestImages.SECRET_METADATA).doesNotContain("EXIF");
        assertThat(out[20] & 0x0C).isZero(); // VP8X flags byte
        int riff = (out[4] & 0xFF) | ((out[5] & 0xFF) << 8) | ((out[6] & 0xFF) << 16) | ((out[7] & 0xFF) << 24);
        assertThat(riff).isEqualTo(out.length - 8);
    }

    @Test
    @DisplayName("Rejects non-images: HTML, SVG, text renamed to .png, empty and truncated files")
    void rejectsNonImages() {
        for (String s : new String[] {"<html><script>alert(1)</script></html>",
                "<svg xmlns='http://www.w3.org/2000/svg' onload='alert(1)'/>", "just text", ""}) {
            assertThatThrownBy(() -> ImageSanitizer.sanitize(s.getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOf(ImageSanitizer.InvalidImageException.class);
        }
        byte[] png = TestImages.png(4, 3);
        assertThatThrownBy(() -> ImageSanitizer.sanitize(Arrays.copyOf(png, 20)))
                .isInstanceOf(ImageSanitizer.InvalidImageException.class);
        byte[] jpg = TestImages.jpeg(4, 3);
        assertThatThrownBy(() -> ImageSanitizer.sanitize(Arrays.copyOf(jpg, 10)))
                .isInstanceOf(ImageSanitizer.InvalidImageException.class);
    }

    @Test
    @DisplayName("Rejects pictures larger than 8000 × 8000 pixels")
    void rejectsHugeDimensions() {
        assertThatThrownBy(() -> ImageSanitizer.sanitize(TestImages.hugePngHeader()))
                .isInstanceOf(ImageSanitizer.InvalidImageException.class)
                .hasMessageContaining("8000");
        assertThatThrownBy(() -> ImageSanitizer.sanitize(TestImages.webpWithExif(9000, 10)))
                .isInstanceOf(ImageSanitizer.InvalidImageException.class);
    }
}
