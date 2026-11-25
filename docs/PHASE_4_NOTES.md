# Phase 4 – Progress notes

Plan and approved decisions: [PHASE_4_PLAN.md](PHASE_4_PLAN.md). Each step stops for approval.

## Step 4.7 – Testing and regression (25 Nov 2026) – done

### Results
| Check | Result |
|---|---|
| Backend: unit + web + MySQL integration (`.\scripts\run-integration-tests.ps1`) | BUILD SUCCESS, **132 / 132** (49 unit/web, 83 MySQL) |
| Frontend `npm run lint` / `npm run build` | pass, no warnings |
| Browser regression, desktop 1440 px (`frontend/scripts/regression-check.js`) | **899 / 899** – 11 static pages, 3 legal pages, 12 categories, 8 brands, all 48 products (name, price, brand, category vs API), catalogue count, category filter + price sort order, search, Price List completeness, unknown product / unknown page / protected route, all internal links resolve; on every page: h1, title, no error state, no overflow, no broken images, demo notice matches `demoMode`, no cart/checkout/buy/pay/book controls |
| Browser regression, phone 375 px (full) | **898 / 899** first run – `/brands` overflowed by 10 px (see fix) – then 250 / 250 quick re-run at 375 px and **250 / 250 at 320 px** after the fix |
| Console (fresh tab, two quick runs) | only the 8 intended `404`s from the unknown-product check; no other errors |
| Mock mode (`VITE_USE_MOCKS=true`) | catalogue from sample data (48 products, same as the API demo seed), no `/api` calls, demo notice shown, Login shows "Accounts are not available…" and is disabled |
| Account flows (from 4.6) | register, login by email/mobile, profile, password change, session ended, logout – all verified in the browser |
| Secrets | 257 committable files: none contains any password or JWT secret value from `backend/.env`; `backend/.env`, `frontend/.env`, `target/`, `dist/`, `node_modules/`, `.claude/settings.local.json` git-ignored; only JWT/BCrypt-shaped string is a fake test fixture |
| No online selling | code search for cart / checkout / payment SDKs / order / booking / reservation identifiers: only comments stating these do not exist |
| Token storage | website code reads/writes browser storage only in `utils/storage.js` (Estimate List); no `Authorization`/Bearer/token code |
| Invented details | no phone, email or pincode values in migrations or demo data |

### Issue found and fixed
- **Brands page overflowed on 360 px phones** (pre-existing since Phase 2): the single-column grid sized itself to the longest brand name and the name could not truncate. Fixed with `grid-cols-1` and `min-w-0` (`pages/BrandsPage.jsx`); verified at 375 and 320 px.

### Still open (owner actions, not code)
- Copy the working `siva_app` password into `DB_PASSWORD` in `backend/.env` so the development database `siva_crackers` can be migrated and used (tests use `siva_crackers_test`, which works).
- Change the MySQL root and `siva_app` passwords that were shared in chat earlier.
- Create the first OWNER with `BOOTSTRAP_OWNER_EMAIL` / `BOOTSTRAP_OWNER_PASSWORD` when ready (backend README), then remove both lines.
- Before going live: real business details, legal review of the policy pages, HTTPS hosting with `APP_COOKIE_SECURE=true` (default).

## Step 4.6 – Frontend integration (25 Nov 2026) – approved

Reference: [`PHASE_4_IMPLEMENTATION.md` §5](PHASE_4_IMPLEMENTATION.md).

### Delivered
- `apiClient.js`: cookies + CSRF, no token storage; `vite.config.js` dev proxy; `VITE_USE_MOCKS=false` default; mock data lazy-loaded.
- `AuthProvider` / `useAuth`, `ProtectedRoute`, `FormField`, `authValidation.js`; pages Login, Register, My Account; header account link; `useDemoMode` (demo notice driven by the backend).
- Backend: `GET /api/auth/session` (always 200) so visitors' page loads do not log 401 errors.
- Privacy Policy: factual "Customer accounts" section.

### Checks
| Check | Result |
|---|---|
| `npm run lint` / `npm run build` | pass, no warnings; mock sample data only in a separate, never-loaded chunk |
| Backend `.\scripts\run-integration-tests.ps1` | BUILD SUCCESS, **132 / 132** (new: `/session` for visitor, user and tampered cookie) |
| Browser (Vite 5173 → backend 8081 on `siva_crackers_test` with demo data) | catalogue, product detail, price range all via `/api` (200); demo notice from `demoMode`; `/account` while logged out → `/login`; register: client errors, success → My Account, header "Hi, <name>"; profile save (mobile normalised); wrong current password → field error; session ended elsewhere → next action redirects to login with message; login by mobile → back to My Account; wrong password → generic message, password field cleared; log out → home, `/me` 401; Estimate List still browser-only (only read-only price lookup); 375 px wide: no horizontal scroll; visitor console: no errors |
| Token storage | page scripts can read only `XSRF-TOKEN`; localStorage holds only the Estimate List; sessionStorage empty |

### Issues found and fixed
- Logging out from My Account landed on `/login` (the login guard reacted first); now goes to the home page.
- Form banner showed "Validation failed"; now "Please correct the highlighted fields." with the field messages.
- Every visitor page load logged a 401 from `/api/auth/me`; the website now uses `/api/auth/session`.

### Note
- Browser checks used the **test** database because the development database is still not migrated (`DB_PASSWORD` in `backend/.env` does not match). Once `DB_PASSWORD` is fixed, the `backend` + `frontend` launch configurations run the same setup against `siva_crackers`.

## Step 4.5 – Authentication and authorization (25 Nov 2026) – approved

Reference: [`PHASE_4_IMPLEMENTATION.md` §4](PHASE_4_IMPLEMENTATION.md).

### Delivered
- V3 migration (`users.token_version`), `JwtService`, `AuthCookies`, `JwtAuthenticationFilter`, `LoginAttemptService`, `PasswordPolicy`, `Identifiers`.
- `AuthController` (csrf, register, login, staff login, logout, me), `AccountController` (profile, password), `AdminStaffController` (OWNER creates MANAGER/CASHIER), `AdminSettingsController` (OWNER/MANAGER read, OWNER write), `OwnerBootstrap`.

### Test results – `.\scripts\run-integration-tests.ps1`: BUILD SUCCESS, **131 / 131**
| Suite | Tests | Covers |
|---|---|---|
| `JwtServiceTest` | 5 | round trip; expiry after 60 min; tampered role; other secret; `alg:none` and garbage |
| `LoginAttemptServiceTest` | 6 | lock on 5th failure; unlock after 15 min; success resets; old failures forgotten; per account; per-IP limits |
| `PasswordAndIdentifierTest` | 3 | password rules (incl. 72-byte limit), message never contains the password, email/phone normalisation |
| `AuthApiIT` (MySQL) | 15 | register (cookie attributes, CUSTOMER only even if `role:OWNER` is sent, BCrypt hash, no token in body); duplicates and validation; CSRF required; login by email or phone; same 401 for unknown/wrong; lockout 429 + Retry-After 900; separate customer/staff logins; inactive users; /me; tampered cookie cleared; logout; profile; password change ends old sessions; role matrix on admin settings (settings restored afterwards); staff provisioning; owner bootstrap (once, never overwrites, nothing logged) |
| `CsrfFlowIT` (MySQL) | 1 | the real browser CSRF flow without test shortcuts, in its own application context |
| Earlier suites | 101 | unchanged and passing (`SchemaMigrationIT` now expects V1-V3) |

Live server check (packaged jar against `siva_crackers_test`, port 8082, no dev profile): `Set-Cookie` headers exactly as in §4.1; CUSTOMER gets 403 on `/api/admin/settings`. Server log and test output searched for the test passwords, JWTs, BCrypt hashes and test emails: **none found**.

### Issues found and fixed
- **Role denials from `@PreAuthorize` returned 500** (they reached the catch-all error handler). They now return 403 "You do not have permission to do this.", the same as URL-level checks.
- `Retry-After` could say 899 for a 15-minute lock; seconds are now rounded up.
- Spring Security's test helper `csrf()` replaces the CSRF store inside a shared test context, so the real-flow test runs in its own context.

### Not yet verified
- Development database `siva_crackers` is still not migrated: `DB_PASSWORD` in `backend/.env` does not match the `siva_app` password (only `TEST_DB_PASSWORD` does).

## Step 4.4 – Public APIs (25 Nov 2026) – approved

Reference: [PHASE_4_IMPLEMENTATION.md](PHASE_4_IMPLEMENTATION.md) §3.

### Delivered
- The 12 endpoints the frontend already calls, with identical response shapes, plus `categories/{slug}` (with safety information), `brands/{slug}`, `settings/contact` and `settings/safety-tips`.
- `ProductSpecifications` (search, category/brand slugs, price, stock; LIKE wildcards escaped), `ProductSort` (the 6 frontend sort values, stable tie-breakers), `ProductQuery` (page/size/sort/price validation), DTO records and mappers, read-only services.
- One visibility rule everywhere (product active + visible, category and brand active); `demoMode` in settings derived from the data.
- `InvalidRequestException` → 400 with `fieldErrors`; `PageResponse`.

### Test results – `.\scripts\run-integration-tests.ps1`: BUILD SUCCESS, **101 / 101**
| Suite | Result | Covers |
|---|---|---|
| Unit + web | 35 / 35 | unchanged |
| `PublicApiIT` (new, full application on MySQL with the demo catalogue) | 25 / 25 | default page shape and values; no internal fields; search by product/brand/category/multi-word/upper-case; empty result and literal `%`, `_`, `100%`; category, brand, combined and price/stock filters; all sorts; pagination incl. beyond the end and no overlap between pages; 8 validation cases (size, sort, page, negative/inverted/non-numeric price, bad slug, long search); featured (+ limit validation); new arrivals; detail with safety notes and 404; related (excludes self, same category first, then brand); by-slugs (order, unknown skipped, > 200 rejected); hidden product and inactive brand disappear everywhere; price list (12 groups, 48 unique); price range ₹30–₹3,000; categories (12, 4 each, detail, 404); brands (8 unique, counts, detail, 404); settings shape with all unknown details null and `demoMode` true, and false when no demo product is visible; contact, safety tips, banners; POST to public → 401; admin/account still 401 |
| 4.2/4.3 integration suites | 41 / 41 | unchanged; ran after the API tests, confirming they clean up |

No real secret appears in the test output.

### Issue found and fixed
- Two API tests double-encoded spaces/`%` in hand-written URLs; they now pass parameters properly (the API itself was correct).

---

## Step 4.3 – Demo data and seeding (25 Nov 2026) – approved

Reference: [PHASE_4_IMPLEMENTATION.md](PHASE_4_IMPLEMENTATION.md) §2.

### Delivered
- `demo/DemoDataSeeder` – idempotent, insert-only seeding of 12 categories (with 36 safety notes), 8 brands, 48 products, all `is_demo = 1`; original product dates kept.
- `demo/DemoDataRunner` – runs the seeder at startup only when `APP_SEED_DEMO=true` (default false).
- `resources/demo/*.json` – copy of the approved Phase 1–3 sample data (validated: 48 products, all flagged demo, no price above MRP, no duplicate slugs/codes).
- `scripts/remove-demo-data.sql` – removes demo rows only.

### Test results – `.\scripts\run-integration-tests.ps1`: BUILD SUCCESS, **76 / 76**
| Suite | Result | Covers |
|---|---|---|
| Unit + web | 35 / 35 | 32 from 4.1 + `DemoDataRunnerTest` (3): runner absent by default and with `false`; present with `true` and calls the seeder |
| `DemoDataSeederIT` (new) | 6 / 6 | 12/8/48 inserted, all demo, 36 safety notes; second run inserts nothing, no duplicates; real product with a demo slug left untouched (name, price, real status) and a real category reused without being flagged demo; price ≤ MRP, 3 out-of-stock / 4 low / 41 in stock; New Arrivals order preserved; settings and banners untouched |
| 4.2 integration suites | 35 / 35 | unchanged |

### Not yet verified
- Seeding the **development** database (needs the `DB_PASSWORD` value fix from 4.2 and `APP_SEED_DEMO=true`).

---

## Step 4.2 – Database design and integration (25 Nov 2026) – approved

Reference: [PHASE_4_IMPLEMENTATION.md](PHASE_4_IMPLEMENTATION.md) §1.

### MySQL connection diagnosis
| Check | Result |
|---|---|
| `siva_app` exists / host | `siva_app@localhost` |
| Privileges | `SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER` on `siva_crackers.*` and `siva_crackers_test.*`; `USAGE` (nothing) globally |
| Server | MySQL 8.0.42, `utf8mb4` |
| `.env` file | UTF-8, one active `DB_PASSWORD` line, no quotes/spaces/hidden characters |
| Login with `TEST_DB_PASSWORD` | **Works** on both databases |
| Login with `DB_PASSWORD` | **Fails (1045)** – `.env` still holds the old password in `DB_PASSWORD`; the new one is only in `TEST_DB_PASSWORD`. Owner action: copy the value into `DB_PASSWORD` |

### Delivered
- `V1__core_schema.sql` (10 tables, constraints, indexes) and `V2__business_defaults.sql` (settings row with known facts only, highlights, safety tips, banners).
- Entities and repositories for all tables; audit timestamps; calculated stock status.
- Integration-test infrastructure with two `_test` safety checks; `scripts/run-integration-tests.ps1`; shared `.env` loader `scripts/lib/DotEnv.ps1` (now also used by `run-dev.ps1`).

### Test results – `.\scripts\run-integration-tests.ps1`: BUILD SUCCESS, **67 / 67**
| Suite | Result | Covers |
|---|---|---|
| Unit + web (Phase 4.1) | 32 / 32 | unchanged |
| `SchemaMigrationIT` | 12 / 12 | connected to `_test`; V1+V2 applied in order; exactly the 10 approved tables; no order/cart/payment/booking/reservation tables or columns; InnoDB + utf8mb4; key constraints and product indexes exist; settings defaults with phone/WhatsApp/email/hours/street/pincode/licence all empty; 3 non-demo banners; second settings row rejected; external banner links rejected; unknown banner theme rejected |
| `CatalogRepositoryIT` | 17 / 17 | save + find by slug with category and brand; calculated stock status; duplicate slug / SKU / category slug / brand name (case-insensitive) rejected; negative price, MRP, stock rejected; price above MRP rejected, equal allowed; bad slug and unknown unit rejected; category/brand with products cannot be deleted; images cascade-deleted and ordered; safety notes ordered; timestamps auto-filled and updated; visible-only counts and price range; lookup by slugs; active categories in display order |
| `UserRepositoryIT` | 6 / 6 | BCrypt hash stored and verifiable; email lookup ignores case; `toString` hides hash and email; unique email (case-insensitive) and phone; 10-digit phone; plain-text password rejected by the database; unknown role rejected; `existsByRole` |

Hibernate schema validation passed (every entity matches the migrated tables). Test output was checked: no real password or secret appears (the only password-like text is the deliberate fake value in the 4.1 error-handling test).

### Issues found and fixed
1. Tests inherited the developer's `.env` app settings (Spring maps `APP_COOKIE_SECURE` onto `app.cookie.secure`), which broke the Secure-cookie test. The integration-test script now loads only the three `TEST_DB_*` variables.
2. Spring classifies MySQL CHECK error 3819 from plain JDBC as "uncategorized"; the constraints worked, so tests now assert the exact constraint name instead of the exception class.

### Not yet verified
- Startup against the **development** database `siva_crackers` (migrations V1/V2 there) – blocked only by the `DB_PASSWORD` value above.

---

## Step 4.1 – Project setup (23 Nov 2026) – approved

### Delivered
- `backend/` Maven project: Spring Boot **4.1.3**, Java **21** target (built and tested on Java 25).
- Feature-based package structure (`config`, `security`, `common`), ready for `catalog`, `settings` and `user` in later steps.
- Configuration entirely from environment variables; no secrets in files. Startup check that names every missing/invalid setting and refuses to start (values never printed), before any database connection.
- Security baseline: stateless, CSRF on (Secure + SameSite=Strict `XSRF-TOKEN` cookie, `X-XSRF-TOKEN` header), JSON 401/403, route rules for public / auth / account / admin, CORS only for listed exact origins, security headers, BCrypt(12) encoder, Spring's default generated-password user disabled.
- One JSON error format via `GlobalExceptionHandler` (400 validation, 404, 405, 415, 500 generic).
- `GET /api/health`.
- Setup docs: `backend/README.md`, `.env.example`, `scripts/setup-database.sql` (least-privilege `siva_app` user), `scripts/run-dev.ps1`.

### Not yet (by plan)
Database tables (4.2), demo seed (4.3), catalogue APIs (4.4), login/JWT cookie/bootstrap/rate limiting (4.5), integration tests and API docs (4.6), frontend switch (4.7). The app cannot fully start until the `siva_app` database user exists and its variables are set.

### Test results
| Check | Result |
|---|---|
| `mvn clean package` | BUILD SUCCESS, **31 / 31 tests pass**, no warnings |
| `AppPropertiesTest` (5) | missing/short JWT secret rejected; secret and owner credentials masked in `toString()`; CORS wildcard rejected |
| `RequiredSettingsCheckTest` (4) | all missing variables listed; short secret; non-MySQL URL; values never in messages |
| `WebLayerSecurityTest` (22) | health public, no session cookie; 404/400/405/500 in ApiError format; 500 hides internal details; rejected input not echoed; CSRF required on POST (403 JSON) and accepted with token; XSRF-TOKEN cookie Secure, SameSite=Strict, path `/`, readable by the SPA; account area 401; admin area 401 anonymous, 403 CUSTOMER, allowed OWNER; non-API paths denied; public POST needs login; CORS allowed origin with credentials, other origins rejected; security headers |
| Real jar, no env vars | exits with a clear list of the 4 missing variables |
| Real jar, short `JWT_SECRET` | exits with "JWT_SECRET is too short"; secret not printed; no DB connection attempted |
| Real jar, PostgreSQL URL | exits with "DB_URL must start with jdbc:mysql://" |

### Pre-4.2 checks (approved 4.1; 23 Nov 2026)
| Check | Result |
|---|---|
| `.env` loading | `run-dev.ps1` sets each `KEY=VALUE` as an environment variable of its own PowerShell process only, supports quoted values, prints names only, accepts `-EnvFile`, and restores/removes every variable it set when the backend stops. Fixed: Maven's harmless JDK warning on stderr no longer aborts the script |
| Spring receives the values | Proven with a **dummy** env file (fake values, dummy port): startup check passed, `dev` profile active, connection attempted to the dummy `DB_URL`; afterwards no variables left in the shell |
| Secrets in logs | Dummy password, JWT secret, username and URL markers searched in the complete 194-line dev-profile log: **none found** |
| Production cookies | New guard: the app refuses to start with `APP_COOKIE_SECURE=false` unless the `dev` profile is active (test added; **32 / 32 tests pass**) |
| Git ignore | Each sensitive path matched by a specific rule: `backend/.env`, `backend/.env.*` (except `.env.example`), `backend/target/`, logs, `frontend/.env*`, `frontend/dist`, `node_modules`, `.claude/settings.local.json`, IDE folders, uploads. Secret scan of all 140 committable files: only clearly fake test values |

### Notes
- **Maven + Avast:** Avast's HTTPS scanning re-signs certificates, which Java rejects. Builds use `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` (Windows certificate store; checks stay on). Documented in the backend README; `run-dev.ps1` sets it.
- **SameSite in MockMvc:** the CSRF cookie's `SameSite=Strict` is verified on the cookie object; MockMvc's header text omits custom attributes, so the real `Set-Cookie` header will be checked against the running server in step 4.5.
