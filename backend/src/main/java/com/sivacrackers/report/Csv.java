package com.sivacrackers.report;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.sivacrackers.common.ShopTime;

/**
 * Builds CSV files that open correctly in Excel (UTF-8 with BOM) and cannot run spreadsheet formulas:
 * text that starts with = + - @ or a control character is prefixed with an apostrophe.
 */
public final class Csv {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StringBuilder out = new StringBuilder("﻿");

    public Csv row(Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(cell(values[i]));
        }
        out.append("\r\n");
        return this;
    }

    static String cell(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof BigDecimal d) {
            return d.toPlainString();
        }
        if (v instanceof Number n) {
            return n.toString();
        }
        if (v instanceof Instant t) {
            return LocalDateTime.ofInstant(t, ShopTime.ZONE).format(TIME);
        }
        String s = v.toString();
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public ResponseEntity<byte[]> download(String fileName) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(out.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
