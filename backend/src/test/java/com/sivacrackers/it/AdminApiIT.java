package com.sivacrackers.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sivacrackers.media.TestImages;
import com.sivacrackers.user.Role;
import com.sivacrackers.user.User;
import com.sivacrackers.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Phase 5 admin API against MySQL: OWNER-only access, dashboard, catalogue CRUD, banners and image uploads. */
class AdminApiIT extends AbstractApiIT {

    private static final String PW = "Diwali2026ok";
    private static final AtomicInteger IP = new AtomicInteger(1);
    static final Path UPLOADS;

    static {
        try {
            UPLOADS = Files.createTempDirectory("siva-uploads-it");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void uploadDir(DynamicPropertyRegistry registry) {
        registry.add("app.media.upload-dir", UPLOADS::toString);
    }

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder encoder;

    Cookie owner;
    Cookie manager;
    Cookie cashier;
    Cookie customer;

    @BeforeAll
    void sessions() throws Exception {
        owner = login("owner5@example.test", Role.OWNER);
        manager = login("manager5@example.test", Role.MANAGER);
        cashier = login("cashier5@example.test", Role.CASHIER);
        customer = login("customer5@example.test", Role.CUSTOMER);
    }

    @org.junit.jupiter.api.AfterAll
    void removeUploads() throws IOException {
        try (var files = Files.walk(UPLOADS)) {
            files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    private Cookie login(String email, Role role) throws Exception {
        users.save(new User("Test " + role, email, encoder.encode(PW), role));
        String path = role == Role.CUSTOMER ? "/api/auth/login" : "/api/auth/staff/login";
        MvcResult r = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + email + "\",\"password\":\"" + PW + "\"}")
                        .with(csrf()).with(req -> {
                            req.setRemoteAddr("10.5.0." + IP.getAndIncrement());
                            return req;
                        }))
                .andExpect(status().isOk()).andReturn();
        String h = r.getResponse().getHeaders("Set-Cookie").stream().filter(s -> s.startsWith("SIVA_SESSION=")).findFirst().orElseThrow();
        return new Cookie("SIVA_SESSION", h.substring(13, h.indexOf(';')));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String body) {
        return b.contentType(MediaType.APPLICATION_JSON).content(body).cookie(owner).with(csrf());
    }

    private long id(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private long categoryId() {
        return id("SELECT id FROM categories ORDER BY id LIMIT 1");
    }

    private long brandId() {
        return id("SELECT id FROM brands ORDER BY id LIMIT 1");
    }

    private String product(String name, String sku, String extra) {
        return """
                {"name":"%s","slug":"","sku":"%s","categoryId":%d,"brandId":%d,"description":"Test item",
                 "packSize":"Box of 10","unit":"BOX","mrp":200.00,"price":150.00,"stockQuantity":25,"reorderLevel":5,
                 "featured":false,"visibleOnWebsite":true,"active":true,"images":[]%s}"""
                .formatted(name, sku, categoryId(), brandId(), extra);
    }

    private String upload(String folder, byte[] bytes, String name) throws Exception {
        MvcResult r = mvc.perform(multipart("/api/admin/uploads/" + folder)
                        .file(new MockMultipartFile("file", name, "image/png", bytes)).cookie(owner).with(csrf()))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.url");
    }

    private static Path file(String url) {
        return UPLOADS.resolve(url.substring("/uploads/".length()));
    }

    // ================================================================== access control

    @Nested
    @DisplayName("Access control")
    class Access {

        @Test
        @DisplayName("Catalogue admin: visitors 401, CUSTOMER and CASHIER 403, MANAGER and OWNER 200; homepage slides OWNER only")
        void roleMatrix() throws Exception {
            for (String path : List.of("/api/admin/dashboard", "/api/admin/products", "/api/admin/categories",
                    "/api/admin/brands")) {
                mvc.perform(get(path)).andExpect(status().isUnauthorized());
                for (Cookie c : List.of(customer, cashier)) {
                    mvc.perform(get(path).cookie(c)).andExpect(status().isForbidden());
                }
                mvc.perform(get(path).cookie(manager)).andExpect(status().isOk());
                mvc.perform(get(path).cookie(owner)).andExpect(status().isOk());
            }
            mvc.perform(get("/api/admin/banners")).andExpect(status().isUnauthorized());
            for (Cookie c : List.of(customer, cashier, manager)) {
                mvc.perform(get("/api/admin/banners").cookie(c)).andExpect(status().isForbidden());
            }
            mvc.perform(get("/api/admin/banners").cookie(owner)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Catalogue writes need OWNER or MANAGER and a CSRF token; nothing is changed otherwise")
        void writesProtected() throws Exception {
            long before = id("SELECT COUNT(*) FROM products");
            String body = product("Should Not Exist", "NOPE-1", "");
            mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content(body).with(csrf()))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content(body).cookie(cashier).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content(body).cookie(owner))
                    .andExpect(status().isForbidden()); // no CSRF token
            mvc.perform(multipart("/api/admin/uploads/products").file(new MockMultipartFile("file", "a.png", "image/png",
                    TestImages.png(2, 2))).cookie(customer).with(csrf())).andExpect(status().isForbidden());
            assertThat(id("SELECT COUNT(*) FROM products")).isEqualTo(before);
        }
    }

    // ================================================================== dashboard

    @Test
    @DisplayName("Dashboard figures equal live database counts; no sales or order figures")
    void dashboard() throws Exception {
        MvcResult r = mvc.perform(get("/api/admin/dashboard").cookie(owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").doesNotExist())
                .andExpect(jsonPath("$.orders").doesNotExist())
                .andReturn();
        String json = r.getResponse().getContentAsString();
        assertThat(((Number) JsonPath.read(json, "$.totalProducts")).longValue()).isEqualTo(id("SELECT COUNT(*) FROM products"));
        assertThat(((Number) JsonPath.read(json, "$.activeProducts")).longValue()).isEqualTo(id("SELECT COUNT(*) FROM products WHERE active"));
        assertThat(((Number) JsonPath.read(json, "$.inactiveProducts")).longValue()).isEqualTo(id("SELECT COUNT(*) FROM products WHERE NOT active"));
        assertThat(((Number) JsonPath.read(json, "$.outOfStockProducts")).longValue())
                .isEqualTo(id("SELECT COUNT(*) FROM products WHERE active AND stock_quantity = 0"));
        assertThat(((Number) JsonPath.read(json, "$.lowStockProducts")).longValue())
                .isEqualTo(id("SELECT COUNT(*) FROM products WHERE active AND stock_quantity > 0 AND stock_quantity <= reorder_level"));
        assertThat(((Number) JsonPath.read(json, "$.totalCategories")).longValue()).isEqualTo(id("SELECT COUNT(*) FROM categories"));
        assertThat(((Number) JsonPath.read(json, "$.totalBrands")).longValue()).isEqualTo(id("SELECT COUNT(*) FROM brands"));
        List<Number> perCategory = JsonPath.read(json, "$.productsByCategory[*].productCount");
        assertThat(perCategory.stream().mapToLong(Number::longValue).sum()).isEqualTo(id("SELECT COUNT(*) FROM products"));
        List<Object> recent = JsonPath.read(json, "$.recentProducts");
        assertThat(recent).hasSizeLessThanOrEqualTo(5).isNotEmpty();
    }

    // ================================================================== products

    @Nested
    @DisplayName("Products")
    class Products {

        @Test
        @DisplayName("Create: 201, slug made from the name, not demo data, shown on the public website")
        void create() throws Exception {
            mvc.perform(json(post("/api/admin/products"), product("Golden Peacock Deluxe", "GPD-01", "")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.slug").value("golden-peacock-deluxe"))
                    .andExpect(jsonPath("$.isDemo").value(false))
                    .andExpect(jsonPath("$.stockStatus").value("IN_STOCK"))
                    .andExpect(jsonPath("$.publiclyVisible").value(true));
            mvc.perform(get("/api/public/products/golden-peacock-deluxe"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.price").value(150.0))
                    .andExpect(jsonPath("$.isDemo").value(false));
        }

        @Test
        @DisplayName("Validation: required fields, positive prices, price ≤ MRP, slug format, unknown category, outside images")
        void validation() throws Exception {
            mvc.perform(json(post("/api/admin/products"), "{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("price")))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("categoryId")));
            mvc.perform(json(post("/api/admin/products"), product("Zero Price", "ZP-1", "").replace("\"price\":150.00", "\"price\":0")))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
            mvc.perform(json(post("/api/admin/products"), product("Too Dear", "TD-1", "").replace("\"price\":150.00", "\"price\":250.00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("must not be more than the MRP"));
            mvc.perform(json(post("/api/admin/products"), product("Bad Slug", "BS-1", "").replace("\"slug\":\"\"", "\"slug\":\"Bad Slug!\"")))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("slug"));
            mvc.perform(json(post("/api/admin/products"), product("No Category", "NC-1", "").replaceFirst("\"categoryId\":\\d+", "\"categoryId\":999999")))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("categoryId"));
            mvc.perform(json(post("/api/admin/products"), product("Outside Image", "OI-1", "")
                            .replace("\"images\":[]", "\"images\":[{\"url\":\"https://evil.example/x.png\",\"alt\":\"x\"}]")))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("images"));
            mvc.perform(json(post("/api/admin/products"), product("Script Image", "SI-1", "")
                            .replace("\"images\":[]", "\"images\":[{\"url\":\"javascript:alert(1)\",\"alt\":\"x\"}]")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Duplicate product code or slug → 409 naming the field")
        void duplicates() throws Exception {
            mvc.perform(json(post("/api/admin/products"), product("Twin One", "TWIN-1", ""))).andExpect(status().isCreated());
            mvc.perform(json(post("/api/admin/products"), product("Twin Two", "twin-1", "")))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.fieldErrors[0].field").value("sku"));
            mvc.perform(json(post("/api/admin/products"), product("Twin One", "TWIN-2", "")))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.fieldErrors[0].field").value("slug"));
        }

        @Test
        @DisplayName("Edit: all fields saved; hiding via quick update removes it from the website")
        void editAndHide() throws Exception {
            MvcResult r = mvc.perform(json(post("/api/admin/products"), product("Edit Me", "EDIT-1", "")))
                    .andExpect(status().isCreated()).andReturn();
            int pid = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
            long otherCategory = id("SELECT id FROM categories ORDER BY id DESC LIMIT 1");
            String updated = product("Edited Name", "EDIT-1", "")
                    .replaceFirst("\"categoryId\":\\d+", "\"categoryId\":" + otherCategory)
                    .replace("\"price\":150.00", "\"price\":99.50").replace("\"stockQuantity\":25", "\"stockQuantity\":0");
            mvc.perform(json(put("/api/admin/products/" + pid), updated))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Edited Name"))
                    .andExpect(jsonPath("$.slug").value("edited-name"))
                    .andExpect(jsonPath("$.category.id").value(otherCategory))
                    .andExpect(jsonPath("$.price").value(99.5))
                    .andExpect(jsonPath("$.stockStatus").value("OUT_OF_STOCK"));
            assertThat(jdbc.queryForObject("SELECT price FROM products WHERE id = ?", java.math.BigDecimal.class, pid))
                    .isEqualByComparingTo("99.50");
            mvc.perform(get("/api/public/products/edited-name")).andExpect(status().isOk());

            mvc.perform(json(patch("/api/admin/products/" + pid), "{\"visibleOnWebsite\":false}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.publiclyVisible").value(false));
            mvc.perform(get("/api/public/products/edited-name")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Delete: confirmation is the UI's job; API deletes, the website no longer shows it; unknown id → 404")
        void deleteProduct() throws Exception {
            MvcResult r = mvc.perform(json(post("/api/admin/products"), product("Delete Me", "DEL-1", "")))
                    .andExpect(status().isCreated()).andReturn();
            int pid = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
            mvc.perform(delete("/api/admin/products/" + pid).cookie(owner).with(csrf())).andExpect(status().isNoContent());
            mvc.perform(get("/api/admin/products/" + pid).cookie(owner)).andExpect(status().isNotFound());
            mvc.perform(get("/api/public/products/delete-me")).andExpect(status().isNotFound());
            mvc.perform(delete("/api/admin/products/" + pid).cookie(owner).with(csrf())).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("List: filters and sorting match the database; inputs are parameters (SQL-like text is harmless)")
        void listFilters() throws Exception {
            mvc.perform(get("/api/admin/products").param("size", "50").cookie(owner))
                    .andExpect(jsonPath("$.totalElements").value(id("SELECT COUNT(*) FROM products")));
            long cat = categoryId();
            mvc.perform(get("/api/admin/products").param("categoryId", String.valueOf(cat)).param("size", "50").cookie(owner))
                    .andExpect(jsonPath("$.totalElements").value(id("SELECT COUNT(*) FROM products WHERE category_id = ?", cat)));
            mvc.perform(get("/api/admin/products").param("availability", "out_of_stock").cookie(owner))
                    .andExpect(jsonPath("$.totalElements").value(id("SELECT COUNT(*) FROM products WHERE stock_quantity = 0")));
            mvc.perform(get("/api/admin/products").param("visibility", "VISIBLE").cookie(owner))
                    .andExpect(jsonPath("$.totalElements").value(id("SELECT COUNT(*) FROM products WHERE active AND visible_on_website")));
            String cheapest = jdbc.queryForObject("SELECT name FROM products ORDER BY price, name, id LIMIT 1", String.class);
            mvc.perform(get("/api/admin/products").param("sort", "price,asc").cookie(owner))
                    .andExpect(jsonPath("$.content[0].name").value(cheapest));
            mvc.perform(get("/api/admin/products").param("search", "' OR 1=1 --").cookie(owner))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
            mvc.perform(get("/api/admin/products").param("size", "7").cookie(owner)).andExpect(status().isBadRequest());
            mvc.perform(get("/api/admin/products").param("visibility", "SECRET").cookie(owner)).andExpect(status().isBadRequest());
            mvc.perform(get("/api/admin/products").param("page", "-1").cookie(owner)).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Text such as <script> is stored and returned as plain text (the website shows it, never runs it)")
        void markupIsData() throws Exception {
            mvc.perform(json(post("/api/admin/products"), product("Rocket <b>Bold</b>", "XSS-1", "")
                            .replace("\"description\":\"Test item\"", "\"description\":\"<script>alert(1)</script>\"")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.description").value("<script>alert(1)</script>"))
                    .andExpect(jsonPath("$.slug").value("rocket-b-bold-b"));
        }
    }

    // ================================================================== images

    @Nested
    @DisplayName("Images")
    class Images {

        @Test
        @DisplayName("Upload: random name, metadata removed, served with locked-down headers")
        void uploadAndServe() throws Exception {
            String url = upload("products", TestImages.pngWithText(), "../../evil name.png");
            assertThat(url).matches("^/uploads/products/[0-9a-f]{32}\\.png$");
            byte[] stored = Files.readAllBytes(file(url));
            assertThat(new String(stored, StandardCharsets.ISO_8859_1)).doesNotContain(TestImages.SECRET_METADATA);

            mvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/png"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("Content-Security-Policy", containsString("sandbox")))
                    .andExpect(header().string("Cache-Control", containsString("max-age=31536000")));
            String jpg = upload("brands", TestImages.jpegWithExif(), "photo.jpeg");
            assertThat(jpg).endsWith(".jpg");
            assertThat(new String(Files.readAllBytes(file(jpg)), StandardCharsets.ISO_8859_1)).doesNotContain(TestImages.SECRET_METADATA);
        }

        @Test
        @DisplayName("Rejected: HTML or SVG disguised as images, empty, over 2 MB, unknown folder; bad names → 404")
        void rejected() throws Exception {
            for (String evil : List.of("<html><script>alert(1)</script></html>", "<svg xmlns='http://www.w3.org/2000/svg'/>")) {
                mvc.perform(multipart("/api/admin/uploads/products")
                                .file(new MockMultipartFile("file", "x.png", "image/png", evil.getBytes(StandardCharsets.UTF_8)))
                                .cookie(owner).with(csrf()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value("Only JPEG, PNG or WebP images can be uploaded."));
            }
            mvc.perform(multipart("/api/admin/uploads/products").file(new MockMultipartFile("file", "x.png", "image/png", new byte[0]))
                    .cookie(owner).with(csrf())).andExpect(status().isBadRequest());
            byte[] big = new byte[2 * 1024 * 1024 + 10];
            System.arraycopy(TestImages.png(2, 2), 0, big, 0, 8);
            mvc.perform(multipart("/api/admin/uploads/products").file(new MockMultipartFile("file", "big.png", "image/png", big))
                    .cookie(owner).with(csrf())).andExpect(status().is4xxClientError());
            mvc.perform(multipart("/api/admin/uploads/secrets").file(new MockMultipartFile("file", "x.png", "image/png", TestImages.png(2, 2)))
                    .cookie(owner).with(csrf())).andExpect(status().isNotFound());
            mvc.perform(get("/uploads/products/abc.png")).andExpect(status().isNotFound());
            mvc.perform(get("/uploads/secrets/0123456789abcdef0123456789abcdef.png")).andExpect(status().isNotFound());
            mvc.perform(get("/uploads/products/0123456789abcdef0123456789abcdef.png")).andExpect(status().isNotFound());
            mvc.perform(get("/uploads/products/x/y.png")).andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Product images: shown on the website; a replaced or deleted product's image file is removed")
        void productImagesLifecycle() throws Exception {
            String first = upload("products", TestImages.png(8, 8), "a.png");
            String second = upload("products", TestImages.png(9, 9), "b.png");
            MvcResult r = mvc.perform(json(post("/api/admin/products"), product("Photo Rocket", "PHOTO-1", "")
                            .replace("\"images\":[]", "\"images\":[{\"url\":\"" + first + "\",\"alt\":\"Box front\"}]")))
                    .andExpect(status().isCreated()).andReturn();
            int pid = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
            mvc.perform(get("/api/public/products/photo-rocket")).andExpect(jsonPath("$.images[0].url").value(first));

            mvc.perform(json(put("/api/admin/products/" + pid), product("Photo Rocket", "PHOTO-1", "")
                            .replace("\"images\":[]", "\"images\":[{\"url\":\"" + second + "\",\"alt\":\"\"}]")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.images[0].alt").value("Photo Rocket"));
            assertThat(Files.exists(file(first))).as("replaced image removed").isFalse();
            assertThat(Files.exists(file(second))).isTrue();

            mvc.perform(delete("/api/admin/products/" + pid).cookie(owner).with(csrf())).andExpect(status().isNoContent());
            assertThat(Files.exists(file(second))).as("deleted product's image removed").isFalse();
        }
    }

    // ================================================================== categories and brands

    @Nested
    @DisplayName("Categories and brands")
    class CategoriesAndBrands {

        @Test
        @DisplayName("Category CRUD: create, duplicate name (any case) 409, edit, shown on the website, delete")
        void categoryCrud() throws Exception {
            String body = """
                    {"name":"Night Wonders","slug":"","description":"Evening fireworks","imageUrl":"","displayOrder":50,
                     "active":true,"safetyNotes":["Light at arm's length"]}""";
            MvcResult r = mvc.perform(json(post("/api/admin/categories"), body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.slug").value("night-wonders"))
                    .andExpect(jsonPath("$.productCount").value(0))
                    .andReturn();
            int cid = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
            mvc.perform(json(post("/api/admin/categories"), body.replace("Night Wonders", "NIGHT wonders")))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
            mvc.perform(json(post("/api/admin/categories"), body.replace("\"Night Wonders\"", "\"\"")))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("name"));

            mvc.perform(json(put("/api/admin/categories/" + cid), body.replace("Evening fireworks", "After sunset")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("After sunset"));
            mvc.perform(get("/api/public/categories/night-wonders"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("After sunset"));
            mvc.perform(get("/api/admin/categories").param("search", "night").cookie(owner))
                    .andExpect(jsonPath("$[*].name", hasItem("Night Wonders")));

            mvc.perform(delete("/api/admin/categories/" + cid).cookie(owner).with(csrf())).andExpect(status().isNoContent());
            mvc.perform(get("/api/public/categories/night-wonders")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("A category or brand that still has products cannot be deleted (409 with an explanation)")
        void deleteBlocked() throws Exception {
            long cat = categoryId();
            long brand = brandId();
            long before = id("SELECT COUNT(*) FROM products");
            mvc.perform(delete("/api/admin/categories/" + cat).cookie(owner).with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("still has")))
                    .andExpect(jsonPath("$.message", containsString("inactive")));
            mvc.perform(delete("/api/admin/brands/" + brand).cookie(owner).with(csrf()))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.message", containsString("still has")));
            assertThat(id("SELECT COUNT(*) FROM products")).isEqualTo(before);
            assertThat(id("SELECT COUNT(*) FROM categories WHERE id = ?", cat)).isEqualTo(1);
        }

        @Test
        @DisplayName("Brand CRUD with logo upload; inactive brand disappears from the public Brands page")
        void brandCrud() throws Exception {
            String logo = upload("brands", TestImages.png(16, 16), "logo.png");
            String body = """
                    {"name":"Sky Lanterns Co","slug":"","description":"Test brand","logoUrl":"%s","displayOrder":60,"active":true}"""
                    .formatted(logo);
            MvcResult r = mvc.perform(json(post("/api/admin/brands"), body))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.logoUrl").value(logo)).andReturn();
            int bid = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
            mvc.perform(json(post("/api/admin/brands"), body.replace("Sky Lanterns Co", "sky lanterns co")))
                    .andExpect(status().isConflict());
            mvc.perform(json(post("/api/admin/brands"), body.replace("Sky Lanterns Co", "Wrong Folder Logo")
                            .replace(logo, "/uploads/products/0123456789abcdef0123456789abcdef.png")))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("logoUrl"));

            mvc.perform(get("/api/public/brands")).andExpect(jsonPath("$[*].slug", hasItem("sky-lanterns-co")));
            mvc.perform(json(put("/api/admin/brands/" + bid), body.replace("\"active\":true", "\"active\":false")))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/public/brands")).andExpect(jsonPath("$[*].slug", not(hasItem("sky-lanterns-co"))));
            mvc.perform(delete("/api/admin/brands/" + bid).cookie(owner).with(csrf())).andExpect(status().isNoContent());
            assertThat(Files.exists(file(logo))).as("deleted brand's logo removed").isFalse();
        }
    }

    // ================================================================== banners

    @Test
    @DisplayName("Homepage slides: internal links only, button needs label and link, shown on the website, deletable")
    void banners() throws Exception {
        String body = """
                {"eyebrow":"Festival","title":"Test slide","subtitle":"Sub","ctaLabel":"See products","ctaLink":"/products",
                 "theme":"orange","displayOrder":99,"active":true}""";
        mvc.perform(json(post("/api/admin/banners"), body.replace("/products", "https://evil.example")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("ctaLink"));
        mvc.perform(json(post("/api/admin/banners"), body.replace("/products", "//evil.example")))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/admin/banners"), body.replace("\"ctaLink\":\"/products\"", "\"ctaLink\":\"\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("ctaLink"));
        mvc.perform(json(post("/api/admin/banners"), body.replace("orange", "purple"))).andExpect(status().isBadRequest());

        MvcResult r = mvc.perform(json(post("/api/admin/banners"), body)).andExpect(status().isCreated()).andReturn();
        int id = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
        try {
            mvc.perform(get("/api/public/banners")).andExpect(jsonPath("$[*].title", hasItem("Test slide")));
            mvc.perform(json(put("/api/admin/banners/" + id), body.replace("\"active\":true", "\"active\":false")))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/public/banners")).andExpect(jsonPath("$[*].title", not(hasItem("Test slide"))));
        } finally {
            mvc.perform(delete("/api/admin/banners/" + id).cookie(owner).with(csrf())).andExpect(status().isNoContent());
        }
        assertThat(id("SELECT COUNT(*) FROM banners WHERE title = 'Test slide'")).isZero();
    }
}
