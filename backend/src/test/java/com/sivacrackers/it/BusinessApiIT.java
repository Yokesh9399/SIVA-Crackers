package com.sivacrackers.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sivacrackers.common.ShopTime;
import com.sivacrackers.user.Role;
import com.sivacrackers.user.User;
import com.sivacrackers.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Phase 6 against MySQL: inventory, suppliers, expenses, shop customers, reports, dashboard and role rules. */
class BusinessApiIT extends AbstractApiIT {

    private static final String PW = "Diwali2026ok";
    private static final AtomicInteger IP = new AtomicInteger(1);

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    Clock clock;

    Cookie owner;
    Cookie manager;
    Cookie cashier;
    Cookie customer;

    @BeforeAll
    void sessions() throws Exception {
        owner = login("owner6@example.test", Role.OWNER);
        manager = login("manager6@example.test", Role.MANAGER);
        cashier = login("cashier6@example.test", Role.CASHIER);
        customer = login("customer6@example.test", Role.CUSTOMER);
    }

    private Cookie login(String email, Role role) throws Exception {
        users.save(new User("Test " + role, email, encoder.encode(PW), role));
        MvcResult r = mvc.perform(post(role == Role.CUSTOMER ? "/api/auth/login" : "/api/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + email + "\",\"password\":\"" + PW + "\"}").with(csrf())
                        .with(req -> {
                            req.setRemoteAddr("10.7.0." + IP.getAndIncrement());
                            return req;
                        }))
                .andExpect(status().isOk()).andReturn();
        return StaffApiIT.session(r);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String body, Cookie as) {
        return b.contentType(MediaType.APPLICATION_JSON).content(body).cookie(as).with(csrf());
    }

    private long q(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    /** A fresh product with a known stock (created through the admin API). */
    private long product(String code, int stock) throws Exception {
        String body = """
                {"name":"Stock Test %s","slug":"","sku":"%s","categoryId":%d,"brandId":%d,"description":"",
                 "packSize":"Box of 10","unit":"BOX","mrp":100,"price":90,"stockQuantity":%d,"reorderLevel":5,
                 "featured":false,"visibleOnWebsite":true,"active":true,"images":[]}"""
                .formatted(code, code, q("SELECT MIN(id) FROM categories"), q("SELECT MIN(id) FROM brands"), stock);
        MvcResult r = mvc.perform(json(post("/api/admin/products"), body, owner)).andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long supplier(String name) throws Exception {
        MvcResult r = mvc.perform(json(post("/api/admin/suppliers"), "{\"name\":\"" + name + "\",\"phone\":\"9876500001\"}", manager))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String movement(long productId, String reason, int quantity, String extra) {
        return "{\"productId\":" + productId + ",\"reason\":\"" + reason + "\",\"quantity\":" + quantity + extra + "}";
    }

    private long stock(long productId) {
        return q("SELECT stock_quantity FROM products WHERE id = ?", productId);
    }

    // ================================================================== roles

    @Test
    @DisplayName("Role table: CASHIER views stock and customers only; MANAGER runs the shop but not staff/settings/slides/log")
    void roleTable() throws Exception {
        String[] managerAndOwner = {"/api/admin/suppliers", "/api/admin/expenses", "/api/admin/expense-categories",
                "/api/admin/reports/summary", "/api/admin/reports/low-stock", "/api/admin/dashboard", "/api/admin/products"};
        for (String p : managerAndOwner) {
            mvc.perform(get(p)).andExpect(status().isUnauthorized());
            mvc.perform(get(p).cookie(customer)).andExpect(status().isForbidden());
            mvc.perform(get(p).cookie(cashier)).andExpect(status().isForbidden());
            mvc.perform(get(p).cookie(manager)).andExpect(status().isOk());
            mvc.perform(get(p).cookie(owner)).andExpect(status().isOk());
        }
        for (String p : new String[] {"/api/admin/inventory", "/api/admin/inventory/alerts", "/api/admin/inventory/movements",
                "/api/admin/customers"}) {
            mvc.perform(get(p).cookie(customer)).andExpect(status().isForbidden());
            mvc.perform(get(p).cookie(cashier)).andExpect(status().isOk());
            mvc.perform(get(p).cookie(manager)).andExpect(status().isOk());
        }
        for (String p : new String[] {"/api/admin/staff", "/api/admin/audit", "/api/admin/banners"}) {
            mvc.perform(get(p).cookie(manager)).andExpect(status().isForbidden());
            mvc.perform(get(p).cookie(cashier)).andExpect(status().isForbidden());
            mvc.perform(get(p).cookie(owner)).andExpect(status().isOk());
        }
        // Role is checked before the body is validated: no validation details leak to unauthorised users
        mvc.perform(json(put("/api/admin/settings"), "{}", manager)).andExpect(status().isForbidden());
        mvc.perform(json(post("/api/admin/products"), "{}", cashier)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
        mvc.perform(json(post("/api/admin/staff"), "{}", manager)).andExpect(status().isForbidden());
        long pid = product("ROLE-1", 10);
        mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "DAMAGED", 1, ""), cashier)).andExpect(status().isForbidden());
        // Cashiers may add a customer while billing (decided 26 Nov 2026), but not edit or delete one
        mvc.perform(json(post("/api/admin/customers"), "{\"fullName\":\"Walk In\",\"phone\":\"9876500099\"}", cashier)).andExpect(status().isCreated());
        long cid = q("SELECT id FROM shop_customers WHERE phone = '9876500099'");
        mvc.perform(json(put("/api/admin/customers/" + cid), "{\"fullName\":\"Changed\",\"phone\":\"9876500099\"}", cashier)).andExpect(status().isForbidden());
        assertThat(stock(pid)).isEqualTo(10);
    }

    // ================================================================== inventory

    @Nested
    @DisplayName("Inventory")
    class Inventory {

        @Test
        @DisplayName("Stock in from a supplier, stock out and adjustment record before/after quantities and who did it")
        void movements() throws Exception {
            long pid = product("INV-1", 10);
            long sid = supplier("Inventory Supplier");
            mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "PURCHASE", 40, ",\"supplierId\":" + sid + ",\"unitCost\":12.50"), manager))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("STOCK_IN"))
                    .andExpect(jsonPath("$.previousQuantity").value(10))
                    .andExpect(jsonPath("$.quantityChange").value(40))
                    .andExpect(jsonPath("$.newQuantity").value(50))
                    .andExpect(jsonPath("$.supplier.name").value("Inventory Supplier"))
                    .andExpect(jsonPath("$.performedByName").value("Test MANAGER"));
            mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "DAMAGED", 5, ",\"note\":\"Wet boxes\""), owner))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.newQuantity").value(45));
            mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "STOCK_COUNT", 44, ""), owner))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.quantityChange").value(-1));
            assertThat(stock(pid)).isEqualTo(44);
            // history adds up: opening stock + every change = current stock
            assertThat(q("SELECT SUM(quantity_change) FROM stock_movements WHERE product_id = ?", pid)).isEqualTo(44);
            mvc.perform(get("/api/admin/inventory/movements").param("productId", String.valueOf(pid)).cookie(cashier))
                    .andExpect(jsonPath("$.totalElements").value(4))
                    .andExpect(jsonPath("$.content[3].reason").value("INITIAL_STOCK"));
        }

        @Test
        @DisplayName("Stock can never go below 0; invalid requests change nothing")
        void neverNegative() throws Exception {
            long pid = product("INV-2", 3);
            mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "SOLD_AT_SHOP", 4, ""), manager))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));
            mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "STOCK_COUNT", 3, ""), manager))
                    .andExpect(status().isBadRequest());
            mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "PURCHASE", 0, ""), manager))
                    .andExpect(status().isBadRequest());
            mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "PRODUCT_EDIT", 9, ""), manager))
                    .andExpect(status().isBadRequest());
            mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "DAMAGED", 1, ",\"unitCost\":5"), manager))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("unitCost"));
            mvc.perform(json(post("/api/admin/inventory/movements"), movement(999999, "PURCHASE", 1, ""), manager))
                    .andExpect(status().isBadRequest());
            assertThat(stock(pid)).isEqualTo(3);
            assertThat(q("SELECT COUNT(*) FROM stock_movements WHERE product_id = ?", pid)).isEqualTo(1); // opening stock only
        }

        @Test
        @DisplayName("20 simultaneous sales of 1 unit against 5 in stock: exactly 5 succeed, stock ends at 0")
        void concurrentStockOut() throws Exception {
            long pid = product("INV-RACE", 5);
            ExecutorService pool = Executors.newFixedThreadPool(8);
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                Callable<Integer> call = () -> mvc.perform(json(post("/api/admin/inventory/movements"),
                        movement(pid, "SOLD_AT_SHOP", 1, ""), manager)).andReturn().getResponse().getStatus();
                results.add(pool.submit(call));
            }
            int created = 0;
            int refused = 0;
            for (Future<Integer> f : results) {
                int s = f.get();
                if (s == 201) {
                    created++;
                } else if (s == 409) {
                    refused++;
                }
            }
            pool.shutdown();
            assertThat(created).isEqualTo(5);
            assertThat(refused).isEqualTo(15);
            assertThat(stock(pid)).isZero();
            assertThat(q("SELECT MIN(new_quantity) FROM stock_movements WHERE product_id = ?", pid)).isZero();
        }

        @Test
        @DisplayName("Product form stock changes are recorded too; deleting a product keeps its history")
        void formChangesAndDelete() throws Exception {
            long pid = product("INV-3", 7);
            mvc.perform(json(patch("/api/admin/products/" + pid), "{\"stockQuantity\":2}", owner)).andExpect(status().isOk());
            assertThat(jdbc.queryForObject("SELECT reason FROM stock_movements WHERE product_id = ? ORDER BY id DESC LIMIT 1",
                    String.class, pid)).isEqualTo("PRODUCT_EDIT");
            mvc.perform(delete("/api/admin/products/" + pid).cookie(owner).with(csrf())).andExpect(status().isNoContent());
            assertThat(q("SELECT COUNT(*) FROM stock_movements WHERE product_sku = 'INV-3' AND product_id IS NULL")).isEqualTo(2);
            assertThat(q("SELECT COUNT(*) FROM audit_log WHERE action = 'PRODUCT_DELETED' AND entity_id = ?", pid)).isEqualTo(1);
        }

        @Test
        @DisplayName("Low-stock alerts follow each product's threshold, which staff can change")
        void alerts() throws Exception {
            long pid = product("INV-4", 6);
            mvc.perform(get("/api/admin/inventory/alerts").cookie(cashier))
                    .andExpect(jsonPath("$.lowStockCount").value(q("SELECT COUNT(*) FROM products WHERE active AND stock_quantity > 0 AND stock_quantity <= reorder_level")))
                    .andExpect(jsonPath("$.outOfStockCount").value(q("SELECT COUNT(*) FROM products WHERE active AND stock_quantity = 0")));
            mvc.perform(json(patch("/api/admin/inventory/products/" + pid + "/threshold"), "{\"reorderLevel\":10}", manager))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.stockStatus").value("LOW_STOCK"));
            mvc.perform(get("/api/admin/inventory/alerts").cookie(manager)).andExpect(jsonPath("$.lowStock[*].sku", hasItem("INV-4")));
            mvc.perform(json(patch("/api/admin/inventory/products/" + pid + "/threshold"), "{\"reorderLevel\":-1}", manager))
                    .andExpect(status().isBadRequest());
            mvc.perform(json(patch("/api/admin/inventory/products/" + pid + "/threshold"), "{\"reorderLevel\":3}", cashier))
                    .andExpect(status().isForbidden());

            // One product's stock row also says when stock last changed and who changed it (browser check, 27 Nov 2026)
            mvc.perform(json(post("/api/admin/inventory/movements"), "{\"productId\":" + pid + ",\"reason\":\"PURCHASE\",\"quantity\":1}", manager))
                    .andExpect(status().isCreated());
            String who = jdbc.queryForObject("SELECT full_name FROM users WHERE email = 'manager6@example.test'", String.class);
            mvc.perform(get("/api/admin/inventory/products/" + pid).cookie(cashier))
                    .andExpect(jsonPath("$.lastMovementBy").value(who))
                    .andExpect(jsonPath("$.lastMovementAt").isNotEmpty());
            mvc.perform(json(patch("/api/admin/inventory/products/" + pid + "/threshold"), "{\"reorderLevel\":2}", manager))
                    .andExpect(jsonPath("$.lastMovementBy").value(who));
        }
    }

    // ================================================================== suppliers

    @Test
    @DisplayName("Suppliers: create, duplicate name 409, edit, purchase history totals, deactivate blocks new deliveries")
    void suppliers() throws Exception {
        long sid = supplier("Sivakasi Test Traders");
        mvc.perform(json(post("/api/admin/suppliers"), "{\"name\":\"sivakasi test traders\"}", owner))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
        mvc.perform(json(post("/api/admin/suppliers"), "{\"name\":\"Bad Phone\",\"phone\":\"12345\"}", owner))
                .andExpect(status().isBadRequest());
        mvc.perform(json(put("/api/admin/suppliers/" + sid), "{\"name\":\"Sivakasi Test Traders\",\"contactPerson\":\"Manager\",\"email\":\"Orders@Example.TEST\"}", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("orders@example.test"));
        long pid = product("SUP-1", 0);
        mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "PURCHASE", 10, ",\"supplierId\":" + sid + ",\"unitCost\":20"), manager))
                .andExpect(status().isCreated());
        mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "PURCHASE", 5, ",\"supplierId\":" + sid), manager))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/admin/suppliers/" + sid).cookie(manager))
                .andExpect(jsonPath("$.deliveries").value(2))
                .andExpect(jsonPath("$.unitsReceived").value(15))
                .andExpect(jsonPath("$.purchaseValue").value(200.0));
        mvc.perform(json(patch("/api/admin/suppliers/" + sid + "/status"), "{\"active\":false}", manager))
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "PURCHASE", 1, ",\"supplierId\":" + sid), manager))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/suppliers").param("search", "sivakasi").param("active", "false").cookie(owner))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ================================================================== expenses

    @Test
    @DisplayName("Expenses: record, validate, edit and cancel with an audit trail; cancelled never counted")
    void expenses() throws Exception {
        long cat = q("SELECT id FROM expense_categories WHERE name = 'Electricity'");
        LocalDate today = ShopTime.today(clock);
        String body = "{\"categoryId\":" + cat + ",\"expenseDate\":\"" + today + "\",\"amount\":1250.50,\"description\":\"EB bill\",\"reference\":\"R-1\"}";
        MvcResult r = mvc.perform(json(post("/api/admin/expenses"), body, manager))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.recordedByName").value("Test MANAGER")).andReturn();
        long id = ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(json(post("/api/admin/expenses"), body.replace(today.toString(), today.plusDays(2).toString()), manager))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("expenseDate"));
        mvc.perform(json(post("/api/admin/expenses"), body.replace("1250.50", "0"), manager))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
        mvc.perform(json(post("/api/admin/expenses"), body.replace("1250.50", "10.555"), manager)).andExpect(status().isBadRequest());
        // The activity log always shows two decimals, even when the browser sends 7.5 (browser check, 27 Nov 2026)
        MvcResult small = mvc.perform(json(post("/api/admin/expenses"), body.replace("1250.50", "7.5"), manager))
                .andExpect(status().isCreated()).andReturn();
        long smallId = ((Number) JsonPath.read(small.getResponse().getContentAsString(), "$.id")).longValue();
        assertThat(jdbc.queryForObject("SELECT details FROM audit_log WHERE action = 'EXPENSE_CREATED' AND entity_id = ?", String.class, smallId))
                .contains("₹7.50");
        jdbc.update("DELETE FROM expenses WHERE id = ?", smallId); // keep the totals checked below unchanged

        mvc.perform(json(put("/api/admin/expenses/" + id), body.replace("1250.50", "1300.00"), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(1300.0));
        assertThat(jdbc.queryForObject("SELECT details FROM audit_log WHERE action = 'EXPENSE_UPDATED' AND entity_id = ?", String.class, id))
                .contains("1250.50").contains("1300.00");

        mvc.perform(get("/api/admin/expenses").param("from", today.toString()).param("to", today.toString()).cookie(manager))
                .andExpect(jsonPath("$.activeTotal").value(1300.0));
        mvc.perform(json(post("/api/admin/expenses/" + id + "/cancel"), "{\"reason\":\"\"}", owner)).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/admin/expenses/" + id + "/cancel"), "{\"reason\":\"Entered twice\"}", owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(json(put("/api/admin/expenses/" + id), body, owner)).andExpect(status().isConflict());
        mvc.perform(get("/api/admin/expenses").param("from", today.toString()).param("to", today.toString()).cookie(manager))
                .andExpect(jsonPath("$.activeTotal").value(0.0))
                .andExpect(jsonPath("$.page.totalElements").value(1)); // still listed, marked cancelled
        assertThat(q("SELECT COUNT(*) FROM expenses WHERE id = ?", id)).isEqualTo(1); // never deleted
        mvc.perform(get("/api/admin/expenses/summary/monthly").param("year", String.valueOf(today.getYear())).cookie(manager))
                .andExpect(jsonPath("$.months.length()").value(12));
        mvc.perform(json(post("/api/admin/expense-categories"), "{\"name\":\"electricity\",\"displayOrder\":1,\"active\":true}", manager))
                .andExpect(status().isConflict());
    }

    // ================================================================== customers

    @Test
    @DisplayName("Shop customers: cashier can search, manager adds/edits/deletes; phone or email required; no public access")
    void customers() throws Exception {
        mvc.perform(json(post("/api/admin/customers"), "{\"fullName\":\"No Contact\"}", manager))
                .andExpect(status().isBadRequest());
        MvcResult r = mvc.perform(json(post("/api/admin/customers"), "{\"fullName\":\"Lakshmi\",\"phone\":\"+91 98765 11111\"}", manager))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.phone").value("9876511111")).andReturn();
        long id = ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
        mvc.perform(json(post("/api/admin/customers"), "{\"fullName\":\"Other\",\"phone\":\"9876511111\"}", manager))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/admin/customers").param("search", "laks").cookie(cashier)).andExpect(jsonPath("$[0].fullName").value("Lakshmi"));
        mvc.perform(get("/api/admin/customers").param("search", "%").cookie(cashier)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/admin/customers/" + id).cookie(cashier).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/customers/" + id).cookie(manager).with(csrf())).andExpect(status().isNoContent());
        assertThat(q("SELECT COUNT(*) FROM shop_customers WHERE id = ?", id)).isZero();
        mvc.perform(get("/api/public/customers")).andExpect(status().isNotFound());
    }

    // ================================================================== reports and dashboard

    @Test
    @DisplayName("Reports: daily summary equals the recorded data (Indian date), no profit figure, CSV safe for Excel")
    void reports() throws Exception {
        long pid = product("REP-1", 0);
        mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "PURCHASE", 8, ""), manager)).andExpect(status().isCreated());
        mvc.perform(json(post("/api/admin/inventory/movements"), movement(pid, "SOLD_AT_SHOP", 3, ""), manager)).andExpect(status().isCreated());
        long cat = q("SELECT id FROM expense_categories WHERE name = 'Transport'");
        LocalDate today = ShopTime.today(clock);
        mvc.perform(json(post("/api/admin/expenses"), "{\"categoryId\":" + cat + ",\"expenseDate\":\"" + today
                + "\",\"amount\":99,\"description\":\"=HYPERLINK(\\\"http://evil.example\\\")\"}", manager)).andExpect(status().isCreated());

        String json = mvc.perform(get("/api/admin/reports/summary").param("from", today.toString()).param("to", today.toString())
                        .param("groupBy", "DAY").cookie(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].period").value(today.toString()))
                .andExpect(jsonPath("$.totals.profit").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long in = q("SELECT COALESCE(SUM(quantity_change),0) FROM stock_movements WHERE movement_type='STOCK_IN' AND created_at >= ?",
                java.time.LocalDateTime.ofInstant(ShopTime.startOf(today), java.time.ZoneOffset.UTC));
        assertThat(((Number) JsonPath.read(json, "$.totals.stockInUnits")).longValue()).isEqualTo(in);
        assertThat(new BigDecimal(JsonPath.read(json, "$.totals.expenseTotal").toString()))
                .isEqualByComparingTo(jdbc.queryForObject("SELECT SUM(amount) FROM expenses WHERE status='ACTIVE' AND expense_date = ?", BigDecimal.class, today));

        MvcResult csv = mvc.perform(get("/api/admin/reports/expenses").param("from", today.toString()).param("to", today.toString())
                        .param("format", "csv").cookie(manager))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn();
        String text = new String(csv.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(text).startsWith("﻿").contains("'=HYPERLINK").doesNotContain(",=HYPERLINK");
        for (String p : List.of("/api/admin/reports/low-stock", "/api/admin/reports/suppliers", "/api/admin/reports/stock-movements",
                "/api/admin/reports/summary")) {
            mvc.perform(get(p).param("format", "csv").cookie(owner)).andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", containsString("text/csv")));
        }
        mvc.perform(get("/api/admin/reports/summary").param("from", today.minusYears(2).toString()).param("groupBy", "DAY").cookie(owner))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/reports/summary").param("from", today.toString()).param("to", today.minusDays(1).toString()).cookie(owner))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/reports/summary").param("format", "xml").cookie(owner)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Dashboard adds live staff, supplier, expense and low-stock figures")
    void dashboard() throws Exception {
        mvc.perform(get("/api/admin/dashboard").cookie(manager))
                .andExpect(jsonPath("$.totalStaff").value(q("SELECT COUNT(*) FROM users WHERE role IN ('OWNER','MANAGER','CASHIER')")))
                .andExpect(jsonPath("$.activeSuppliers").value(q("SELECT COUNT(*) FROM suppliers WHERE active")))
                .andExpect(jsonPath("$.lowStockProducts").value(q("SELECT COUNT(*) FROM products WHERE active AND stock_quantity > 0 AND stock_quantity <= reorder_level")))
                .andExpect(jsonPath("$.expensesThisMonth").exists());
    }
}
