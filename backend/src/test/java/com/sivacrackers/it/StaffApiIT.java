package com.sivacrackers.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sivacrackers.user.Role;
import com.sivacrackers.user.User;
import com.sivacrackers.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

/** Phase 5: staff management (OWNER only) and its safety rules, against MySQL. */
class StaffApiIT extends AbstractApiIT {

    static final String PW = "Diwali2026ok";
    private static final AtomicInteger IP = new AtomicInteger(1);

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder encoder;

    Cookie owner;
    Long ownerId;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("DELETE FROM audit_log");
        jdbc.update("DELETE FROM users");
        ownerId = users.save(new User("Shop Owner", "owner@staff.test", encoder.encode(PW), Role.OWNER)).getId();
        owner = session(login("owner@staff.test", PW, true).andExpect(status().isOk()).andReturn());
    }

    ResultActions login(String email, String password, boolean staff) throws Exception {
        return mvc.perform(post(staff ? "/api/auth/staff/login" : "/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + email + "\",\"password\":\"" + password + "\"}").with(csrf())
                .with(r -> {
                    r.setRemoteAddr("10.6." + (IP.get() / 250) + "." + (IP.getAndIncrement() % 250));
                    return r;
                }));
    }

    static Cookie session(MvcResult r) {
        String h = r.getResponse().getHeaders("Set-Cookie").stream().filter(s -> s.startsWith("SIVA_SESSION=")).findFirst().orElseThrow();
        return new Cookie("SIVA_SESSION", h.substring(13, h.indexOf(';')));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String body, Cookie as) {
        return b.contentType(MediaType.APPLICATION_JSON).content(body).cookie(as).with(csrf());
    }

    private long createStaff(String name, String email, String role) throws Exception {
        MvcResult r = mvc.perform(json(post("/api/admin/staff"),
                        "{\"fullName\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + PW + "\",\"role\":\"" + role + "\"}", owner))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    @DisplayName("Only the OWNER can manage staff; MANAGER, CASHIER and CUSTOMER get 403, visitors 401")
    void ownerOnly() throws Exception {
        createStaff("Mani", "mani@staff.test", "MANAGER");
        createStaff("Kavi", "kavi@staff.test", "CASHIER");
        users.save(new User("Web Customer", "cust@staff.test", encoder.encode(PW), Role.CUSTOMER));
        Cookie manager = session(login("mani@staff.test", PW, true).andReturn());
        Cookie cashier = session(login("kavi@staff.test", PW, true).andReturn());
        Cookie customer = session(login("cust@staff.test", PW, false).andReturn());

        mvc.perform(get("/api/admin/staff")).andExpect(status().isUnauthorized());
        for (Cookie c : List.of(manager, cashier, customer)) {
            mvc.perform(get("/api/admin/staff").cookie(c)).andExpect(status().isForbidden());
            mvc.perform(json(post("/api/admin/staff"),
                    "{\"fullName\":\"X\",\"email\":\"x@staff.test\",\"password\":\"" + PW + "\",\"role\":\"CASHIER\"}", c))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/admin/staff").cookie(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("mani@staff.test")))
                .andExpect(jsonPath("$[*].email", not(hasItem("cust@staff.test")))) // website customers are not staff
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/admin/staff").param("search", "kavi").cookie(owner)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/admin/staff").param("role", "MANAGER").cookie(owner)).andExpect(jsonPath("$[0].fullName").value("Mani"));
        mvc.perform(get("/api/admin/staff").param("role", "CUSTOMER").cookie(owner)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Create: never an OWNER; duplicate email 409; weak password 400; shown with status and creation date")
    void create() throws Exception {
        mvc.perform(json(post("/api/admin/staff"), "{\"fullName\":\"Boss\",\"email\":\"boss@staff.test\",\"password\":\"" + PW + "\",\"role\":\"OWNER\"}", owner))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("role"));
        mvc.perform(json(post("/api/admin/staff"), "{\"fullName\":\"Weak\",\"email\":\"weak@staff.test\",\"password\":\"password\",\"role\":\"CASHIER\"}", owner))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
        long id = createStaff("Ravi", "ravi@staff.test", "CASHIER");
        mvc.perform(json(post("/api/admin/staff"), "{\"fullName\":\"Ravi 2\",\"email\":\"RAVI@staff.test\",\"password\":\"" + PW + "\",\"role\":\"CASHIER\"}", owner))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
        mvc.perform(get("/api/admin/staff/" + id).cookie(owner))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.role").value("CASHIER"))
                .andExpect(jsonPath("$.createdAt").exists());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action = 'STAFF_CREATED' AND entity_id = ?", Long.class, id)).isEqualTo(1);
    }

    @Test
    @DisplayName("Edit: details and role change; the role change ends the person's sessions; no self role change; no new OWNER")
    void edit() throws Exception {
        long id = createStaff("Selvi", "selvi@staff.test", "CASHIER");
        Cookie selvi = session(login("selvi@staff.test", PW, true).andReturn());
        mvc.perform(get("/api/auth/me").cookie(selvi)).andExpect(status().isOk());

        mvc.perform(json(put("/api/admin/staff/" + id), "{\"fullName\":\"Selvi R\",\"email\":\"selvi@staff.test\",\"phone\":\"98765 43219\",\"role\":\"MANAGER\"}", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.phone").value("9876543219"));
        mvc.perform(get("/api/auth/me").cookie(selvi)).andExpect(status().isUnauthorized()); // old session ended

        mvc.perform(json(put("/api/admin/staff/" + id), "{\"fullName\":\"Selvi R\",\"email\":\"selvi@staff.test\",\"role\":\"OWNER\"}", owner))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("role"));
        mvc.perform(json(put("/api/admin/staff/" + ownerId), "{\"fullName\":\"Shop Owner\",\"email\":\"owner@staff.test\",\"role\":\"MANAGER\"}", owner))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].message").value("cannot be changed for your own account"));
        mvc.perform(json(put("/api/admin/staff/" + ownerId), "{\"fullName\":\"Shop Owner K\",\"email\":\"owner@staff.test\",\"role\":\"OWNER\"}", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Shop Owner K"));
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, ownerId)).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("Deactivate: cannot log in, existing session ends; reactivate works; self and OWNER cannot be deactivated")
    void activation() throws Exception {
        long id = createStaff("Arun", "arun@staff.test", "CASHIER");
        Cookie arun = session(login("arun@staff.test", PW, true).andReturn());
        mvc.perform(json(patch("/api/admin/staff/" + id + "/status"), "{\"active\":false}", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/auth/me").cookie(arun)).andExpect(status().isUnauthorized());
        login("arun@staff.test", PW, true).andExpect(status().isUnauthorized());

        mvc.perform(json(patch("/api/admin/staff/" + id + "/status"), "{\"active\":true}", owner)).andExpect(status().isOk());
        login("arun@staff.test", PW, true).andExpect(status().isOk());

        mvc.perform(json(patch("/api/admin/staff/" + ownerId + "/status"), "{\"active\":false}", owner))
                .andExpect(status().isBadRequest());
        long other = users.save(new User("Second Owner", "owner2@staff.test", encoder.encode(PW), Role.OWNER)).getId();
        mvc.perform(json(patch("/api/admin/staff/" + other + "/status"), "{\"active\":false}", owner))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT active FROM users WHERE id = ?", Boolean.class, other)).isTrue();
    }

    @Test
    @DisplayName("Reset password: new one works, old one and old sessions do not; never logged; not for the OWNER")
    void resetPassword() throws Exception {
        long id = createStaff("Devi", "devi@staff.test", "MANAGER");
        Cookie devi = session(login("devi@staff.test", PW, true).andReturn());
        mvc.perform(json(post("/api/admin/staff/" + id + "/reset-password"), "{\"newPassword\":\"short\"}", owner))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/admin/staff/" + id + "/reset-password"), "{\"newPassword\":\"Temporary2026x\"}", owner))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").cookie(devi)).andExpect(status().isUnauthorized());
        login("devi@staff.test", PW, true).andExpect(status().isUnauthorized());
        login("devi@staff.test", "Temporary2026x", true).andExpect(status().isOk());

        mvc.perform(json(post("/api/admin/staff/" + ownerId + "/reset-password"), "{\"newPassword\":\"Temporary2026x\"}", owner))
                .andExpect(status().isBadRequest());
        String log = String.join("|", jdbc.queryForList("SELECT CONCAT(action, ' ', COALESCE(details, '')) FROM audit_log", String.class));
        assertThat(log).contains("STAFF_PASSWORD_RESET").doesNotContain("Temporary2026x");
    }

    @Test
    @DisplayName("Activity log: OWNER can read it (newest first), MANAGER cannot")
    void auditLog() throws Exception {
        createStaff("Log Test", "log@staff.test", "MANAGER");
        Cookie manager = session(login("log@staff.test", PW, true).andReturn());
        mvc.perform(get("/api/admin/audit").cookie(manager)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/audit").cookie(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("STAFF_CREATED"))
                .andExpect(jsonPath("$.content[0].actorName").value("Shop Owner"));
        mvc.perform(get("/api/admin/audit").param("size", "7").cookie(owner)).andExpect(status().isBadRequest());
    }
}
