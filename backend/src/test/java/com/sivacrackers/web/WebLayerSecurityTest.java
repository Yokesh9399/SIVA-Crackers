package com.sivacrackers.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sivacrackers.common.web.HealthController;
import com.sivacrackers.config.AppProperties;
import com.sivacrackers.config.ClockConfig;
import com.sivacrackers.config.SecurityConfig;
import com.sivacrackers.security.ApiErrorWriter;
import com.sivacrackers.security.AuthCookies;
import com.sivacrackers.security.JwtService;
import com.sivacrackers.user.UserRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.sivacrackers.security.RestAccessDeniedHandler;
import com.sivacrackers.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Phase 4.1 web layer: error format, CSRF, cookies, role rules, CORS and security headers.
 * Runs without a database.
 */
@WebMvcTest(controllers = {HealthController.class, ProbeController.class})
@Import({SecurityConfig.class, ApiErrorWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        JwtService.class, AuthCookies.class, ClockConfig.class})
@EnableConfigurationProperties(AppProperties.class)
@ActiveProfiles("test")
class WebLayerSecurityTest {

    @Autowired
    MockMvc mvc;

    /** No database in this test; the session filter's user lookup is mocked (no session cookie is sent anyway). */
    @MockitoBean
    UserRepository userRepository;

    @Nested
    @DisplayName("Health and error format")
    class ErrorFormat {

        @Test
        @DisplayName("GET /api/health is public, returns {status: UP} and creates no session")
        void health() throws Exception {
            mvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(header().doesNotExist("Set-Cookie"));
        }

        @Test
        @DisplayName("Unknown public URL → 404 in the standard ApiError format")
        void unknownUrl() throws Exception {
            mvc.perform(get("/api/public/does-not-exist"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Resource not found"))
                    .andExpect(jsonPath("$.path").value("/api/public/does-not-exist"))
                    .andExpect(jsonPath("$.timestamp").isString())
                    .andExpect(jsonPath("$.fieldErrors", empty()));
        }

        @Test
        @DisplayName("NotFoundException → 404 with its message")
        void notFoundException() throws Exception {
            mvc.perform(get("/api/public/probe/not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Product not found"));
        }

        @Test
        @DisplayName("Unexpected error → 500 with a generic message and no internal details")
        void unexpectedError() throws Exception {
            MvcResult result = mvc.perform(get("/api/public/probe/boom"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Something went wrong. Please try again later."))
                    .andReturn();
            String body = result.getResponse().getContentAsString();
            assertThat(body).doesNotContain("hunter2").doesNotContain("IllegalStateException").doesNotContain("at com.");
        }

        @Test
        @DisplayName("Wrong parameter type → 400 with the field name")
        void typeMismatch() throws Exception {
            mvc.perform(get("/api/public/probe/number").param("value", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("value"));
        }

        @Test
        @DisplayName("Parameter constraint violated → 400 with field error")
        void paramValidation() throws Exception {
            mvc.perform(get("/api/public/probe/size").param("size", "5"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
        }

        @Test
        @DisplayName("Missing required parameter → 400")
        void missingParam() throws Exception {
            mvc.perform(get("/api/public/probe/number"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("value"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("is required"));
        }

        @Test
        @DisplayName("Invalid JSON body → 400 field errors; rejected values are not echoed back")
        void bodyValidation() throws Exception {
            MvcResult result = mvc.perform(post("/api/auth/probe/echo").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\",\"email\":\"not-an-email-XYZ\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                    .andExpect(jsonPath("$.fieldErrors[1].field").value("name"))
                    .andReturn();
            assertThat(result.getResponse().getContentAsString()).doesNotContain("not-an-email-XYZ");
        }

        @Test
        @DisplayName("Malformed JSON → 400 'Malformed request body'")
        void malformedJson() throws Exception {
            mvc.perform(post("/api/auth/probe/echo").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{not json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed request body"));
        }

        @Test
        @DisplayName("Unsupported HTTP method → 405 JSON")
        @WithMockUser
        void methodNotAllowed() throws Exception {
            mvc.perform(delete("/api/health").with(csrf()))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405));
        }
    }

    @Nested
    @DisplayName("CSRF protection")
    class Csrf {

        @Test
        @DisplayName("POST without CSRF token → 403 with a clear JSON message")
        void postWithoutToken() throws Exception {
            mvc.perform(post("/api/auth/probe/echo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Asha\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message", containsString("security token")));
        }

        @Test
        @DisplayName("POST with a valid CSRF token → accepted")
        void postWithToken() throws Exception {
            mvc.perform(post("/api/auth/probe/echo").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Asha\",\"email\":\"asha@example.test\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Asha"));
        }

        @Test
        @DisplayName("XSRF-TOKEN cookie is readable by the SPA but Secure and SameSite=Strict")
        void csrfCookieAttributes() throws Exception {
            MvcResult result = mvc.perform(get("/api/auth/probe/csrf"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                    .andReturn();
            // Checked on the Cookie object: Spring Security stores SameSite as a cookie attribute, which
            // Tomcat writes into Set-Cookie. (MockMvc's header text omits custom attributes, so the
            // real header is verified against the running server in Phase 4.5.)
            jakarta.servlet.http.Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
            assertThat(cookie).isNotNull();
            assertThat(cookie.getValue()).isNotBlank();
            assertThat(cookie.getPath()).isEqualTo("/");
            assertThat(cookie.getSecure()).isTrue();
            assertThat(cookie.isHttpOnly()).isFalse(); // must be readable by the SPA for the double-submit pattern
            assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
        }
    }

    @Nested
    @DisplayName("Authentication and roles")
    class Roles {

        @Test
        @DisplayName("Account area without login → 401 JSON")
        void accountRequiresLogin() throws Exception {
            mvc.perform(get("/api/account/profile"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in to continue."));
        }

        @Test
        @DisplayName("Admin area without login → 401")
        void adminRequiresLogin() throws Exception {
            mvc.perform(get("/api/admin/settings")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Admin area as CUSTOMER → 403")
        @WithMockUser(roles = "CUSTOMER")
        void adminForbiddenForCustomer() throws Exception {
            mvc.perform(get("/api/admin/settings"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("You do not have permission to do this."));
        }

        @Test
        @DisplayName("Admin area as OWNER passes security (404 only because the endpoint arrives in Phase 4.5)")
        @WithMockUser(roles = "OWNER")
        void adminAllowedForOwner() throws Exception {
            mvc.perform(get("/api/admin/settings")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Paths outside /api are denied")
        void nonApiDenied() throws Exception {
            mvc.perform(get("/some-page")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Public endpoints accept GET only without login")
        void publicPostNeedsLogin() throws Exception {
            mvc.perform(post("/api/public/probe/not-found").with(csrf())).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("CORS and headers")
    class CorsAndHeaders {

        @Test
        @DisplayName("Preflight from an allowed origin → allowed with credentials")
        void allowedOrigin() throws Exception {
            mvc.perform(options("/api/public/products")
                            .header("Origin", "http://allowed.test")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://allowed.test"))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        }

        @Test
        @DisplayName("Preflight from any other origin → rejected")
        void otherOriginRejected() throws Exception {
            mvc.perform(options("/api/public/products")
                            .header("Origin", "http://evil.test")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }

        @Test
        @DisplayName("Security headers: nosniff, frame DENY, no-referrer, no-store")
        void securityHeaders() throws Exception {
            mvc.perform(get("/api/health"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "no-referrer"))
                    .andExpect(header().string("Cache-Control", containsString("no-store")));
        }
    }
}
