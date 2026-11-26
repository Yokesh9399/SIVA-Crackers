package com.sivacrackers.media;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;

/** Small real images for tests, optionally with metadata that must be stripped. */
public final class TestImages {

    public static final String SECRET_METADATA = "GPS-LAT-11.0168-SECRET";

    private TestImages() {
    }

    public static byte[] png(int width, int height) {
        return encode("png", width, height);
    }

    public static byte[] jpeg(int width, int height) {
        return encode("jpg", width, height);
    }

    /** PNG with a tEXt chunk (like camera / editor metadata) right after IHDR. */
    public static byte[] pngWithText() {
        byte[] png = png(4, 3);
        byte[] text = ("Comment\0" + SECRET_METADATA).getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream chunk = new ByteArrayOutputStream();
        writeInt(chunk, text.length);
        byte[] typeAndData = new byte[4 + text.length];
        System.arraycopy("tEXt".getBytes(StandardCharsets.US_ASCII), 0, typeAndData, 0, 4);
        System.arraycopy(text, 0, typeAndData, 4, text.length);
        chunk.writeBytes(typeAndData);
        CRC32 crc = new CRC32();
        crc.update(typeAndData);
        writeInt(chunk, (int) crc.getValue());
        int afterIhdr = 8 + 25;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(png, 0, afterIhdr);
        out.writeBytes(chunk.toByteArray());
        out.write(png, afterIhdr, png.length - afterIhdr);
        return out.toByteArray();
    }

    /** JPEG with an APP1 "Exif" segment (where cameras store GPS) right after SOI. */
    public static byte[] jpegWithExif() {
        byte[] jpg = jpeg(4, 3);
        byte[] payload = ("Exif\0\0" + SECRET_METADATA).getBytes(StandardCharsets.ISO_8859_1);
        int len = payload.length + 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpg, 0, 2);
        out.write(0xFF);
        out.write(0xE1);
        out.write(len >> 8);
        out.write(len & 0xFF);
        out.writeBytes(payload);
        out.write(jpg, 2, jpg.length - 2);
        return out.toByteArray();
    }

    /** Minimal extended WebP: VP8X (EXIF flag set) + VP8L header + EXIF chunk. Parsed only, never decoded. */
    public static byte[] webpWithExif(int width, int height) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes("WEBP".getBytes(StandardCharsets.US_ASCII));
        byte[] vp8x = new byte[10];
        vp8x[0] = 0x08; // EXIF present
        put24(vp8x, 4, width - 1);
        put24(vp8x, 7, height - 1);
        chunk(body, "VP8X", vp8x);
        long bits = (width - 1) | ((long) (height - 1) << 14);
        byte[] vp8l = {0x2F, (byte) bits, (byte) (bits >> 8), (byte) (bits >> 16), (byte) (bits >> 24), 0};
        chunk(body, "VP8L", vp8l);
        chunk(body, "EXIF", SECRET_METADATA.getBytes(StandardCharsets.US_ASCII));
        byte[] data = body.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("RIFF".getBytes(StandardCharsets.US_ASCII));
        writeLe(out, data.length);
        out.writeBytes(data);
        return out.toByteArray();
    }

    /** PNG signature + IHDR claiming a huge picture (decompression-bomb style). */
    public static byte[] hugePngHeader() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        writeInt(out, 13);
        out.writeBytes("IHDR".getBytes(StandardCharsets.US_ASCII));
        writeInt(out, 50_000);
        writeInt(out, 50_000);
        out.writeBytes(new byte[] {8, 2, 0, 0, 0});
        writeInt(out, 0);
        writeInt(out, 0);
        out.writeBytes("IEND".getBytes(StandardCharsets.US_ASCII));
        writeInt(out, 0);
        return out.toByteArray();
    }

    private static byte[] encode(String format, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, 0xFFAA00);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) {
        out.writeBytes(type.getBytes(StandardCharsets.US_ASCII));
        writeLe(out, data.length);
        out.writeBytes(data);
        if (data.length % 2 == 1) {
            out.write(0);
        }
    }

    private static void put24(byte[] b, int at, int v) {
        b[at] = (byte) v;
        b[at + 1] = (byte) (v >> 8);
        b[at + 2] = (byte) (v >> 16);
    }

    private static void writeInt(ByteArrayOutputStream out, int v) {
        out.write(v >> 24);
        out.write(v >> 16);
        out.write(v >> 8);
        out.write(v);
    }

    private static void writeLe(ByteArrayOutputStream out, int v) {
        out.write(v);
        out.write(v >> 8);
        out.write(v >> 16);
        out.write(v >> 24);
    }
}
