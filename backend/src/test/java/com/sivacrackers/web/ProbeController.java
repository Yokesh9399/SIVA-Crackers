package com.sivacrackers.web;

import com.sivacrackers.common.exception.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEST-ONLY controller (lives in src/test, never shipped) used to exercise the
 * error handler and security rules before the real endpoints exist.
 */
@RestController
public class ProbeController {

    record EchoRequest(@NotBlank(message = "is required") String name,
                       @Email(message = "must be a valid email address") String email) {
    }

    @GetMapping("/api/public/probe/not-found")
    String notFound() {
        throw new NotFoundException("Product not found");
    }

    @GetMapping("/api/public/probe/boom")
    String boom() {
        throw new IllegalStateException("internal detail: db password=hunter2");
    }

    @GetMapping("/api/public/probe/number")
    int number(@RequestParam int value) {
        return value;
    }

    @GetMapping("/api/public/probe/size")
    int size(@RequestParam @Min(12) @Max(48) int size) {
        return size;
    }

    @PostMapping("/api/auth/probe/echo")
    Map<String, String> echo(@Valid @RequestBody EchoRequest request) {
        return Map.of("name", request.name());
    }

    /** Mirrors the Phase 4.5 GET /api/auth/csrf endpoint: touching the token makes Spring issue the XSRF-TOKEN cookie. */
    @GetMapping("/api/auth/probe/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName());
    }
}
