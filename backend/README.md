# SIVA Crackers – Backend (Spring Boot)

REST API for the SIVA Crackers information catalogue.

> The website is an information catalogue only. This backend has **no** checkout, online payment, online ordering, booking or reservation features.

**Status:** Phases 4–6 – public read-only catalogue API, customer/staff login (HttpOnly cookie sessions, CSRF, lockout, OWNER bootstrap), role-based admin API (catalogue, uploads, staff, stock, suppliers, expenses, shop customers, in-store billing with PDF invoices, reports with CSV/PDF, activity log). **Full endpoint reference: [`../docs/API.md`](../docs/API.md)**; Postman collection: [`../docs/postman/`](../docs/postman/).

## Tech stack

| | |
|---|---|
| Language / build | Java 21 target (runs on Java 21+), Maven 3.9+ |
| Framework | Spring Boot 4.1.3 (Spring Framework 7, Spring Security 7, Hibernate 7) |
| Database | MySQL 8, schema managed by Flyway |
| Auth (from 4.5) | JWT in an **HttpOnly, Secure, SameSite=Strict** cookie + CSRF protection; BCrypt (cost 12) passwords |
| Docs | OpenAPI / Swagger UI (dev profile only) |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc, Spring Security Test |

## Project structure

```
backend/
├── pom.xml
├── .env.example               # names of all settings (no values)
├── scripts/
│   ├── setup-database.sql     # one-time: databases + least-privilege MySQL user
│   ├── run-dev.ps1            # run locally with settings from backend/.env
│   ├── run-integration-tests.ps1  # all tests incl. MySQL (siva_crackers_test only)
│   ├── remove-demo-data.sql   # deletes is_demo rows only
│   └── lib/DotEnv.ps1         # shared .env loader (names printed, values never)
└── src/
    ├── main/java/com/sivacrackers/
    │   ├── SivaCrackersApplication.java
    │   ├── config/            # AppProperties (validated settings), SecurityConfig, JpaConfig, RequiredSettingsCheck
    │   ├── security/          # JSON 401/403 handlers, ApiErrorWriter
    │   ├── catalog/           # category/, brand/, product/ – entities + repositories
    │   ├── settings/          # BusinessSettings, BusinessHighlight, Banner + repositories
    │   ├── user/              # User, Role, UserRepository
    │   └── common/
    │       ├── entity/        # AuditedEntity (timestamps), BaseEntity (id)
    │       ├── dto/           # ApiError, FieldErrorDto
    │       ├── exception/     # ApiException + NotFound/Conflict/BadRequest, GlobalExceptionHandler
    │       └── web/           # HealthController
    ├── main/resources/
    │   ├── application.yml        # all secrets read from environment variables
    │   ├── application-dev.yml    # Swagger UI, debug logging
    │   └── db/migration/          # V1__core_schema.sql, V2__business_defaults.sql
    └── test/
        ├── java/.../web, config   # unit + web-layer tests (no database)
        └── java/.../it            # MySQL integration tests (*IT) + _test safety guard
```

## Setup (Windows, one time)

### 1. Prerequisites
Java 21 or newer, Maven 3.9+, MySQL 8 running locally.

### 2. Create the databases and the `siva_app` user
```powershell
mysql -u root -p
```
Then paste the statements from [`scripts/setup-database.sql`](scripts/setup-database.sql), typing **your own** password in place of `<choose-a-strong-password>`. Do not save the real password in the file.

### 3. Generate a JWT secret (cryptographically random, 48 bytes)
```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

### 4. Configure
Copy `.env.example` to `.env` (git-ignored) in the `backend` folder and fill in `DB_PASSWORD` and `JWT_SECRET` with your own values. Alternatively set them as environment variables in your shell or hosting platform.

### 5. Run
```powershell
.\scripts\run-dev.ps1
```
Check: <http://localhost:8080/api/health> → `{"status":"UP"}`. In the dev profile, Swagger UI is at <http://localhost:8080/swagger-ui.html>.

## Configuration

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `DB_URL` | yes | – | `jdbc:mysql://localhost:3306/siva_crackers` |
| `DB_USERNAME` | yes | – | `siva_app` |
| `DB_PASSWORD` | yes | – | your MySQL password |
| `JWT_SECRET` | yes | – | random, at least 32 bytes |
| `JWT_EXPIRY_MINUTES` | no | `60` | login session length (5–1440) |
| `PORT` | no | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | no | – | `dev` enables Swagger UI and debug logs |
| `APP_COOKIE_SECURE` | no | `true` | keep `true` outside local development |
| `APP_COOKIE_SAME_SITE` | no | `Strict` | `Strict` or `Lax` |
| `CORS_ALLOWED_ORIGINS` | no | *(empty)* | exact origins for a separately hosted frontend; no wildcards |
| `APP_SEED_DEMO` | no | `false` | load clearly labelled demo catalogue (from 4.3) |
| `BOOTSTRAP_OWNER_EMAIL` / `BOOTSTRAP_OWNER_PASSWORD` | no | – | one-time first OWNER account (from 4.5) |
| `APP_UPLOAD_DIR` | no | `uploads` | folder for images uploaded in the admin (from Phase 5); git-ignored; use a persistent, backed-up folder on a server |
| `TEST_DB_URL` / `TEST_DB_USERNAME` / `TEST_DB_PASSWORD` | tests | – | MySQL integration tests |

The application **refuses to start** with a clear list of problems if a required variable is missing, if `JWT_SECRET` is shorter than 32 bytes, or if `DB_URL` is not a MySQL JDBC URL. Messages name the variable but never print its value.

## Security baseline (Phase 4.1)

- **Stateless** API; no server sessions, no `JSESSIONID`.
- **CSRF protection on** for every state-changing request: readable `XSRF-TOKEN` cookie (Secure, SameSite=Strict, path `/`) echoed in the `X-XSRF-TOKEN` header (Axios does this automatically for same-origin requests).
- **Route rules:** `GET /api/health` and `GET /api/public/**` are public; `/api/auth/**` is open (CSRF still applies to POST); `/api/admin/**` requires a staff role (OWNER, MANAGER, CASHIER) with finer checks per endpoint; any other `/api/**` needs login; everything outside `/api` is denied.
- **JSON** 401/403 responses (no login page, no HTTP Basic pop-up).
- **CORS** off unless `CORS_ALLOWED_ORIGINS` lists exact origins; credentials allowed only for those.
- **Headers:** `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`.
- **No secrets in logs or responses:** settings objects mask secrets in `toString()`, SQL bind values are never logged, error responses never contain stack traces or internal messages, rejected input values are never echoed back, and Spring's default generated password user is disabled.

## First OWNER account

There is no default admin account. To create the first OWNER, add both variables to `backend/.env` (choose your own values; never share them):

```
BOOTSTRAP_OWNER_EMAIL=
BOOTSTRAP_OWNER_PASSWORD=
```

Start the backend once. The OWNER is created only if no OWNER exists yet (password: 8-72 characters, at least one letter and one digit). Log in through the staff login, then **delete both lines** from `.env`. Later starts ignore them and never change an existing account. MANAGER and CASHIER accounts are created by the OWNER (`POST /api/admin/staff`); public sign-up only ever creates CUSTOMER accounts.

## Admin dashboard (Phase 5)

Staff use **http://localhost:5173/admin** (website and admin share one origin; Vite forwards `/api` and `/uploads` to this backend). Log in with the OWNER account created above; the OWNER then creates MANAGER and CASHIER accounts under **Staff**. Role table (enforced by URL rules before the request body is read, and again with `@PreAuthorize`):

| Role | Can use |
|---|---|
| OWNER | everything, incl. staff, activity log, homepage slides, business settings |
| MANAGER | dashboard, products, categories, brands, uploads, stock, suppliers, expenses, shop customers, reports; reads settings |
| CASHIER | in-store billing (create bills, history, invoices, mark paid – not void), stock and stock history (view only), customer look-up and add |

Voiding bills: OWNER and MANAGER. Payment methods: listed for all staff, changed by the OWNER only.

Website customer accounts are refused. Endpoints: [`../docs/API.md`](../docs/API.md).

Uploaded images are stored in `APP_UPLOAD_DIR` (default `backend/uploads/`) under random names, with location/camera metadata removed, and served read-only at `/uploads/{products|brands|categories}/…`. Back this folder up together with the database.

## Demo data

`APP_SEED_DEMO=true` loads the clearly labelled demonstration catalogue (12 categories, 8 fictional brands, 48 products, all `is_demo = 1`) at startup. It is **off by default**, safe to leave on across restarts (inserts only what is missing) and never changes or deletes existing rows. To remove it later, set `APP_SEED_DEMO=false` and run `scripts/remove-demo-data.sql` after a backup.

## Error format

Every error uses the same JSON:
```json
{
  "timestamp": "2026-11-23T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/public/products",
  "fieldErrors": [{ "field": "size", "message": "must be one of 12, 24, 48" }]
}
```

## Build and test

```powershell
mvn clean package                      # compile, run unit + web-layer tests, build the jar
mvn test                               # unit + web tests only (no database needed)
.\scripts\run-integration-tests.ps1    # + MySQL integration tests on siva_crackers_test
```

`run-integration-tests.ps1` loads only `TEST_DB_URL`, `TEST_DB_USERNAME` and `TEST_DB_PASSWORD` from `backend/.env`. The tests **rebuild their database from scratch**, so they refuse to run unless the URL points to a localhost database whose name ends in `_test`, and check again on the live connection before cleaning anything. The development database is never touched.

### Troubleshooting: `Access denied for user 'siva_app'@'localhost'`
The password in `backend/.env` does not match the MySQL account. In MySQL Workbench (as root) set it again with `ALTER USER 'siva_app'@'localhost' IDENTIFIED BY '…';` and paste the **same** value into both `DB_PASSWORD` and `TEST_DB_PASSWORD` in `backend/.env`. Never share the password in chat or commit it.

### Troubleshooting: `PKIX path building failed` when Maven downloads
Some antivirus products (for example Avast "Web Shield") scan HTTPS by re-signing certificates with their own root certificate. Windows trusts it, Java does not. Tell Maven to use the Windows certificate store (certificate checking stays on):
```powershell
$env:MAVEN_OPTS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT'
```
`scripts/run-dev.ps1` sets this automatically.
