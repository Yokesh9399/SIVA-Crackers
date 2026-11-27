package com.sivacrackers.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sivacrackers.config.AppProperties;
import com.sivacrackers.user.OwnerBootstrap;
import com.sivacrackers.user.Role;
import com.sivacrackers.user.User;
import com.sivacrackers.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Registration, login, sessions, lockout, roles and staff provisioning against MySQL. */
@ExtendWith(OutputCaptureExtension.class)
class AuthApiIT extends AbstractApiIT {

    private static final String PW = "Diwali2026ok";
    private static final AtomicInteger IP = new AtomicInteger(1);

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    AppProperties props;

    /** Each test uses its own client IP so the per-IP limits never interfere between tests. */
    private String ip;

    @BeforeEach
    void cleanUsers() {
        jdbc.update("DELETE FROM users");
        ip = "10.0.0." + IP.getAndIncrement();
    }

    // ---------------------------------------------------------------- helpers

    private RequestPostProcessor fromIp() {
        return r -> {
            r.setRemoteAddr(ip);
            return r;
        };
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String body) {
        return b.contentType(MediaType.APPLICATION_JSON).content(body).with(fromIp());
    }

    private static String registerBody(String email, String password) {
        return """
                {"fullName":"Asha","email":"%s","password":"%s"}""".formatted(email, password);
    }

    private static String loginBody(String id, String password) {
        return """
                {"identifier":"%s","password":"%s"}""".formatted(id, password);
    }

    private static String sessionHeader(MvcResult r) {
        return r.getResponse().getHeaders("Set-Cookie").stream()
                .filter(h -> h.startsWith("SIVA_SESSION=")).findFirst().orElseThrow();
    }

    private static Cookie session(MvcResult r) {
        String h = sessionHeader(r);
        return new Cookie("SIVA_SESSION", h.substring("SIVA_SESSION=".length(), h.indexOf(';')));
    }

    private User createUser(String email, Role role) {
        return users.save(new User("Staff " + role, email, encoder.encode(PW), role));
    }

    private Cookie loginAs(String email, Role role) throws Exception {
        createUser(email, role);
        String path = role == Role.CUSTOMER ? "/api/auth/login" : "/api/auth/staff/login";
        return session(mvc.perform(json(post(path), loginBody(email, PW)).with(csrf()))
                .andExpect(status().isOk()).andReturn());
    }

    // ---------------------------------------------------------------- registration

    @Test
    @DisplayName("Register: 201, CUSTOMER, HttpOnly/Secure/SameSite=Strict cookie on /api, no token in the body")
    void register() throws Exception {
        MvcResult r = mvc.perform(json(post("/api/auth/register"), registerBody("Asha@Example.test", PW)).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.email").value("asha@example.test"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn();

        String cookie = sessionHeader(r);
        assertThat(cookie).contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api", "Max-Age=3600");
        assertThat(r.getResponse().getContentAsString()).doesNotContain(session(r).getValue());

        User saved = users.findByEmailIgnoreCase("asha@example.test").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getPasswordHash()).startsWith("$2").hasSize(60).isNotEqualTo(PW);
        assertThat(encoder.matches(PW, saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Register: a 'role' sent by the browser is ignored – always CUSTOMER")
    void registerCannotChooseRole() throws Exception {
        mvc.perform(json(post("/api/auth/register"),
                        """
                        {"fullName":"Eve","email":"eve@example.test","password":"%s","role":"OWNER"}""".formatted(PW))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
        assertThat(users.existsByRole(Role.OWNER)).isFalse();
    }

    @Test
    @DisplayName("Register: duplicate email (any case) → 409; weak password / bad email → 400")
    void registerValidation() throws Exception {
        mvc.perform(json(post("/api/auth/register"), registerBody("dup@example.test", PW)).with(csrf()))
                .andExpect(status().isCreated());
        mvc.perform(json(post("/api/auth/register"), registerBody("DUP@example.test", PW)).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(json(post("/api/auth/register"), registerBody("weak@example.test", "password")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
        mvc.perform(json(post("/api/auth/register"), registerBody("not-an-email", PW)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    @DisplayName("POSTs without a CSRF token are refused (403)")
    void csrfRequired() throws Exception {
        mvc.perform(json(post("/api/auth/register"), registerBody("nocsrf@example.test", PW)))
                .andExpect(status().isForbidden());
        assertThat(users.existsByEmailIgnoreCase("nocsrf@example.test")).isFalse();
    }

    // ---------------------------------------------------------------- login & lockout

    @Test
    @DisplayName("Login works with email or phone; wrong password and unknown account give the same 401")
    void login() throws Exception {
        User u = createUser("login@example.test", Role.CUSTOMER);
        u.setPhone("9876543210");
        users.save(u);

        mvc.perform(json(post("/api/auth/login"), loginBody("LOGIN@example.test", PW)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));
        mvc.perform(json(post("/api/auth/login"), loginBody("+91 98765 43210", PW)).with(csrf()))
                .andExpect(status().isOk());

        String wrongPw = mvc.perform(json(post("/api/auth/login"), loginBody("login@example.test", "Wrong12345")).with(csrf()))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknown = mvc.perform(json(post("/api/auth/login"), loginBody("nobody@example.test", "Wrong12345")).with(csrf()))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(wrongPw).contains("Incorrect email/phone or password.");
        assertThat(unknown).contains("Incorrect email/phone or password.");
        assertThat(users.findByEmailIgnoreCase("login@example.test").orElseThrow().getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("Five failed logins lock the account for 15 minutes (429 + Retry-After), even with the right password")
    void lockout() throws Exception {
        createUser("lock@example.test", Role.CUSTOMER);
        for (int i = 0; i < 5; i++) {
            mvc.perform(json(post("/api/auth/login"), loginBody("lock@example.test", "Wrong12345")).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(json(post("/api/auth/login"), loginBody("lock@example.test", PW)).with(csrf()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    @DisplayName("Customers cannot use the staff login and staff cannot use the customer login")
    void separateLogins() throws Exception {
        createUser("cust@example.test", Role.CUSTOMER);
        createUser("mgr@example.test", Role.MANAGER);
        mvc.perform(json(post("/api/auth/staff/login"), loginBody("cust@example.test", PW)).with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/auth/login"), loginBody("mgr@example.test", PW)).with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/auth/staff/login"), loginBody("mgr@example.test", PW)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("Deactivated accounts cannot log in, and their existing sessions stop working")
    void inactiveUser() throws Exception {
        Cookie c = loginAs("gone@example.test", Role.CUSTOMER);
        jdbc.update("UPDATE users SET active = FALSE WHERE email = 'gone@example.test'");
        mvc.perform(get("/api/auth/me").cookie(c)).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/auth/login"), loginBody("gone@example.test", PW)).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- session

    @Test
    @DisplayName("/me: 401 without a session, the user with one; a tampered cookie → 401 and the cookie is cleared")
    void me() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        Cookie c = loginAs("me@example.test", Role.CUSTOMER);
        mvc.perform(get("/api/auth/me").cookie(c))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.test"));

        Cookie forged = new Cookie("SIVA_SESSION", c.getValue().substring(0, c.getValue().length() - 3) + "abc");
        MvcResult r = mvc.perform(get("/api/auth/me").cookie(forged)).andExpect(status().isUnauthorized()).andReturn();
        assertThat(sessionHeader(r)).contains("Max-Age=0");
    }

    @Test
    @DisplayName("/session: always 200 – user null for visitors and for tampered cookies, the user when logged in")
    void session() throws Exception {
        mvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value(org.hamcrest.Matchers.nullValue()));
        Cookie c = loginAs("sess@example.test", Role.CUSTOMER);
        mvc.perform(get("/api/auth/session").cookie(c))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("sess@example.test"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
        MvcResult r = mvc.perform(get("/api/auth/session").cookie(new Cookie("SIVA_SESSION", "forged")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value(org.hamcrest.Matchers.nullValue()))
                .andReturn();
        assertThat(sessionHeader(r)).contains("Max-Age=0");
    }

    @Test
    @DisplayName("Logout: 204, the cookie is deleted (Max-Age=0) and the old session token no longer works on the server")
    void logout() throws Exception {
        Cookie c = loginAs("bye@example.test", Role.CUSTOMER);
        mvc.perform(get("/api/auth/me").cookie(c)).andExpect(status().isOk());
        MvcResult r = mvc.perform(post("/api/auth/logout").cookie(c).with(csrf()).with(fromIp()))
                .andExpect(status().isNoContent()).andReturn();
        assertThat(sessionHeader(r)).startsWith("SIVA_SESSION=;").contains("Max-Age=0", "HttpOnly", "Path=/api");
        // A copy of the cookie kept from before the logout is refused (found in the browser check of 27 Nov 2026)
        mvc.perform(get("/api/auth/me").cookie(c)).andExpect(status().isUnauthorized());

        Cookie staff = loginAs("bye-staff@example.test", Role.CASHIER);
        mvc.perform(get("/api/admin/bills").cookie(staff)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").cookie(staff).with(csrf()).with(fromIp())).andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/bills").cookie(staff)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Profile: view and update own name/phone")
    void profile() throws Exception {
        Cookie c = loginAs("prof@example.test", Role.CUSTOMER);
        mvc.perform(json(put("/api/account/profile"), """
                        {"fullName":"Asha Devi","phone":"98765 43211"}""").cookie(c).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Asha Devi"))
                .andExpect(jsonPath("$.phone").value("9876543211"));
        mvc.perform(get("/api/account/profile").cookie(c))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Asha Devi"));
        mvc.perform(json(put("/api/account/profile"), """
                        {"fullName":"x","phone":"12345"}""").cookie(c).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Change password: wrong current → 400; success signs out other sessions, new cookie works")
    void changePassword() throws Exception {
        Cookie old = loginAs("pw@example.test", Role.CUSTOMER);
        mvc.perform(json(put("/api/account/password"), """
                        {"currentPassword":"Wrong12345","newPassword":"NewPass2026"}""").cookie(old).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currentPassword"));

        MvcResult r = mvc.perform(json(put("/api/account/password"), """
                        {"currentPassword":"%s","newPassword":"NewPass2026"}""".formatted(PW)).cookie(old).with(csrf()))
                .andExpect(status().isOk()).andReturn();
        Cookie fresh = session(r);

        mvc.perform(get("/api/auth/me").cookie(old)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(fresh)).andExpect(status().isOk());
        mvc.perform(json(post("/api/auth/login"), loginBody("pw@example.test", PW)).with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/auth/login"), loginBody("pw@example.test", "NewPass2026")).with(csrf()))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- roles

    private static final String SETTINGS_BODY = """
            {"shopName":"SIVA Crackers","tagline":"","addressLine1":"","area":"Eachanari","city":"Coimbatore",
             "state":"Tamil Nadu","pincode":"","phone":"","alternatePhone":"","whatsappNumber":"",
             "whatsappEnquiryEnabled":false,"email":"","businessHours":"","mapEmbedUrl":"","mapIsApproximate":true,
             "licenceDetails":"","announcementText":"","announcementActive":false,"aboutText":"About",
             "priceDisclaimer":"","legalNotice":"Information only.","whatsappNotice":"",
             "highlights":[{"title":"Family-owned shop","text":"Since long ago"}],"safetyTips":["Keep water nearby"]}""";

    @Test
    @DisplayName("Admin settings: anonymous 401, CUSTOMER/CASHIER 403, MANAGER read-only, OWNER read/write")
    void roleMatrix() throws Exception {
        mvc.perform(get("/api/admin/settings")).andExpect(status().isUnauthorized());

        Cookie customer = loginAs("c@example.test", Role.CUSTOMER);
        Cookie cashier = loginAs("cash@example.test", Role.CASHIER);
        Cookie manager = loginAs("m@example.test", Role.MANAGER);
        Cookie owner = loginAs("o@example.test", Role.OWNER);

        mvc.perform(get("/api/admin/settings").cookie(customer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/settings").cookie(cashier)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/settings").cookie(manager)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/settings").cookie(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopName").value("SIVA Crackers"));

        mvc.perform(json(put("/api/admin/settings"), SETTINGS_BODY).cookie(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(json(put("/api/admin/settings"), SETTINGS_BODY.replace("\"pincode\":\"\"", "\"pincode\":\"12\""))
                        .cookie(owner).with(csrf()))
                .andExpect(status().isBadRequest());

        // Keep the current settings so the shared test database is put back afterwards
        String before = mvc.perform(get("/api/admin/settings").cookie(owner))
                .andReturn().getResponse().getContentAsString();
        try {
            mvc.perform(json(put("/api/admin/settings"), SETTINGS_BODY.replace("\"aboutText\":\"About\"", "\"aboutText\":\"Updated\""))
                            .cookie(owner).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.aboutText").value("Updated"))
                    .andExpect(jsonPath("$.phone").value(org.hamcrest.Matchers.nullValue()));
        } finally {
            mvc.perform(json(put("/api/admin/settings"), before).cookie(owner).with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Staff provisioning: only the OWNER can create MANAGER/CASHIER accounts; never another OWNER")
    void staffProvisioning() throws Exception {
        Cookie owner = loginAs("boss@example.test", Role.OWNER);
        Cookie manager = loginAs("mgr2@example.test", Role.MANAGER);
        String body = """
                {"fullName":"Ravi","email":"ravi@example.test","password":"%s","role":"%s"}""";

        mvc.perform(json(post("/api/admin/staff"), body.formatted(PW, "CASHIER")).cookie(manager).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(json(post("/api/admin/staff"), body.formatted(PW, "OWNER")).cookie(owner).with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/admin/staff"), body.formatted(PW, "CASHIER")).cookie(owner).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CASHIER"));
        mvc.perform(json(post("/api/auth/staff/login"), loginBody("ravi@example.test", PW)).with(csrf()))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- owner bootstrap

    private OwnerBootstrap bootstrap(String email, String password) {
        AppProperties p = new AppProperties(props.jwt(), props.cookie(), props.cors(), false,
                new AppProperties.Bootstrap(email, password));
        return new OwnerBootstrap(p, users, encoder);
    }

    @Test
    @DisplayName("Owner bootstrap: creates one OWNER once, never changes it, and never logs the email or password")
    void ownerBootstrap(CapturedOutput output) {
        assertThat(bootstrap("", "").createOwnerIfNeeded()).isFalse();
        assertThatThrownBy(() -> bootstrap("first.owner@example.test", "short").createOwnerIfNeeded())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("short");

        assertThat(bootstrap("First.Owner@example.test", "OwnerPass2026").createOwnerIfNeeded()).isTrue();
        User owner = users.findByEmailIgnoreCase("first.owner@example.test").orElseThrow();
        assertThat(owner.getRole()).isEqualTo(Role.OWNER);
        String hash = owner.getPasswordHash();

        assertThat(bootstrap("second.owner@example.test", "OtherPass2026").createOwnerIfNeeded()).isFalse();
        assertThat(users.findByEmailIgnoreCase("first.owner@example.test").orElseThrow().getPasswordHash()).isEqualTo(hash);
        assertThat(users.existsByEmailIgnoreCase("second.owner@example.test")).isFalse();

        List<String> secrets = Arrays.asList("first.owner", "second.owner", "OwnerPass2026", "OtherPass2026", hash);
        assertThat(output.getAll()).doesNotContain(secrets);
    }
}
