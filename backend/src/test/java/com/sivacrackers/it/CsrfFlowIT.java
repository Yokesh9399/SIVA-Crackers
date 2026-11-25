package com.sivacrackers.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The browser's real CSRF flow, with no test shortcuts.
 *
 * <p>Runs in its own fresh application context: Spring Security's test helper {@code csrf()} (used by
 * the other API tests) swaps the CSRF token store inside the shared context, which would hide the real
 * cookie / header behaviour checked here.
 */
@TestPropertySource(properties = "test.context=csrf-flow") // separate, untouched application context
class CsrfFlowIT extends AbstractApiIT {

    private static final String BODY = """
            {"fullName":"Asha","email":"%s","password":"Diwali2026ok"}""";

    @Test
    @DisplayName("GET /api/auth/csrf sets a readable XSRF-TOKEN cookie; echoing it in X-XSRF-TOKEN is required for POSTs")
    void realCsrfFlow() throws Exception {
        jdbc.update("DELETE FROM users");
        MvcResult r = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andReturn();
        Cookie xsrf = r.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        assertThat(xsrf.isHttpOnly()).isFalse();   // the frontend must be able to read it
        assertThat(xsrf.getSecure()).isTrue();
        assertThat(xsrf.getAttribute("SameSite")).isEqualTo("Strict");

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted("forged@example.test"))
                        .cookie(xsrf).header("X-XSRF-TOKEN", "forged"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted("nocookie@example.test"))
                        .header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isForbidden());
        MvcResult ok = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.formatted("real@example.test"))
                        .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isCreated())
                .andReturn();

        // A new CSRF token is issued at login, so a token seen before login cannot be reused afterwards
        Cookie rotated = ok.getResponse().getCookie("XSRF-TOKEN");
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(xsrf.getValue());
    }
}
