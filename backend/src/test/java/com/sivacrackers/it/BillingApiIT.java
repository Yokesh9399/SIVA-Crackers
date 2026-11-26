package com.sivacrackers.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.sivacrackers.common.ShopTime;
import com.sivacrackers.user.Role;
import com.sivacrackers.user.User;
import com.sivacrackers.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Phase 6: internal in-store billing against MySQL – roles, totals, stock deducted exactly once, voids, invoices, history, reports. */
class BillingApiIT extends AbstractApiIT {

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
        owner = login("owner-b@example.test", Role.OWNER);
        manager = login("manager-b@example.test", Role.MANAGER);
        cashier = login("cashier-b@example.test", Role.CASHIER);
        customer = login("customer-b@example.test", Role.CUSTOMER);
    }

    private Cookie login(String email, Role role) throws Exception {
        users.save(new User("Test " + role, email, encoder.encode(PW), role));
        MvcResult r = mvc.perform(post(role == Role.CUSTOMER ? "/api/auth/login" : "/api/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + email + "\",\"password\":\"" + PW + "\"}").with(csrf())
                        .with(req -> {
                            req.setRemoteAddr("10.8.0." + IP.getAndIncrement());
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

    private BigDecimal money(String sql, Object... args) {
        return jdbc.queryForObject(sql, BigDecimal.class, args);
    }

    /** A product with a known price and stock, created through the admin API. */
    private long product(String code, String price, int stock) throws Exception {
        String body = """
                {"name":"Bill Test %s","slug":"","sku":"%s","categoryId":%d,"brandId":%d,"description":"",
                 "packSize":"Box of 10","unit":"BOX","mrp":%s,"price":%s,"stockQuantity":%d,"reorderLevel":2,
                 "featured":false,"visibleOnWebsite":true,"active":true,"images":[]}"""
                .formatted(code, code, q("SELECT MIN(id) FROM categories"), q("SELECT MIN(id) FROM brands"), price, price, stock);
        MvcResult r = mvc.perform(json(post("/api/admin/products"), body, owner)).andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String bill(String key, String items, String extra) {
        return "{\"requestKey\":\"" + key + "\",\"items\":[" + items + "],\"paymentMethod\":\"CASH\",\"paymentStatus\":\"PAID\"" + extra + "}";
    }

    private static String line(long productId, int qty) {
        return "{\"productId\":" + productId + ",\"quantity\":" + qty + "}";
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    private ResultActions create(String body, Cookie as) throws Exception {
        return mvc.perform(json(post("/api/admin/bills"), body, as));
    }

    private long id(MvcResult r) throws Exception {
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long stock(long productId) {
        return q("SELECT stock_quantity FROM products WHERE id = ?", productId);
    }

    // ================================================================== access

    @Test
    @DisplayName("Roles: every staff role bills and prints; only OWNER/MANAGER void; only OWNER manages payment methods; others 401/403")
    void roles() throws Exception {
        long pid = product("ROLE-B", "50.00", 10);
        mvc.perform(get("/api/admin/bills")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/bills").cookie(customer)).andExpect(status().isForbidden());
        create(bill(key(), line(pid, 1), ""), customer).andExpect(status().isForbidden());

        MvcResult r = create(bill(key(), line(pid, 1), ""), cashier).andExpect(status().isCreated()).andReturn();
        long billId = id(r);
        mvc.perform(get("/api/admin/bills").cookie(cashier)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/bills/" + billId + "/invoice.pdf").cookie(cashier)).andExpect(status().isOk());
        mvc.perform(json(post("/api/admin/bills/" + billId + "/void"), "{\"reason\":\"test\"}", cashier)).andExpect(status().isForbidden());
        mvc.perform(json(post("/api/admin/bills/" + billId + "/void"), "{}", cashier)).andExpect(status().isForbidden()); // role before validation

        mvc.perform(get("/api/admin/payment-methods").cookie(cashier)).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("CASH"));
        String method = "{\"code\":\"BANK_TRANSFER\",\"label\":\"Bank transfer (in-store)\",\"displayOrder\":9,\"active\":true}";
        mvc.perform(json(post("/api/admin/payment-methods"), method, manager)).andExpect(status().isForbidden());
        mvc.perform(json(post("/api/admin/payment-methods"), method, owner)).andExpect(status().isCreated());
        mvc.perform(json(post("/api/admin/payment-methods"), method, owner)).andExpect(status().isConflict());
        assertThat(q("SELECT COUNT(*) FROM bills WHERE id = ?", billId)).isEqualTo(1);
    }

    // ================================================================== creating bills

    @Nested
    @DisplayName("Creating bills")
    class Create {

        @Test
        @DisplayName("Totals from current prices, unique bill number, stock deducted once per line with linked movements")
        void createsBill() throws Exception {
            long a = product("BILL-A", "120.50", 10);
            long b = product("BILL-B", "35.00", 5);
            long cust = q("SELECT 0"); // placeholder replaced below
            MvcResult c = mvc.perform(json(post("/api/admin/customers"), "{\"fullName\":\"Ravi\",\"phone\":\"9876522222\"}", cashier))
                    .andExpect(status().isCreated()).andReturn();
            cust = id(c);
            MvcResult r = create(bill(key(), line(a, 2) + "," + line(b, 3) + "," + line(a, 1),
                    ",\"customerId\":" + cust + ",\"discountType\":\"PERCENT\",\"discountValue\":10,\"amountReceived\":500"), cashier)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.billNumber").value(org.hamcrest.Matchers.matchesPattern("^SC-\\d{4}-\\d{6}$")))
                    .andExpect(jsonPath("$.items.length()").value(2)) // repeated product merged
                    .andExpect(jsonPath("$.subtotal").value(466.5))  // 3 × 120.50 + 3 × 35
                    .andExpect(jsonPath("$.discountAmount").value(46.65))
                    .andExpect(jsonPath("$.total").value(419.85))
                    .andExpect(jsonPath("$.changeDue").value(80.15))
                    .andExpect(jsonPath("$.customerName").value("Ravi"))
                    .andExpect(jsonPath("$.createdByName").value("Test CASHIER"))
                    .andExpect(jsonPath("$.replayed").value(false))
                    .andReturn();
            long billId = id(r);
            assertThat(stock(a)).isEqualTo(7);
            assertThat(stock(b)).isEqualTo(2);
            assertThat(q("SELECT COUNT(*) FROM stock_movements WHERE bill_id = ? AND reason = 'BILL_SALE'", billId)).isEqualTo(2);
            assertThat(q("SELECT SUM(-quantity_change) FROM stock_movements WHERE bill_id = ?", billId)).isEqualTo(6);
            assertThat(q("SELECT COUNT(*) FROM audit_log WHERE action = 'BILL_CREATED' AND entity_id = ?", billId)).isEqualTo(1);
            // history still adds up for each product
            assertThat(q("SELECT SUM(quantity_change) FROM stock_movements WHERE product_id = ?", a)).isEqualTo(7);
        }

        @Test
        @DisplayName("Validation: discount over 20%, too little stock, inactive product, unknown/inactive payment method, short payment – nothing saved")
        void validation() throws Exception {
            long p = product("BILL-V", "100.00", 4);
            long billsBefore = q("SELECT COUNT(*) FROM bills");
            create(bill(key(), line(p, 1), ",\"discountType\":\"PERCENT\",\"discountValue\":25"), manager)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("discountValue"));
            create(bill(key(), line(p, 2), ",\"discountType\":\"AMOUNT\",\"discountValue\":40.01"), manager)
                    .andExpect(status().isBadRequest());
            create(bill(key(), line(p, 5), ""), manager)
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.fieldErrors[0].message", containsString("only 4")));
            create(bill(key(), line(999999, 1), ""), manager).andExpect(status().isBadRequest());
            create(bill(key(), "", ""), manager).andExpect(status().isBadRequest());
            create(bill(key(), line(p, 1), "").replace("\"CASH\"", "\"BITCOIN\""), manager).andExpect(status().isBadRequest());
            create(bill(key(), line(p, 1), ",\"amountReceived\":50"), manager)
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("amountReceived"));
            create(bill("bad key!", line(p, 1), ""), manager).andExpect(status().isBadRequest());
            mvc.perform(json(patch("/api/admin/products/" + p), "{\"active\":false}", owner)).andExpect(status().isOk());
            create(bill(key(), line(p, 1), ""), manager).andExpect(status().isBadRequest());
            mvc.perform(json(patch("/api/admin/products/" + p), "{\"active\":true}", owner)).andExpect(status().isOk());

            long other = q("SELECT id FROM payment_methods WHERE code = 'OTHER'");
            mvc.perform(json(put("/api/admin/payment-methods/" + other),
                    "{\"code\":\"OTHER\",\"label\":\"Other (in-store)\",\"displayOrder\":4,\"active\":false}", owner)).andExpect(status().isOk());
            try {
                create(bill(key(), line(p, 1), "").replace("\"CASH\"", "\"OTHER\""), manager)
                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].message").value("is switched off"));
            } finally {
                mvc.perform(json(put("/api/admin/payment-methods/" + other),
                        "{\"code\":\"OTHER\",\"label\":\"Other (in-store)\",\"displayOrder\":4,\"active\":true}", owner)).andExpect(status().isOk());
            }
            assertThat(q("SELECT COUNT(*) FROM bills")).isEqualTo(billsBefore);
            assertThat(stock(p)).isEqualTo(4);
        }

        @Test
        @DisplayName("Submitting the same bill twice (double click / retry) returns the saved bill and deducts stock only once")
        void idempotent() throws Exception {
            long p = product("BILL-IDEM", "10.00", 10);
            String k = key();
            long first = id(create(bill(k, line(p, 3), ""), cashier).andExpect(status().isCreated()).andReturn());
            MvcResult again = create(bill(k, line(p, 3), ""), cashier)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true)).andReturn();
            assertThat(id(again)).isEqualTo(first);
            assertThat(stock(p)).isEqualTo(7);
            assertThat(q("SELECT COUNT(*) FROM bills WHERE request_key = ?", k)).isEqualTo(1);
            assertThat(q("SELECT COUNT(*) FROM stock_movements WHERE bill_id = ?", first)).isEqualTo(1);
        }

        @Test
        @DisplayName("10 counters billing the last 3 units at once: exactly 3 bills, stock 0, every bill number unique")
        void concurrent() throws Exception {
            long p = product("BILL-RACE", "20.00", 3);
            ExecutorService pool = Executors.newFixedThreadPool(6);
            List<Future<MvcResult>> results = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                String body = bill(key(), line(p, 1), "");
                results.add(pool.submit(() -> create(body, cashier).andReturn()));
            }
            int created = 0;
            int refused = 0;
            Set<String> numbers = new HashSet<>();
            for (Future<MvcResult> f : results) {
                MvcResult r = f.get();
                if (r.getResponse().getStatus() == 201) {
                    created++;
                    numbers.add(JsonPath.read(r.getResponse().getContentAsString(), "$.billNumber"));
                } else if (r.getResponse().getStatus() == 409) {
                    refused++;
                }
            }
            pool.shutdown();
            assertThat(created).isEqualTo(3);
            assertThat(refused).isEqualTo(7);
            assertThat(numbers).hasSize(3);
            assertThat(stock(p)).isZero();
        }
    }

    // ================================================================== after billing

    @Test
    @DisplayName("Void: items back to stock once, bill kept as VOIDED and logged; cannot void twice or pay a voided bill")
    void voiding() throws Exception {
        long p = product("BILL-VOID", "15.00", 6);
        long billId = id(create(bill(key(), line(p, 4), ""), cashier).andExpect(status().isCreated()).andReturn());
        assertThat(stock(p)).isEqualTo(2);
        mvc.perform(json(post("/api/admin/bills/" + billId + "/void"), "{\"reason\":\"\"}", manager)).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/admin/bills/" + billId + "/void"), "{\"reason\":\"Customer changed mind\"}", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOIDED"))
                .andExpect(jsonPath("$.voidedByName").value("Test MANAGER"));
        assertThat(stock(p)).isEqualTo(6);
        assertThat(q("SELECT COUNT(*) FROM stock_movements WHERE bill_id = ? AND reason = 'BILL_VOID'", billId)).isEqualTo(1);
        mvc.perform(json(post("/api/admin/bills/" + billId + "/void"), "{\"reason\":\"again\"}", owner)).andExpect(status().isConflict());
        assertThat(stock(p)).isEqualTo(6);
        mvc.perform(json(patch("/api/admin/bills/" + billId + "/payment"), "{\"paymentMethod\":\"CASH\"}", owner)).andExpect(status().isConflict());
        assertThat(q("SELECT COUNT(*) FROM bill_items WHERE bill_id = ?", billId)).isEqualTo(1); // never deleted
        assertThat(q("SELECT COUNT(*) FROM audit_log WHERE action = 'BILL_VOIDED' AND entity_id = ?", billId)).isEqualTo(1);
    }

    @Test
    @DisplayName("Pending bill paid later: status and method recorded once; amounts never change")
    void pendingPayment() throws Exception {
        long p = product("BILL-PEND", "40.00", 5);
        long billId = id(create(bill(key(), line(p, 2), "").replace("\"PAID\"", "\"PENDING\""), cashier)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.paymentStatus").value("PENDING")).andReturn());
        mvc.perform(json(patch("/api/admin/bills/" + billId + "/payment"), "{\"paymentMethod\":\"UPI\",\"amountReceived\":10}", cashier))
                .andExpect(status().isBadRequest());
        mvc.perform(json(patch("/api/admin/bills/" + billId + "/payment"), "{\"paymentMethod\":\"UPI\"}", cashier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod.code").value("UPI"))
                .andExpect(jsonPath("$.total").value(80.0));
        mvc.perform(json(patch("/api/admin/bills/" + billId + "/payment"), "{\"paymentMethod\":\"CASH\"}", cashier)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PDF invoice: real PDF with bill number, lines, totals and payment; no invented tax or GST lines")
    void invoicePdf() throws Exception {
        long p = product("BILL-PDF", "250.00", 5);
        MvcResult r = create(bill(key(), line(p, 2), ",\"discountType\":\"AMOUNT\",\"discountValue\":50"), cashier)
                .andExpect(status().isCreated()).andReturn();
        long billId = id(r);
        String number = JsonPath.read(r.getResponse().getContentAsString(), "$.billNumber");
        MvcResult pdf = mvc.perform(get("/api/admin/bills/" + billId + "/invoice.pdf").cookie(owner))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andReturn();
        byte[] bytes = pdf.getResponse().getContentAsByteArray();
        assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        PdfReader reader = new PdfReader(bytes);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();
        assertThat(text).contains(number, "Bill Test BILL-PDF", "BILL-PDF", "Rs. 500.00", "Rs. 50.00", "Rs. 450.00", "Cash", "Paid")
                .doesNotContainIgnoringCase("GST").doesNotContainIgnoringCase("tax");
        mvc.perform(get("/api/admin/bills/999999/invoice.pdf").cookie(owner)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Customer purchase history: their bills, total of finalized bills only (voided listed, not counted)")
    void customerHistory() throws Exception {
        long p = product("BILL-HIST", "100.00", 10);
        MvcResult c = mvc.perform(json(post("/api/admin/customers"), "{\"fullName\":\"Meena\",\"phone\":\"9876533333\"}", manager))
                .andExpect(status().isCreated()).andReturn();
        long cust = id(c);
        create(bill(key(), line(p, 2), ",\"customerId\":" + cust), cashier).andExpect(status().isCreated());
        long voided = id(create(bill(key(), line(p, 1), ",\"customerId\":" + cust), cashier).andExpect(status().isCreated()).andReturn());
        mvc.perform(json(post("/api/admin/bills/" + voided + "/void"), "{\"reason\":\"mistake\"}", owner)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/customers/" + cust + "/bills").cookie(cashier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.finalizedCount").value(1))
                .andExpect(jsonPath("$.finalizedTotal").value(200.0))
                .andExpect(jsonPath("$.voidedCount").value(1));
        mvc.perform(get("/api/admin/bills").param("search", "Meena").cookie(manager)).andExpect(jsonPath("$.page.totalElements").value(2));
        mvc.perform(get("/api/admin/bills").param("status", "VOIDED").param("search", "Meena").cookie(manager))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @DisplayName("Reports and dashboard: revenue equals finalized bills in the database; CSV and PDF exports; no profit figure")
    void reportsAndDashboard() throws Exception {
        long p = product("BILL-REP", "60.00", 20);
        create(bill(key(), line(p, 3), ""), cashier).andExpect(status().isCreated());
        long v = id(create(bill(key(), line(p, 1), ""), cashier).andExpect(status().isCreated()).andReturn());
        mvc.perform(json(post("/api/admin/bills/" + v + "/void"), "{\"reason\":\"test\"}", owner)).andExpect(status().isOk());
        LocalDate today = ShopTime.today(clock);
        java.time.LocalDateTime start = java.time.LocalDateTime.ofInstant(ShopTime.startOf(today), java.time.ZoneOffset.UTC);
        BigDecimal revenue = money("SELECT COALESCE(SUM(total),0) FROM bills WHERE status='FINALIZED' AND created_at >= ?", start);

        String rep = mvc.perform(get("/api/admin/reports/billing").param("from", today.toString()).param("to", today.toString()).cookie(manager))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profit").doesNotExist()).andReturn().getResponse().getContentAsString();
        assertThat(new BigDecimal(JsonPath.read(rep, "$.revenue").toString())).isEqualByComparingTo(revenue);
        assertThat(((Number) JsonPath.read(rep, "$.voidedBills")).longValue()).isGreaterThanOrEqualTo(1);

        mvc.perform(get("/api/admin/reports/product-sales").param("from", today.toString()).param("to", today.toString())
                        .param("search", "BILL-REP").cookie(owner))
                .andExpect(jsonPath("$[0].unitsSold").value(3))
                .andExpect(jsonPath("$[0].revenue").value(180.0));
        String summary = mvc.perform(get("/api/admin/reports/summary").param("from", today.toString()).param("to", today.toString()).cookie(owner))
                .andReturn().getResponse().getContentAsString();
        assertThat(new BigDecimal(JsonPath.read(summary, "$.totals.revenue").toString())).isEqualByComparingTo(revenue);

        for (String path : List.of("billing", "product-sales", "summary", "expenses", "suppliers", "low-stock", "stock-movements")) {
            mvc.perform(get("/api/admin/reports/" + path).param("format", "pdf").cookie(owner))
                    .andExpect(status().isOk()).andExpect(header().string("Content-Type", "application/pdf"));
            mvc.perform(get("/api/admin/reports/" + path).param("format", "csv").cookie(owner))
                    .andExpect(status().isOk()).andExpect(header().string("Content-Type", containsString("text/csv")));
        }
        mvc.perform(get("/api/admin/reports/billing").cookie(cashier)).andExpect(status().isForbidden());

        String dash = mvc.perform(get("/api/admin/dashboard").cookie(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.recentBills.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.recentMovements.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.totalSuppliers").exists())
                .andReturn().getResponse().getContentAsString();
        assertThat(new BigDecimal(JsonPath.read(dash, "$.revenueToday").toString())).isEqualByComparingTo(revenue);
    }
}
