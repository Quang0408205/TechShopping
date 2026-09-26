# CLAUDE_CONTEXT — TechShopping handoff

Last updated: 2026-09-25, **end of session 2**. The user stopped the session after Checkpoint 2.7 and asked for a final review of this file. **Open work is listed in [`docs/PENDING_WORK.md`](PENDING_WORK.md); read it first.**
Session 2 summary:
- Phase 2 backend 2.1–2.5b was built and committed as `c9e2d6c`;
- frontend 2.6 (auth), the frontend restructure (role folders, partials, CORS +5501) and 2.7 (account page) are done but **not committed**;
- 2.8 has not started.
Previous updates: 2026-09-25 (Checkpoint 2.5b: admin user management; Checkpoint 2.5a: `/users/me` + per-user refresh index; Checkpoint 2.4: authorization D4 + Swagger bearer; Checkpoint 2.3: Auth API; Checkpoint 2.2: JWT + Redis refresh tokens + CORS; Checkpoint 2.1; Phase 2 decisions; frontend analysis; inspection report); 2026-09-24 (end of Phase 1 / Product module).
Everything here was checked against the code, git and the running database at the time of writing.
If this file contradicts the code, **the code wins**. Update this file afterwards.

---

# 1. Project Overview

- **Name:** TechShopping
- **Purpose:** backend for "Xây dựng hệ thống quản lý và khuyến nghị mua sắm thiết bị công nghệ tại Hệ thống Thế Giới Di Động". It is a REST API for product management, cart/order/payment, warranty, stores, recommendations and a chatbot. It is being built phase by phase.
- **Stack:**
  - Java 21. `JAVA_HOME` = JDK 21.0.12. Note that the `java` on PATH is 17; `mvnw` uses `JAVA_HOME`.
  - Spring Boot **4.0.8**, which brings Hibernate 7.2, Spring Data JPA 4.1 and Jackson 3.
  - Maven (always run `mvnw` / `mvnw.cmd`; there is no global `mvn`).
  - Spring Web MVC, Spring Data JPA, Spring Validation, Spring Security **7.0.7**, Spring Security OAuth2 resource server (Nimbus JOSE JWT 10.4), Spring Data Redis, Lombok.
  - springdoc-openapi **3.0.3**, the line that supports Boot 4.0.x (3.1.x targets Boot 4.1).
  - PostgreSQL 16 and Redis 7, both running in Docker.
- **Repository layout:**
  - `backend/Tech/`: Spring Boot project. groupId `com.example`, artifactId `Tech`, base package `com.example.Tech`.
  - `database/techshopping.sql`: **SQL schema, source of truth.**
  - `database/docker-init/02_create_test_db.sh`: creates `techshopping_test` and applies the same schema to it.
  - `docker-compose.yml`: postgres + redis.
  - `Raw_data/`: Python crawler and TGDD CSVs (`tgdd_products_cleaned.csv`, 877 rows).
  - `AI_Service/`: empty (`.gitkeep` only, containing 4 spaces; committed that way by the user in `21d2cda`).
  - `frontend/`: static HTML/CSS/JS customer site "LAHY", originally written by teammate **HoangPhuoc38** (commit `f58f92d`). It is part of the project and must be **developed further, not rewritten**: no framework, keep its look, and change the frontend to match the backend API, not the other way round.
    - Since **2.6**, login, register and the header login state call the real Auth API.
    - Cart, recommendation and contact are still simulated on the client; products stay hardcoded until 2.8.
    - The original analysis (A–H) is in §10.3 (it uses the old flat paths).
  - **Frontend structure** (restructured 2026-09-25 at the user's request: folders by role, JS/CSS mirrored, shared header/footer):
    ```
    frontend/
    ├── index.html                 home page (kept at the root so Live Server opens it)
    ├── auth/                      login.html, register.html (no header/footer: centred .login-box)
    ├── customer/                  products, cart, recommendation, services, contact, account (2.7) .html
    ├── admin/                     empty (.gitkeep), for STAFF/ADMIN pages later
    ├── partials/                  header.html, footer.html (shared, see below)
    ├── css/  style.css (shared), home.css, auth/login.css, customer/{products,cart,recommendation,contact,services,account}.css
    ├── js/   core/{api.js, layout.js, main.js}, home.js, auth/{login,register}.js,
    │         customer/{products,cart,recommendation,contact,services,account}.js
    └── assets/images/
    ```
    - `css/customer/services.css` and `js/customer/services.js` are empty and not loaded (pre-existing).
    - **Shared styles in `css/style.css`** (since 2.7): `.page-hero` (moved from `products.css`; it now styles every customer page that uses it), `.form-error`, `.form-success`, `.field-error` (moved from `auth/login.css`, plus the new `.form-success`). Page CSS files keep only page-specific rules.
    - **Form pattern** (register, account): `<p class="form-error" …hidden>` / `<p class="form-success" …hidden>` for the whole form, `<small class="field-error" data-field="<json field>">` per input, `novalidate` + JS validation that mirrors the backend DTO, a `FIELD_MESSAGES` map in Vietnamese, and backend `details` mapped onto the same fields.
  - **Script order on pages with header/footer:** `js/core/api.js` → `js/core/layout.js` → `js/core/main.js` → page script. Auth pages load only `api.js` + their own script. Customer pages use `../` for `css/`, `js/` and `assets/`.
  - **Shared header/footer:**
    - A page contains `<div data-include="header" data-active="home|products|recommendation|services|contact"></div>` and `<div data-include="footer"></div>`.
    - `js/core/layout.js` fetches `partials/<name>.html`, replaces `{{ROOT}}` with the site root and **replaces** the placeholder element. It does not nest it, so the sticky header keeps working. The nav link whose `data-page` equals `data-active` gets class `active`.
    - It exposes the promise `layoutReady` and fires the event `layout:ready`. `main.js` awaits `layoutReady` before `renderAuthState()` / `updateCartCount()`; any script that touches the header must do the same.
    - All pages share the **full footer** (the one from index.html); cart, contact and services used to have shorter footers.
    - **Needs HTTP** (Live Server). Opening a file directly (`file://`) cannot fetch the partials, and `layout.js` logs a `console.warn`.
  - **Site root:** `api.js` defines `SITE_ROOT = new URL("../../", document.currentScript.src)` and `siteUrl(path)`. Links built in JS and redirects must use `siteUrl("customer/…")` / `siteUrl("auth/…")`, never hard-coded relative paths. It works whether the site is served at `/` (Live Server opened on `frontend/`) or under `/frontend/` (opened on the workspace root); both were verified.
  - **Dev hosting:**
    - VS Code Live Server on port **5501** (set `liveServer.settings.port` to 5501), because 5500 is taken by Oracle `TNSLSNR` (§7 #14);
    - `application-dev.yml` CORS allows 5500 and 5501 on both `127.0.0.1` and `localhost`.
  - **Frontend conventions** (keep them):
    - plain `function () {}` style with generous blank lines, 4-space indentation, double quotes;
    - Vietnamese UI text and comments;
    - each page script wraps its code in `DOMContentLoaded`;
    - localStorage keys are prefixed `lahy_`;
    - new pages go into the folder of their role and get their JS/CSS in the mirrored folder.
  - **`frontend/js/core/api.js` API:**
    - `API_BASE_URL = "http://localhost:8080/api/v1"`, `SITE_ROOT`, `siteUrl(path)`.
    - `apiRequest(path, {method, body, auth})`:
      - returns `data`, or throws `ApiError{status, code, message, details}` (network failure → code `NETWORK_ERROR`, status 0);
      - with `auth: true` it sends the bearer token, and on 401 it refreshes **once** and retries;
      - concurrent 401s share one refresh;
      - a failed refresh with 401/403 → `clearAuth()`.
    - Auth storage: `getAuth` / `saveAuth` / `clearAuth` / `isLoggedIn` / `getCurrentUser`, in localStorage `lahy_auth` = `{accessToken, refreshToken, user}`.
    - `logout()` clears the browser first, then calls `POST /auth/logout`, ignoring errors.
    - `getErrorMessage(error)` maps backend codes to Vietnamese through `API_ERROR_MESSAGES`.
    - `getRedirectTarget(default)` accepts only `?redirect=[folder/]name.html[?query]`: at most 1 folder, no `..`, no scheme or `//` (open-redirect guard). It returns an **absolute** URL (`siteUrl`).
    - `redirectToLogin()` → `auth/login.html?redirect=<current page path relative to SITE_ROOT>`.
    - Only send the token (`auth: true`) to endpoints that need it, because a bad token is rejected even on public endpoints.
  - `docs/` (untracked, local only):
    - this file;
    - `PENDING_WORK.md`: the open work, in Vietnamese; read first;
    - `claude.md`: a short pointer;
    - `tools/e2e/`: the browser test scripts of session 2.
- **Planned phases:**
  1. Product ← **DONE**
  2. User + Auth + JWT ← **IN PROGRESS** (backend 2.1–2.5b done and committed; frontend 2.6, restructure and 2.7 done; next: 2.8 products from the API; see §10)
  3. Cart
  4. Order
  5. Payment + Installment
  6. Warranty / Maintenance / Return
  7. Store / Employee / Sales
  8. Recommendation (separate Python/FastAPI AI service)
  9. Chatbot (Python/FastAPI)
  10. Redis cache / rate limiting / admin / reports

# 2. Current Architecture

Layered architecture with sub-packages per domain. Domains: `product` (complete), `user` (entities and repositories 2.1, self-service API 2.5a, admin API 2.5b) and `auth` (Auth API, 2.3).
Paths below are relative to `backend/Tech/src/main/java/com/example/Tech/`.

| Layer | Location | Contents |
|---|---|---|
| Entity | `entity/product/` | `Category`, `Brand`, `Product`, `ProductVariant`, `ProductImage`, `Attribute`, `AttributeValue`, `VariantAttributeValue`, `VariantAttributeValueId` (@Embeddable), `ProductSpecification` |
| Entity | `entity/user/` | `Role`, `User` (soft delete via `deletedAt`; `isEnabled()` = not deleted and `active` ≠ false, added in 2.5a), `UserRole` + `UserRoleId` (@EmbeddedId + 2× @MapsId, `assignedAt`), `CustomerProfile` (shared PK: `@Id customerId` + `@MapsId @OneToOne(LAZY) user`), enum `RoleName` {CUSTOMER, STAFF, ADMIN} |
| Repository | `repository/product/` | 9 `JpaRepository` interfaces + `ProductFilterSpecifications` (JPA Criteria filters for the product search) |
| Repository | `repository/user/` | `RoleRepository` (`findByName`, `existsByName`), `UserRepository` (`findByEmail`, `findByUsername`, `existsByEmail`, `existsByUsername`; callers must pass normalized lower-case values; `JpaSpecificationExecutor` since 2.5b), `UserRoleRepository` (`findAllByIdUserId` with `@EntityGraph(role)`; `findRoleNamesByUserId` = JPQL `select r.name … order by r.name`, used for tokens and responses since 2.5a; 2.5b: `findRoleNamesByUserIds(ids)` → projection `UserRoleName(userId, roleName)` for a whole page in one query, `countEnabledUsersWithRole(name)`; `existsByRoleName`), `CustomerProfileRepository`, `UserFilterSpecifications` (2.5b: `deletedAt is null` unless `includeDeleted`; keyword LIKE on email / username / `lower(fullname)`; `isActive` where NULL counts as active; role through an EXISTS subquery, upper-cased) |
| DTO request | `dto/request/product/` | `*CreateRequest` / `*UpdateRequest` records for all 8 resources + `ProductSearchRequest` |
| DTO request | `dto/request/auth/` | `RegisterRequest` (email `@Email` max 120; username 3–50 chars matching `AccountUtil.USERNAME_REGEX`; password 8–72 chars; fullname max 120; phone optional, max 20); `LoginRequest(identifier, password)`; `RefreshTokenRequest(refreshToken)`, used by refresh **and** logout |
| DTO response | `dto/response/product/`, `dto/response/common/` | 8 `*Response` records; `ApiResult<T>`, `ApiError`, `PageResponse<T>` |
| DTO request | `dto/request/user/` (2.5a) | `UserUpdateRequest(fullname required ≤120, phone ≤20, avatarUrl ≤2048)`, which cannot change email/username (unknown JSON fields are ignored); `ChangePasswordRequest(currentPassword, newPassword 8–72)`; `CustomerProfileUpdateRequest(dateOfBirth @Past, gender MALE\|FEMALE\|OTHER, address ≤255, city/district/ward ≤100, postalCode ≤20, defaultShippingAddress ≤1000)`. PUT semantics: omitted fields are cleared |
| DTO response | `dto/response/user/` (2.5a) | `UserResponse(id, email, username, fullname, phone, avatarUrl, isActive, roles, lastLogin, createdAt, updatedAt)`, `CustomerProfileResponse(customerId, dateOfBirth, gender, address, city, district, ward, postalCode, defaultShippingAddress, loyaltyPoints, totalSpent, createdAt, updatedAt)` |
| DTO response | `dto/response/auth/` | `AuthResponse(accessToken, refreshToken, tokenType="Bearer", expiresIn=1800, user)`, `AuthUserResponse(id, email, username, fullname, roles)` |
| Mapper | `mapper/product/` | 8 hand-written `@Component` mappers (no MapStruct) |
| Mapper | `mapper/user/CustomerProfileMapper.java` (2.5a) | `updateEntity` (trims; blank → null; never touches loyaltyPoints/totalSpent), `toResponse` |
| Service | `service/user/UserService`, `service/impl/user/UserServiceImpl` (2.5a) | class `@Transactional(readOnly)`; each call first runs `loadCurrentUser(id)`: missing → 401 INVALID_TOKEN, `!isEnabled()` → 403 ACCOUNT_DISABLED (this closes the 30-min access-token window for these endpoints). `getMe`, `updateMe`, `changePassword` (wrong current → INVALID_PASSWORD; >72 bytes or same as current → VALIDATION_ERROR; then BCrypt + `revokeAll`), `getMyProfile` (missing → CUSTOMER_PROFILE_NOT_FOUND), `updateMyProfile` (upsert: creates `CustomerProfile(user)` when missing) |
| DTO request | `dto/request/user/` (2.5b) | `UserSearchRequest(keyword ≤255, role ≤50, isActive, includeDeleted)`, `UserStatusUpdateRequest(active @NotNull)`, `UserRolesUpdateRequest(roles @NotEmpty, elements @NotBlank)` |
| Service | `service/user/AdminUserService`, `service/impl/user/AdminUserServiceImpl` (2.5b) | Every call first runs `ensureActingAdmin(adminId)`: the caller must still exist (else 401 INVALID_TOKEN), be enabled (else 403 ACCOUNT_DISABLED) and have ADMIN **in the DB** (else 403 ACCESS_DENIED). This closes the 30-min window for admin actions. Per-method rules are listed below the table |
| Controller | `controller/user/AdminUserController.java` (2.5b) | `/api/v1/admin/users`, class-level `@PreAuthorize("hasRole('ADMIN')")` on top of the URL rule: `GET` (`@PageableDefault(size=20, sort="id")`), `GET /{id}`, `PATCH /{id}/status`, `PUT /{id}/roles`, `DELETE /{id}` (204). The admin id comes from the JWT `sub` |
| Controller | `controller/user/UserController.java` (2.5a) | `/api/v1/users/me`: GET, PUT, `PUT /password` (200 `data: null`), `GET /profile`, `PUT /profile`. The user id comes from `@AuthenticationPrincipal Jwt` → `Long.valueOf(jwt.getSubject())` |
| Mapper | `mapper/user/UserMapper.java` | `toEntity(RegisterRequest)`: normalizes email/username, trims fullname, blank phone → null. `toAuthUserResponse(User, roles)`. Since 2.5a: `updateEntity(User, UserUpdateRequest)` (fullname / phone / avatarUrl only, never email or username), `toResponse(User, roles)` → `UserResponse`, and package-private `trimToNull` (also used by `CustomerProfileMapper`) |
| Service | `service/product/` (interfaces), `service/impl/product/` (impls) | 8 services |
| Service | `service/auth/AuthService`, `service/impl/auth/AuthServiceImpl` | class-level `@Transactional`; behaviour of each method is listed below the table |
| Controller | `controller/product/` | 8 `@RestController`s, all under `/api/v1` |
| Controller | `controller/auth/AuthController.java` | `POST /api/v1/auth/register` (201), `/login`, `/refresh`, `/logout` (200, `data` = null) |
| Exception | `exception/` | `ErrorCode` (enum + HttpStatus), `BusinessException`, `ResourceNotFoundException`, `GlobalExceptionHandler` (`@RestControllerAdvice`). Since 2.4 it also maps `AccessDeniedException` (including `AuthorizationDeniedException` from `@PreAuthorize`) → 403 ACCESS_DENIED and `AuthenticationException` → 401 UNAUTHORIZED, before the 500 catch-all |
| Util | `util/AccountUtil.java` | `normalize` (trim + lower-case, ROOT locale), `USERNAME_REGEX` `^[A-Za-z0-9._-]+$`, `MIN_PASSWORD_LENGTH` 8, `MAX_PASSWORD_BYTES` 72 + `exceedsBcryptLimit`. Used by the initializer, the mapper and the auth service |
| Security | `security/SecurityConfig.java` | Stateless; CSRF, httpBasic and formLogin disabled; `cors(withDefaults)`; `oauth2ResourceServer(jwt)` with our converter, entry point and access-denied handler (also set in `exceptionHandling`). `@EnableMethodSecurity`. **Rules (2.4, decision D4), in order:** Swagger + `/error` permitAll → `POST /api/v1/auth/**` permitAll → `GET` on `CATALOG_PATHS` (8 catalogue prefixes: categories, brands, products, product-variants, attributes, attribute-values, product-images, product-specifications) permitAll → `POST/PUT/PATCH/DELETE` on `CATALOG_PATHS` `hasRole("ADMIN")` → `/api/v1/admin/**` `hasRole("ADMIN")` → other `/api/v1/**` `authenticated()` → anything else `denyAll`. CORS preflight is answered by the CORS filter before these rules. Bean `PasswordEncoder` = `BCryptPasswordEncoder` |
| Security | `security/JwtProperties.java` | `@ConfigurationProperties("app.jwt")` record: `secret`, `issuer`, `accessTokenTtl`, `refreshTokenTtl`. It **fails startup** if the secret is missing or < 32 bytes ("app.jwt.secret (environment variable JWT_SECRET) must be set and at least 32 bytes long"), if the issuer is blank, or if a TTL is ≤ 0 |
| Security | `security/JwtConfig.java` | beans `JwtEncoder` (`NimbusJwtEncoder.withSecretKey`, HS256), `JwtDecoder` (`NimbusJwtDecoder.withSecretKey`, HS256, `JwtValidators.createDefaultWithIssuer`), `JwtAuthenticationConverter` (claim `roles` → `ROLE_*`). Constants `ROLES_CLAIM` = "roles", `USERNAME_CLAIM` = "username" |
| Security | `security/JwtTokenService.java` | `issueAccessToken(User, List<String> roles)`: header HS256 / typ JWT; claims `iss`, `sub` = userId, `iat`, `exp` = iat + 30 min, `username`, `roles`. Also `accessTokenTtlSeconds()`. Uses the `Clock` bean |
| Security | `security/RefreshTokenService.java` | Opaque refresh tokens in Redis. `issue(userId)` makes a 32-byte SecureRandom base64url token (43 chars) and stores key `auth:refresh:<sha256 hex>` → userId with TTL 7 d. `consume(token)` does an atomic GETDEL and returns the userId; unknown, used or blank → `BusinessException(INVALID_TOKEN)`. `revoke(token)` deletes the key (GETDEL; unknown tokens are ignored). **Rotation = `consume` + a user check + `issue`**, done by the auth service. **Per-user index (2.5a):** `issue` also adds the hash to the set `auth:user-refresh:<userId>`, whose TTL is reset to 7 d on each issue; `consume` / `revoke` remove it; `revokeAll(userId)` deletes every indexed token and the set. Used by password change (2.5a) and admin deactivate/delete (2.5b) |
| Security | `security/RestAuthenticationEntryPoint.java`, `RestAccessDeniedHandler.java`, `SecurityErrorResponseWriter.java` | 401/403 raised by filters, returned as `ApiResult` JSON through the Jackson 3 `JsonMapper` bean. They first call Spring's `BearerTokenAuthenticationEntryPoint` / `BearerTokenAccessDeniedHandler`, so the RFC 6750 `WWW-Authenticate` header is kept. 401 code: `INVALID_TOKEN` if a bearer token was rejected (`OAuth2AuthenticationException`), otherwise `UNAUTHORIZED`. 403 code: `ACCESS_DENIED` |
| Config | `config/OpenApiConfig.java` | OpenAPI info (description covers catalogue + auth and how to authorize), security scheme `bearerAuth` (HTTP bearer JWT) + a global security requirement, so Swagger UI has an "Authorize" button |
| Config | `config/CorsConfig.java`, `config/CorsProperties.java` | `CorsConfigurationSource` for `/api/**`. Origins come from `app.cors.allowed-origins` (blank entries are dropped). Methods GET/POST/PUT/PATCH/DELETE/OPTIONS; headers Authorization and Content-Type; exposes WWW-Authenticate; credentials off; max-age 1 h |
| Config | `config/ClockConfig.java` | `Clock` bean (system clock), so tests can use a fixed clock |
| Config | `config/AuthDataInitializer.java`, `config/BootstrapAdminProperties.java` | `ApplicationRunner` (@Transactional). It creates the missing roles CUSTOMER/STAFF/ADMIN at every startup, and creates the first ADMIN from `app.bootstrap-admin.*` ← env `ADMIN_EMAIL`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`. It skips with a WARN if a value is missing, the password has < 8 chars or > 72 bytes, or the email/username is taken. It skips silently if any ADMIN already exists. Email/username are trimmed and lower-cased; the password is BCrypt-hashed. Admin fullname = "Administrator" |
| Util | `util/SlugUtil.java` | slug generation: strips Vietnamese diacritics, `đ`→`d`, `+`→`plus`; `SLUG_REGEX` |
| App | `TechApplication.java` | sets default TimeZone to `Asia/Ho_Chi_Minh` before start (see Known Issues #1) |

**`AuthServiceImpl` behaviour:**
- **register:**
  - rejects passwords over 72 bytes (VALIDATION_ERROR);
  - DUPLICATE_EMAIL / DUPLICATE_USERNAME;
  - then User (`saveAndFlush`) + UserRole CUSTOMER + CustomerProfile in one transaction;
  - returns tokens.
- **login:**
  - an identifier containing `@` is looked up by email, otherwise by username (normalized);
  - unknown user → a dummy BCrypt check (timing), then INVALID_CREDENTIALS;
  - wrong password → INVALID_CREDENTIALS;
  - only after the password matches: disabled or deleted → ACCOUNT_DISABLED;
  - sets `lastLogin`.
- **refresh:** `consume` → user missing → INVALID_TOKEN; disabled → ACCOUNT_DISABLED; otherwise a new pair (rotation).
- **logout:** `revoke`.
- Roles come from `findRoleNamesByUserId` (sorted in SQL).

**`AdminUserServiceImpl` rules (2.5b):**
- **search:** filter + page, with roles loaded in one query. **getById:** soft-deleted users are included; missing → 404 USER_NOT_FOUND.
- **updateStatus:**
  - on yourself → 409 CANNOT_MODIFY_OWN_ACCOUNT;
  - on a deleted user → 409 USER_DELETED (deleted accounts cannot be reactivated);
  - deactivating the last enabled ADMIN → 409 LAST_ADMIN;
  - deactivation runs `revokeAll`; activation does not.
- **updateRoles:**
  - names are trimmed and upper-cased, de-duplicated and sorted; an unknown name → 404 ROLE_NOT_FOUND ("Role 'X' does not exist");
  - on a deleted user → 409 USER_DELETED;
  - removing your own ADMIN → 409 CANNOT_MODIFY_OWN_ACCOUNT (changing your other roles is allowed); removing ADMIN from the last enabled ADMIN → 409 LAST_ADMIN;
  - only the difference is written (removed roles deleted, new roles inserted);
  - **no token revocation**: `/auth/refresh` re-reads the roles, so new roles apply within one access-token lifetime (≤ 30 min), and a new login applies them immediately.
- **delete:**
  - on yourself → 409;
  - already deleted → no-op 204 (idempotent);
  - on the last enabled ADMIN → 409 LAST_ADMIN;
  - otherwise sets `deletedAt = now(clock)` and `active = false`, then runs `revokeAll`. The customer profile is kept.
- The LAST_ADMIN checks are defence in depth: because the caller must be an enabled ADMIN and cannot target themselves, at least 2 enabled admins exist whenever another admin is targeted. They are covered by unit tests.

**Configuration** (`src/main/resources/`):
- `application.yml`: default profile `dev`; `ddl-auto: validate`; `open-in-view: false`; pageable default 20, max 100; springdoc paths; port 8080;
  - `app.bootstrap-admin.{email,username,password}` = `${ADMIN_EMAIL:}` / `${ADMIN_USERNAME:}` / `${ADMIN_PASSWORD:}` (empty by default, no secrets in the file);
  - `app.jwt.secret: ${JWT_SECRET:}` (**required**; startup fails if missing or short), `issuer: techshopping`, `access-token-ttl: 30m`, `refresh-token-ttl: 7d`.
- `application-dev.yml`: `localhost:5432/techshopping`; `${DB_USERNAME:postgres}` / `${DB_PASSWORD:postgres}`; Redis on localhost:6379; show-sql; `app.cors.allowed-origins`: `http://127.0.0.1:5500`, `http://localhost:5500`, `http://127.0.0.1:5501`, `http://localhost:5501` (Live Server; the 5501 entries were added after 2.6 and are **not committed**).
- `application-test.yml`: `localhost:5432/techshopping_test`; a fixed test-only `app.jwt.secret`; no CORS origins (the integration test sets its own).
- `application-prod.yml`: `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`, `${REDIS_HOST}`, `${REDIS_PORT}`, `app.jwt.secret: ${JWT_SECRET}`, `app.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:}` (comma-separated); Swagger disabled.
- There is no `application.properties` (deleted on purpose).

**pom.xml additions** over the initial project: security, **oauth2-resource-server** (added in 2.2, version managed by Boot), data-redis, springdoc 3.0.3, security-test. Surefire has `argLine -Duser.timezone=Asia/Ho_Chi_Minh`.

**Tests** (`backend/Tech/src/test/java/com/example/Tech/`):
- `service/impl/product/*ServiceImplTest`: 8 classes, JUnit 5 + Mockito, with the real mappers.
- `repository/product/ProductFilterSpecificationsTest`: `@DataJpaTest` against the real PostgreSQL `techshopping_test`, with `@AutoConfigureTestDatabase(replace = NONE)`; rolls back after each test.
- `repository/user/UserRepositoryTest` (5 now; 3 in 2.1): same setup; checks the User defaults, `UserRole` composite key + `@EntityGraph`, and the `CustomerProfile` shared PK. Uses its own role `TEST_ROLE_JPA` (rolled back).
- `config/AuthDataInitializerTest` (6): Mockito + a real `BCryptPasswordEncoder(4)`.
- `security/` (2.2):
  - `JwtPropertiesTest` (4);
  - `JwtTokenServiceTest` (6): the real `JwtConfig` beans without Spring; checks claims, expiry, wrong secret, wrong issuer and role authorities;
  - `RefreshTokenServiceTest` (10 now; 7 in 2.2): Redis mocked;
  - `SecurityErrorHandlersTest` (3): the 401/403 JSON bodies and the `WWW-Authenticate` header;
  - `SecurityInfrastructureIntegrationTest` (6): `@SpringBootTest` + `@AutoConfigureMockMvc` (package `org.springframework.boot.webmvc.test.autoconfigure`), with property `app.cors.allowed-origins=http://localhost:5500`; checks CORS preflight allowed/rejected, public GET, a valid token, an invalid token → 401 INVALID_TOKEN, and a denied path → 401 UNAUTHORIZED;
  - `RefreshTokenServiceRedisIntegrationTest` (3 now; 2 in 2.2): runs against the **real Redis** container and leaves no keys.
- 2.5b:
  - `service/impl/user/AdminUserServiceImplTest` (19): Mockito; checks the acting-admin re-check, the one-query role loading, and all status / role / delete rules including LAST_ADMIN;
  - `controller/user/AdminUserApiIntegrationTest` (9): MockMvc + `@Transactional`. It saves its own ADMIN user (token from `JwtTokenService`) and registers customers through the API; `revokeAll` runs for every created user. It checks:
    - CUSTOMER 403 and anonymous 401;
    - an admin demoted in the DB is denied despite an ADMIN token;
    - search with keyword (including Vietnamese upper case "KHÁCH HÀNG"), role filter, paging and an unknown sort field (400);
    - get by id / 404;
    - deactivate → login 403, old refresh 401, `/users/me` 403; reactivate → login 200;
    - status validation and self-protection;
    - roles → the next login token has the new roles; empty list 400; unknown role 404; own ADMIN removal 409;
    - soft delete (idempotent; hidden unless `includeDeleted`; login 403; USER_DELETED on reactivation; self-delete 409).
  - `ProductSecurityIntegrationTest.adminArea_requiresAdmin` was updated: a fake ADMIN token (user id not in the DB) now gets 401 INVALID_TOKEN from the service re-check; `/api/v1/admin/not-mapped-yet` with that token gets 404.
- 2.5a:
  - `service/impl/user/UserServiceImplTest` (12): Mockito + real BCrypt + real mappers;
  - `controller/user/UserApiIntegrationTest` (6): MockMvc + `@Transactional`. It registers its own user in `@BeforeEach` and runs `revokeAll` in `@AfterEach`. It checks: anonymous 401 / own account; email and username in the body are ignored; the profile created at registration; profile update + validation 400; missing profile → 404, then PUT creates it; password change → old refresh token 401, old password 401, new password 200; deactivated account + valid token → 403;
  - `RefreshTokenServiceTest` was rewritten (10) for the index;
  - `RefreshTokenServiceRedisIntegrationTest` now has 3 (+`revokeAll` only affects that user);
  - `UserRepositoryTest` now has 5 (+sorted role names, +`isEnabled`);
  - `ProductSecurityIntegrationTest` now uses `/api/v1/not-mapped-yet` as its "unmapped path" (because `/users/me` exists now).
- 2.4:
  - `security/ProductSecurityIntegrationTest` (14 = 8 parameterized + 6), real tokens from `JwtTokenService`, `@Transactional`. For each of the 8 catalogue resources: anonymous GET of an unknown id → 404 (so public); anonymous POST → 401 UNAUTHORIZED; CUSTOMER and STAFF POST → 403 ACCESS_DENIED; CUSTOMER PUT/DELETE → 403; ADMIN POST `{}` → 400 VALIDATION_ERROR (authorization passed, nothing created). Also: public lists 200; ADMIN DELETE of an unknown brand → 404 BRAND_NOT_FOUND; CUSTOMER+ADMIN → allowed; `/api/v1/admin/**` anonymous 401 / CUSTOMER 403 / ADMIN 404 (no endpoints yet); `/api/v1/users/me` anonymous 401 / authenticated 404 (no endpoint yet); auth endpoints stay public.
  - `exception/GlobalExceptionHandlerSecurityTest` (2).
- 2.3:
  - `service/impl/auth/AuthServiceImplTest` (15): Mockito + real `BCryptPasswordEncoder(4)` + fixed `Clock`;
  - `controller/auth/AuthApiIntegrationTest` (5): `@SpringBootTest` + MockMvc + **`@Transactional`**, so DB rows are rolled back, because MockMvc runs in the test thread. All refresh tokens it creates are revoked in `@AfterEach`. It checks the full flow register → login → a protected-style call with the token → refresh rotation (old token 401) → logout (refresh then 401), plus login by email, wrong password, duplicates 409, and validation 400 with field details.
- `util/SlugUtilTest`.
- `TechApplicationTests`: `@SpringBootTest` + `@ActiveProfiles("test")`. It loads the full context, which also means Hibernate schema validation. It also runs `AuthDataInitializer`, so `techshopping_test` keeps the 3 roles (not rolled back; this is expected and idempotent).

# 3. Database Status

- **Engine:** PostgreSQL 16, container `techshopping-postgres`.
- **Databases:** `techshopping` (dev) and `techshopping_test` (tests).
- **Schema:** `database/techshopping.sql`, which has no version number. It contains 35 tables, indexes, 5 extra CHECK constraints and 3 views (`v_products_full`, `v_sales_by_store`, `v_customer_stats`).
  - Both databases were created from this file by the docker init scripts, and both have 35 tables (verified).
  - The init scripts **only run when the `postgres_data` volume is empty.**
- **Hibernate policy:** `ddl-auto: validate`. Never use create/update. The schema is created only from the SQL file. **Do not modify `techshopping.sql` without the user's approval.**

## Tables mapped (13 of 35: Product group + User group)

User group (Checkpoint 2.1):

| Table | PK | Notes |
|---|---|---|
| `roles` | `role_id` serial → Integer | `name` v50 UQ NN; seeded at startup (no SQL seed) |
| `users` | `user_id` bigserial → Long | `email` v120 UQ, `username` v50 UQ (both stored lower-case); `password_hash` BCrypt; `is_active` nullable default true (entity sets true); `deleted_at` soft delete |
| `user_roles` | composite (`user_id`, `role_id`) | both FKs ON DELETE CASCADE; extra `assigned_at` → mapped as entity `UserRole` |
| `customer_profiles` | `customer_id` = FK users (CASCADE) | shared PK via `@MapsId`; `loyalty_points` 0, `total_spent` 0 |

Product group:

| Table | PK | Notes |
|---|---|---|
| `categories` | `category_id` serial → Integer | `slug` UQ NN; `parent_id` self-FK (no cascade); `name` **not** unique |
| `brands` | `brand_id` serial → Integer | `name` UQ, `slug` UQ |
| `products` | `product_id` bigserial → Long | `category_id` NN FK; `brand_id` **nullable** FK; `slug` UQ; `sku` UQ nullable; `deleted_at` (soft delete); CHECK `base_price >= 0` |
| `product_variants` | `variant_id` bigserial | FK product ON DELETE CASCADE; `sku_variant` UQ nullable; CHECK `price >= 0`; has `color`, `storage`, `ram` columns |
| `product_images` | `image_id` bigserial | FK product ON DELETE CASCADE; `is_primary` default false |
| `product_specifications` | `specification_id` bigserial | FK product ON DELETE CASCADE; `spec_name` v100 NN, `spec_value` text NN |
| `attributes` | `attribute_id` serial → Integer | `name` UQ; `attribute_type` free varchar(50) |
| `attribute_values` | `value_id` serial → Integer | FK attribute (no cascade); UQ(`attribute_id`, `value`) |
| `variant_attribute_values` | composite (`variant_id`, `value_id`) | both FKs ON DELETE CASCADE; **no extra columns** |

Foreign keys that point at Product tables from modules not built yet: `cart_items` and `order_items` → `product_variants`; `user_interactions`, `recommendations` and `product_relations` → `products`. None of these cascade.

## Not implemented (22 tables)

| Group | Tables |
|---|---|
| Cart / Order | `carts`, `cart_items`, `orders`, `order_items` |
| Payment | `payments`, `installment_orders`, `installment_payments` |
| Warranty / Return | `warranties`, `warranty_requests`, `maintenance_requests`, `return_requests`, `return_items` |
| Store | `stores`, `employees`, `employee_assignments`, `sales_records` |
| Recommendation | `user_interactions`, `recommendations`, `product_relations` |
| Chat | `chat_sessions`, `chat_messages`, `support_tickets` |

The full User-group schema (every column) is in §10.3. The SQL has **no role seed data**; `AuthDataInitializer` creates the roles (decision D1).

## Schema observations (not acted on; do not "fix" without approval)

- `warranty_requests.assigned_to_employee` and `maintenance_requests.assigned_to_employee` reference `users(user_id)`, while `chat_sessions.assigned_to_employee` and `support_tickets.employee_id` reference `employees(employee_id)`. This is inconsistent.
- `updated_at` columns only have a default; there are no triggers. Entities use `@UpdateTimestamp`.
- Product options are stored twice: `product_variants.color/storage/ram` and the attribute tables. Both are mapped as-is.

## Current data in dev DB `techshopping` (imported through the API from `Raw_data`)

| Table | Rows |
|---|---|
| categories | 8 (6 real + 2 E2E test) |
| brands | 55 (54 real + 1 E2E test) |
| products | 877 active + 2 soft-deleted test products |
| product_variants | 877 (one per product; RAM/storage parsed from the name) |
| product_images | 762 (761 real + 1 attached to a soft-deleted test product) |
| attributes | 3 (Màu sắc, RAM, Bộ nhớ trong) |
| attribute_values | 87 |
| variant_attribute_values | 1709 |
| product_specifications | 1 (attached to a soft-deleted test product) |
| roles | 3: 1 CUSTOMER, 2 STAFF, 3 ADMIN (created by `AuthDataInitializer` on 2026-09-25) |
| users / user_roles / customer_profiles | 0. **No ADMIN yet**: the dev app was started without `ADMIN_*`. The user must start it once with their own `ADMIN_EMAIL` / `ADMIN_USERNAME` / `ADMIN_PASSWORD` (see §11) |

`techshopping_test`:
- 0 products; tests roll back.
- roles CUSTOMER 4 / STAFF 5 / ADMIN 6 (ids 1–3 were used by rolled-back test inserts).
- **1 verification admin**: user_id 4 `e2e-admin@techshopping.test` / `e2e-admin`, ADMIN role, created by the 2.1 end-to-end check with a random throwaway password. No one knows the password; nothing depends on this account. Because an ADMIN already exists, `AuthDataInitializer` will never create another admin in the test DB.
- **1 verification customer** (Checkpoint 2.3 curl run): user_id 20 `e2e.customer@techshopping.test` / `e2e.customer`, "Nguyễn Văn Kiểm Thử", password `Matkhau@123`, role CUSTOMER + customer_profile. The 2.5a curl run changed phone → `0909999999` and filled the profile (MALE, 1995-08-20, 123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh, 700000); it also changed the password and **changed it back** to `Matkhau@123`. User ids 5–19 were used by rolled-back test inserts. All its refresh tokens were revoked. Automated tests must not use the email/username `e2e.customer`.
- **6 frontend E2E customers** (browser runs of 2.6, the restructure and 2.7), all `@techshopping.test`, role CUSTOMER + profile. Do not reuse these usernames (the next free one is `fe.e2e7`):
  - `fe.e2e` (id 74), `fe.e2e2` (75), `fe.e2e3` (76), `fe.e2e6` (79): "Nguyễn Thị Giao Diện", password `Matkhau@123`;
  - `fe.e2e4` (77) and `fe.e2e5` (78): renamed by the 2.7 test to "Trần Thị Hồ Sơ", profile city TP. Hồ Chí Minh, password **`MatkhauMoi@456`** (changed by the password test).

  All their sessions were revoked; Redis has 0 `auth:*` keys.

# 4. Implemented Features

All Phase 1 features are **DONE**. For each one: an entity, a repository, request/response records, a mapper, a service, a controller, Swagger annotations and unit tests.

| Feature | Main files | API | Tables | Tests | Status |
|---|---|---|---|---|---|
| Common response/error format | `ApiResult`, `ApiError`, `PageResponse`, `ErrorCode`, `GlobalExceptionHandler` | all | – | covered via service tests and E2E | DONE |
| Category CRUD (hierarchy, cycle check, delete guards) | `CategoryController/Service(Impl)`, `Category` | `/api/v1/categories` | categories | `CategoryServiceImplTest` (12) | DONE |
| Brand CRUD (delete guard) | `BrandController/Service(Impl)`, `Brand` | `/api/v1/brands` | brands | `BrandServiceImplTest` (10) | DONE |
| Product CRUD + soft delete | `ProductController/Service(Impl)`, `Product` | `/api/v1/products` | products | `ProductServiceImplTest` (14) | DONE |
| Product search/filter/pagination | `ProductSearchRequest`, `ProductFilterSpecifications`, `ProductRepository.findAll(spec, pageable)` | `GET /api/v1/products` | products | `ProductFilterSpecificationsTest` (9, real DB) + 2 service tests | DONE |
| Pagination for category/brand/attribute lists | controllers with `@PageableDefault(size = 20, …)` | GET list endpoints | – | E2E only | DONE |
| Product variant CRUD | `ProductVariantController/Service(Impl)`, `ProductVariant` | `/api/v1/product-variants`, `/api/v1/products/{id}/variants` | product_variants | `ProductVariantServiceImplTest` (17, shared with the next row) | DONE |
| Variant ↔ attribute value links | same service; `VariantAttributeValue(+Id)`, `VariantAttributeValueRepository` | `POST`/`DELETE /api/v1/product-variants/{id}/attribute-values/{valueId}` | variant_attribute_values | in `ProductVariantServiceImplTest` | DONE |
| Attribute CRUD (delete guard) | `AttributeController/Service(Impl)` | `/api/v1/attributes` | attributes | `AttributeServiceImplTest` (7) | DONE |
| Attribute value CRUD | `AttributeValueController/Service(Impl)` | `/api/v1/attribute-values`, `/api/v1/attributes/{id}/values` | attribute_values | `AttributeValueServiceImplTest` (8) | DONE |
| Product images (single primary per product) | `ProductImageController/Service(Impl)` | `/api/v1/product-images`, `/api/v1/products/{id}/images` | product_images | `ProductImageServiceImplTest` (10) | DONE |
| Product specifications | `ProductSpecificationController/Service(Impl)` | `/api/v1/product-specifications`, `/api/v1/products/{id}/specifications` | product_specifications | `ProductSpecificationServiceImplTest` (8) | DONE |
| Slug generation | `SlugUtil` | used by category/brand/product | – | `SlugUtilTest` (4) | DONE |
| Swagger / OpenAPI | `OpenApiConfig`, annotations | `/swagger-ui/index.html`, `/v3/api-docs` | – | manual (HTTP 200; 42 operations in Phase 1, since then the auth, users/me and admin endpoints were added, 56 endpoints in total; the `bearerAuth` "Authorize" button since 2.4) | DONE |
| Redis | `spring-boot-starter-data-redis` + yml host/port | – | – | used since 2.2 for **refresh tokens** (`RefreshTokenService`, real-Redis integration test) | PARTIAL (refresh tokens only; no caching, no rate limiting yet) |
| Security | `SecurityConfig` | – | – | `ProductSecurityIntegrationTest` (14), `SecurityInfrastructureIntegrationTest` (6) | DONE for Phase 2 scope (JWT, role rules D4, CORS, 401/403 JSON). No rate limiting yet (phase 10) |
| **Phase 2 / 2.6** Frontend auth: login (email or username), register, header name + logout, token refresh | now at `frontend/js/core/api.js`, `js/auth/login.js`, `js/auth/register.js`, `auth/register.html`, `auth/login.html`, `js/core/main.js` (`renderAuthState`), `css/auth/login.css`, `css/style.css` | `/api/v1/auth/*`, `/users/me` | – | headless Edge E2E 24/24 (§6) | DONE |
| **Phase 2 / 2.7** Account page: account info (fullname, phone, avatar; email/username read-only), customer profile (upsert, empty form when missing), change password (then re-login), summary (username, points, total spent, join date), logout; header name links here | `frontend/customer/account.html`, `js/customer/account.js`, `css/customer/account.css` (+ `contact.css` reused), `js/core/main.js` (`href = siteUrl("customer/account.html")`), shared messages in `css/style.css` | `/users/me`, `/users/me/profile`, `/users/me/password`, `/auth/logout` | – | headless Edge E2E 31/31 + layout regression 32/32 (§6) | DONE, not committed |
| **Frontend restructure** (user request, after 2.6): role folders `auth/`, `customer/`, `admin/`; JS/CSS mirrored; shared `partials/header.html` + `partials/footer.html` loaded by `js/core/layout.js`; `SITE_ROOT` / `siteUrl` for folder-independent links | see §1 "Frontend structure" | – | – | headless Edge E2E 32/32 (§6) | DONE, staged, not committed |
| **Phase 2 / 2.5b** Admin user management: search / get / activate-deactivate / replace roles / soft delete, with self-protection and last-admin rules | `AdminUserController`, `AdminUserService(Impl)`, `UserFilterSpecifications`, `UserRoleName` | `/api/v1/admin/users*` | users, user_roles (+ Redis) | `AdminUserServiceImplTest` (19), `AdminUserApiIntegrationTest` (9) | DONE (live ADMIN check only via MockMvc, see §6) |
| **Phase 2 / 2.5a** Self-service account: me / update / change password (revokes all sessions) / customer profile (upsert) | `UserController`, `UserService(Impl)`, `CustomerProfileMapper`, user DTOs, `RefreshTokenService.revokeAll` | `/api/v1/users/me*` | users, customer_profiles (+ Redis) | `UserServiceImplTest` (12), `UserApiIntegrationTest` (6), curl | DONE |
| **Phase 2 / 2.4** Authorization D4 + Swagger bearer | `SecurityConfig`, `GlobalExceptionHandler`, `OpenApiConfig` | all | – | `ProductSecurityIntegrationTest` (14), `GlobalExceptionHandlerSecurityTest` (2), curl | DONE |
| **Phase 2 / 2.2** JWT access tokens (HS256, 30 min) | `JwtProperties`, `JwtConfig`, `JwtTokenService` | – (used by the Auth API in 2.3) | – | `JwtPropertiesTest` (4), `JwtTokenServiceTest` (6) | DONE |
| **Phase 2 / 2.2** Refresh tokens in Redis (7 d, single use) | `RefreshTokenService` | – | – (Redis) | `RefreshTokenServiceTest` (10 now), `RefreshTokenServiceRedisIntegrationTest` (3 now, real Redis) | DONE |
| **Phase 2 / 2.3** Auth API: register / login (email or username) / refresh (rotation) / logout | `AuthController`, `AuthService(Impl)`, `UserMapper`, `AccountUtil`, auth DTOs | `/api/v1/auth/*` | users, user_roles, customer_profiles (+ Redis) | `AuthServiceImplTest` (15), `AuthApiIntegrationTest` (5), curl E2E | DONE |
| **Phase 2 / 2.2** 401/403 JSON + CORS | `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, `SecurityErrorResponseWriter`, `CorsConfig` | all | – | `SecurityErrorHandlersTest` (3), `SecurityInfrastructureIntegrationTest` (6), manual curl | DONE |
| **Phase 2 / 2.1** User-group entities + repositories | `entity/user/*`, `repository/user/*` | – (no API yet) | roles, users, user_roles, customer_profiles | `UserRepositoryTest` (5 now, real DB) | DONE |
| **Phase 2 / 2.1** Role seeding + bootstrap ADMIN | `AuthDataInitializer`, `BootstrapAdminProperties` | – | roles, users, user_roles | `AuthDataInitializerTest` (6) + manual check on dev/test DB | DONE |

**Error codes defined:**
- **400:** VALIDATION_ERROR, MALFORMED_REQUEST, INVALID_CATEGORY_PARENT, INVALID_PRODUCT_DATA, INVALID_PRODUCT_VARIANT_DATA.
- **404:** CATEGORY_NOT_FOUND, BRAND_NOT_FOUND, PRODUCT_NOT_FOUND, PRODUCT_VARIANT_NOT_FOUND, PRODUCT_IMAGE_NOT_FOUND, PRODUCT_SPECIFICATION_NOT_FOUND, ATTRIBUTE_NOT_FOUND, ATTRIBUTE_VALUE_NOT_FOUND, VARIANT_ATTRIBUTE_VALUE_NOT_FOUND.
- **409:** RESOURCE_IN_USE, DATA_INTEGRITY_VIOLATION, DUPLICATE_CATEGORY, DUPLICATE_BRAND, DUPLICATE_PRODUCT, DUPLICATE_PRODUCT_VARIANT, DUPLICATE_ATTRIBUTE, DUPLICATE_ATTRIBUTE_VALUE, DUPLICATE_VARIANT_ATTRIBUTE_VALUE, VARIANT_ATTRIBUTE_CONFLICT.
- **500:** INTERNAL_ERROR.
- **Added in 2.2 for auth/user** (defined but not all used yet):
  - 401: UNAUTHORIZED, INVALID_TOKEN, INVALID_CREDENTIALS;
  - 403: ACCESS_DENIED, ACCOUNT_DISABLED;
  - 404: USER_NOT_FOUND, ROLE_NOT_FOUND, CUSTOMER_PROFILE_NOT_FOUND (added in 2.5a);
  - 409: DUPLICATE_EMAIL, DUPLICATE_USERNAME;
  - 400: INVALID_PASSWORD;
  - 409 (added in 2.5b): CANNOT_MODIFY_OWN_ACCOUNT, LAST_ADMIN, USER_DELETED.
- The handler also returns `NOT_FOUND` / `METHOD_NOT_ALLOWED` for unknown paths and wrong HTTP methods.

# 5. Current API

All 56 endpoints below exist in code (read from the controllers). The 5 `/admin/users` endpoints (2.5b) were tested by `AdminUserApiIntegrationTest`; curl covered only the 401/403 paths. The 42 Product endpoints were tested through the Phase 1 E2E script. The 4 Auth endpoints (2.3) were tested by `AuthApiIntegrationTest` and a curl run. The 5 `/users/me` endpoints (2.5a) were tested by `UserApiIntegrationTest` and a curl run.

**Access rules since 2.4 (D4):**
- every `GET` below and the 4 `POST /api/v1/auth/*` are **public**;
- every catalogue `POST` / `PUT` / `DELETE` needs an **ADMIN** access token (`Authorization: Bearer …`);
- without a token → 401 UNAUTHORIZED; with a non-ADMIN token → 403 ACCESS_DENIED;
- the Phase 1 E2E and import scripts would now need an ADMIN token for writes.
Response format: `{success, timestamp, data, error:{code, message, details}}`. Lists return `data = {content, page, size, totalElements, totalPages}`.

| METHOD | PATH | PURPOSE | STATUS |
|---|---|---|---|
| GET | /api/v1/categories | list, paginated (default sort displayOrder, id) | DONE |
| GET | /api/v1/categories/{id} | get by id | DONE |
| POST | /api/v1/categories | create (slug auto-generated if blank) | DONE |
| PUT | /api/v1/categories/{id} | update (parent cycle check) | DONE |
| DELETE | /api/v1/categories/{id} | delete; 409 if it has children or products | DONE |
| GET | /api/v1/brands | list, paginated (default sort name) | DONE |
| GET | /api/v1/brands/{id} | get by id | DONE |
| POST | /api/v1/brands | create | DONE |
| PUT | /api/v1/brands/{id} | update | DONE |
| DELETE | /api/v1/brands/{id} | delete; 409 if it has products | DONE |
| GET | /api/v1/products | search + paginate: `keyword`, `categoryId`, `brandId`, `isActive`, `minPrice`, `maxPrice`, `page`, `size`, `sort` | DONE |
| GET | /api/v1/products/{id} | get by id (soft-deleted → 404) | DONE |
| POST | /api/v1/products | create | DONE |
| PUT | /api/v1/products/{id} | update | DONE |
| DELETE | /api/v1/products/{id} | soft delete (`deleted_at`, `is_active = false`) | DONE |
| GET | /api/v1/products/{productId}/variants | variants of a product, with attributeValues | DONE |
| GET | /api/v1/product-variants/{id} | get variant | DONE |
| POST | /api/v1/product-variants | create variant | DONE |
| PUT | /api/v1/product-variants/{id} | update variant (product not changeable) | DONE |
| DELETE | /api/v1/product-variants/{id} | hard delete (the DB cascades links) | DONE |
| POST | /api/v1/product-variants/{id}/attribute-values/{valueId} | assign a value (max 1 per attribute) | DONE |
| DELETE | /api/v1/product-variants/{id}/attribute-values/{valueId} | remove a value | DONE |
| GET | /api/v1/attributes | list, paginated (default sort name) | DONE |
| GET | /api/v1/attributes/{id} | get by id | DONE |
| GET | /api/v1/attributes/{id}/values | values of an attribute | DONE |
| POST | /api/v1/attributes | create | DONE |
| PUT | /api/v1/attributes/{id} | update | DONE |
| DELETE | /api/v1/attributes/{id} | delete; 409 if it has values | DONE |
| GET | /api/v1/attribute-values/{id} | get by id | DONE |
| POST | /api/v1/attribute-values | create | DONE |
| PUT | /api/v1/attribute-values/{id} | update value text | DONE |
| DELETE | /api/v1/attribute-values/{id} | delete (the DB cascades links) | DONE |
| GET | /api/v1/products/{productId}/images | images ordered by display_order | DONE |
| GET | /api/v1/product-images/{id} | get image | DONE |
| POST | /api/v1/product-images | add image (`isPrimary = true` unsets the others) | DONE |
| PUT | /api/v1/product-images/{id} | update image | DONE |
| DELETE | /api/v1/product-images/{id} | delete image | DONE |
| GET | /api/v1/products/{productId}/specifications | specs ordered by spec_order | DONE |
| GET | /api/v1/product-specifications/{id} | get spec | DONE |
| POST | /api/v1/product-specifications | create spec | DONE |
| PUT | /api/v1/product-specifications/{id} | update spec | DONE |
| DELETE | /api/v1/product-specifications/{id} | delete spec | DONE |
| POST | /api/v1/auth/register | `{email, username, password, fullname, phone?}` → 201 `AuthResponse`; account gets role CUSTOMER + customer profile | DONE |
| POST | /api/v1/auth/login | `{identifier (email or username), password}` → 200 `AuthResponse`; 401 INVALID_CREDENTIALS; 403 ACCOUNT_DISABLED | DONE |
| POST | /api/v1/auth/refresh | `{refreshToken}` → 200 new `AuthResponse`; the old refresh token is invalid afterwards; 401 INVALID_TOKEN | DONE |
| POST | /api/v1/auth/logout | `{refreshToken}` → 200 `data: null`; revokes that refresh token (unknown tokens are ignored) | DONE |
| GET | /api/v1/users/me | own account `UserResponse` (token required; 403 ACCOUNT_DISABLED if deactivated or deleted) | DONE |
| PUT | /api/v1/users/me | `{fullname, phone?, avatarUrl?}`; email/username cannot change | DONE |
| PUT | /api/v1/users/me/password | `{currentPassword, newPassword}` → 200; 400 INVALID_PASSWORD; revokes **all** refresh tokens of the user | DONE |
| GET | /api/v1/users/me/profile | own `CustomerProfileResponse`; 404 CUSTOMER_PROFILE_NOT_FOUND (e.g. admin accounts) | DONE |
| PUT | /api/v1/users/me/profile | create or update own profile (dateOfBirth, gender MALE/FEMALE/OTHER, address fields); loyalty/totalSpent read-only | DONE |
| GET | /api/v1/admin/users | ADMIN: search `keyword`, `role`, `isActive`, `includeDeleted`, `page`, `size`, `sort` → `PageResponse<UserResponse>` | DONE |
| GET | /api/v1/admin/users/{id} | ADMIN: user by id (deleted users included); 404 USER_NOT_FOUND | DONE |
| PATCH | /api/v1/admin/users/{id}/status | ADMIN: `{active}`; deactivation revokes all sessions; 409 CANNOT_MODIFY_OWN_ACCOUNT / LAST_ADMIN / USER_DELETED | DONE |
| PUT | /api/v1/admin/users/{id}/roles | ADMIN: `{roles: [...]}` replaces the set; 404 ROLE_NOT_FOUND; 409 own-ADMIN removal / LAST_ADMIN / USER_DELETED | DONE |
| DELETE | /api/v1/admin/users/{id} | ADMIN: soft delete + revoke all sessions → 204 (idempotent); 409 self / LAST_ADMIN | DONE |

`AuthResponse` = `{accessToken, refreshToken, tokenType: "Bearer", expiresIn: 1800, user: {id, email, username, fullname, roles}}`. The access token goes in `Authorization: Bearer …`. Access token claims: `iss`=techshopping, `sub`=userId, `username`, `roles`, `iat`, `exp`.

Swagger UI: `http://localhost:8080/swagger-ui/index.html` (dev profile only).

# 6. Tests & Verification

**Checkpoint 2.7 results (2026-09-25):**

| Check | Result |
|---|---|
| `node --check` on all frontend JS | OK |
| **Headless Edge E2E** `e2e-account.mjs` (repo root served on :5501, backend on the test profile + CORS override) | **31/31 PASS** (one earlier run failed only on a navigation race in the script itself; fixed and re-run) |
| Regression `e2e-frontend-layout.mjs` (register step with a new user `fe.e2e6`) | **32/32 PASS** |
| Select vs input font on the account form | both 13.33 px, same family; fixed from the first screenshot, where `font: inherit` made the select larger |
| Screenshots | account page (hero, summary bar, two form columns, shared header/footer) and the login error box after the CSS move: consistent with the site design |
| Leftovers | test DB users `fe.e2e4`, `fe.e2e5` (password now `MatkhauMoi@456`), `fe.e2e6`; one leftover refresh token of `fe.e2e5` was revoked by hand; Redis 0 `auth:*` keys; 0 ERROR lines |

The 31 account checks:
- **Shared CSS:** `.page-hero` is now styled on cart.html (120 px, #f3f3f3) and unchanged on products; the login error box keeps its style.
- **Access:** logged out, `customer/account.html` goes to `auth/login.html?redirect=customer/account.html`, and logging in returns there with the data loaded.
- **Loaded data:**
  - email and username are read-only and filled; fullname and phone are filled;
  - the summary shows username, 0 points, 0đ and the join date (d/m/yyyy);
  - the profile that registration created → empty form without the "no profile" hint;
  - the header name links to `customer/account.html`.
- **Account form:** an empty fullname → field error + general error; a save shows "Đã lưu thông tin tài khoản.", the header name changes immediately and `lahy_auth.user.fullname` is updated.
- **Profile form:**
  - a future date of birth → field error;
  - a Vietnamese profile saves, and after a reload all 8 values are still there;
  - clearing gender and date of birth is accepted (empty fields are sent as `null`).
- **Missing profile** (mocked 404 CUSTOMER_PROFILE_NOT_FOUND through the CDP Fetch domain): the page loads, shows the hint and an empty profile form.
- **Password:**
  - a wrong current password → "Mật khẩu hiện tại không đúng." on its field;
  - new = current → client error;
  - success → message, the session is cleared, redirect to `auth/login.html`;
  - afterwards the old password → INVALID_CREDENTIALS, and the new password logs in.
- **Disabled account** (mocked 403 ACCOUNT_DISABLED): message shown, content hidden, session cleared.
- **Expired session** (bad access token + bad refresh token): redirect to login with `?redirect=customer%2Faccount.html`, and the session is cleared.
- No missing static files and no JS exceptions, errors or warnings.

Scratchpad scripts added in 2.7: `e2e-account.mjs` (uses **CDP `Fetch.requestPaused` / `fulfillRequest`** to mock single API responses, including an `Access-Control-Allow-Origin` header) and `check-select.mjs`.

**Frontend restructure results (2026-09-25):**

| Check | Result |
|---|---|
| `node --check` on all `frontend/js/**/*.js` | all OK |
| Dev profile + new CORS (no data written) | preflight `OPTIONS /api/v1/auth/login` from `127.0.0.1:5501` and `localhost:5501` → 200 + Allow-Origin; `127.0.0.1:5500` → 200; `127.0.0.1:5502` → 403 |
| **Headless Edge E2E** `e2e-frontend-layout.mjs`, with the site served from the **repo root**, i.e. pages under `/frontend/…` exactly like Live Server on the workspace (backend on the test profile + CORS override for :5501) | **32/32 PASS**, listed below |
| Site served with `frontend/` as root (`/index.html`), checked with Edge `--dump-dom` | header and footer injected; links = `http://127.0.0.1:5502/…`; active item correct |
| Screenshot `customer/cart.html` | shared header + full footer render correctly; the pre-existing `.page-hero` styling gap was noticed (§7 #17) |
| Leftovers | new test DB user `fe.e2e3` ("Nguyễn Thị Giao Diện", `Matkhau@123`); Redis cleaned by `logout()` |

The 32 E2E checks:
- For each of the 6 pages with a header (index + 5 customer pages):
  - header and footer are injected, and no placeholder is left;
  - the header is a direct child of `<body>` and still `position: sticky`;
  - the correct active menu item is set (none for cart);
  - the full footer is present;
  - the header links resolve from the site root.
- Navigation: a nav click index → contact, and a logo click back.
- The index category card goes to `customer/products.html?category=phone`, and the filter is applied.
- `auth/register.html` has its styles; registration redirects to `/frontend/index.html` (not `auth/index.html`) with the name in the header, and the cart counter is updated after the layout loads.
- Logout from `customer/cart.html` works.
- `redirectToLogin()` from `customer/recommendation.html?x=1` goes to `auth/login.html?redirect=customer/recommendation.html?x=1`, and logging in returns there.
- The redirect guard rejects external, `//`, `../` and deep paths and allows `customer/cart.html`.
- Opening `auth/login.html` while logged in goes to the root index.
- No missing static files (CSS/JS/images/partials) and no JS exceptions, errors or warnings.

**Checkpoint 2.6 results (2026-09-25):**

The backend was not changed, so the 225 backend tests still hold (last run before commit `c9e2d6c`).

| Check | Result |
|---|---|
| `node --check` on all `frontend/js/*.js` | all OK |
| Static serving | a Node static server served `frontend/` on **5501** (5500 is taken, §7 #14); all 8 pages returned 200 |
| Backend for the run | profile **test** (no dev accounts created), random `JWT_SECRET`, `--app.cors.allowed-origins=http://127.0.0.1:5501,http://localhost:5501` |
| **Headless Microsoft Edge E2E** driven over the Chrome DevTools Protocol (`e2e-frontend-auth.mjs`) | **24/24 PASS**, listed below |
| Screenshots (`screenshots-auth.mjs`) | header logged out and logged in (name on black + "Đăng xuất" outlined), login error box, register field errors: consistent with the original design |
| Leftovers | test DB users `fe.e2e`, `fe.e2e2`; Redis 0 `auth:*` keys; 0 ERROR lines in the backend log |

The 24 E2E checks:
- `api.js` loads before `main.js`; the header shows "Đăng nhập" when logged out.
- Register:
  - client validation marks email / username / password / confirmPassword and stays on the page;
  - a successful registration with a Vietnamese name redirects to index, stores `lahy_auth`, and the backend normalizes email/username;
  - the header shows the full name + "Đăng xuất", and the cart counter still works.
- Tokens: a forced invalid access token → `apiRequest("/users/me", {auth: true})` refreshes, retries and succeeds, and the refresh token is rotated.
- Logout: clears storage, the header returns to "Đăng nhập", and the server revoked the refresh token (INVALID_TOKEN).
- Duplicate register → "Email này đã được đăng ký." on the email field, and the button is re-enabled.
- Login page:
  - the label reads "Email hoặc tên đăng nhập", and "Đăng ký" links to register.html;
  - a wrong password shows the Vietnamese INVALID_CREDENTIALS message;
  - an upper-case username with `?redirect=products.html` lands on products with the name in the header;
  - opening login.html while logged in → index.
- The redirect guard rejects `https://evil.example` and `//evil.example/x.html` and allows `cart.html`.
- No JS exceptions or `console.error` on any of the 8 pages. Browser network logs for intentional 4xx responses and the missing favicon are ignored.

**The E2E scripts live in the session scratchpad**, not in the repo: `static-server.mjs`, `e2e-frontend-auth.mjs`, `screenshots-auth.mjs` in `C:\Users\Quang\AppData\Local\Temp\claude\…\scratchpad\`. They may disappear. They use Node 24 (built-in `fetch` / `WebSocket`) and Edge at `C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe` with `--headless=new --remote-debugging-port`. Recreate them if needed, or ask the user whether to keep them in the repo, e.g. under `frontend/tests/`.

**Checkpoint 2.5b results (2026-09-25):**

| Check | Result |
|---|---|
| `mvnw clean test` | **BUILD SUCCESS: 225 tests, 0 failures, 0 errors** (197 + `AdminUserServiceImplTest` 19 + `AdminUserApiIntegrationTest` 9); 0 `Schema-validation`; Redis 0 keys; test DB unchanged (still only `e2e-admin` and `e2e.customer`) |
| curl on the running app (profile test) | anonymous `GET /admin/users` 401; CUSTOMER `e2e.customer` → 403 ACCESS_DENIED on GET and DELETE; Swagger lists `/api/v1/admin/users`, `/{id}` (GET + DELETE), `/{id}/status` PATCH, `/{id}/roles` PUT; 0 ERROR lines |
| Live ADMIN actions by curl | **not run**, per the 2.5b choice (b): no admin with a known password exists, and `e2e-admin` was not touched. They are covered by `AdminUserApiIntegrationTest` with a real admin user and real tokens |

**Checkpoint 2.5a results (2026-09-25):**

| Check | Result |
|---|---|
| `mvnw clean test` | **BUILD SUCCESS: 197 tests, 0 failures, 0 errors**; passed on the first run. 0 `Schema-validation`; Redis 0 keys afterwards; test DB rolled back |
| curl on the running app (profile test, `e2e.customer`) | anonymous `GET /users/me` 401; `GET /users/me` 200 with roles `["CUSTOMER"]` and Vietnamese fullname; `PUT /users/me` with `"email":"doi@example.com"` in the body → email unchanged, phone updated; `GET /profile` 200 (loyaltyPoints 0); `PUT /profile` 200 (Vietnamese city/ward saved); invalid gender + future dateOfBirth → 400 with both field details; change password with wrong current → INVALID_PASSWORD; correct → 200; old refresh token → INVALID_TOKEN; old password → INVALID_CREDENTIALS; new password → login OK; password changed back to `Matkhau@123` → login OK. Leftover Redis keys from the last login were deleted by hand; 0 ERROR lines |

**Checkpoint 2.4 results (2026-09-25):**

| Check | Result |
|---|---|
| `mvnw clean test` | **BUILD SUCCESS: 173 tests, 0 failures, 0 errors** (157 + `ProductSecurityIntegrationTest` 14 + `GlobalExceptionHandlerSecurityTest` 2); 0 `Schema-validation`; Redis 0 keys afterwards |
| curl on the running app (profile test + CORS origin `http://127.0.0.1:5500`, random `JWT_SECRET`) | anonymous: `GET /products` 200, `GET /categories` 200, `POST /brands` 401 UNAUTHORIZED, `GET /users/me` 401. CUSTOMER `e2e.customer`: login OK; `POST /brands` 403 ACCESS_DENIED; `DELETE /products/1` 403; `GET /products` 200; `GET /admin/users` 403. Preflight `OPTIONS /brands` (POST, authorization + content-type) → 200 with Allow-Origin. Swagger api-docs 200, UI 200, scheme `bearerAuth` = http/bearer/JWT. Logout 200, Redis 0 keys, 0 ERROR lines |
| ADMIN write path | covered by MockMvc with real ADMIN tokens (validation 400, service 404). **Not run by curl**: there is no ADMIN in the dev DB, and the test-DB admin's password is unknown |

**Checkpoint 2.3 results (2026-09-25):**

| Check | Result |
|---|---|
| `mvnw clean test` | **BUILD SUCCESS: 157 tests, 0 failures, 0 errors** (137 + `AuthServiceImplTest` 15 + `AuthApiIntegrationTest` 5); 0 `Schema-validation` |
| After the tests | `techshopping_test.users` unchanged (rolled back); Redis 0 keys |
| curl E2E on the running app (profile **test**, random `JWT_SECRET`; the dev DB was **not** touched) | register → 201 (email/username lower-cased, Vietnamese fullname stored correctly, roles `["CUSTOMER"]`); register again → 409 DUPLICATE_EMAIL; login with `E2E.CUSTOMER` → 200 (access token 303 chars, refresh 43 chars); JWT payload `{"iss":"techshopping","sub":"20","exp":iat+1800,"roles":["CUSTOMER"],"username":"e2e.customer"}`; Redis key TTL 604799 s, value `20`; wrong password → 401 INVALID_CREDENTIALS; refresh → new token; reused old refresh → 401 INVALID_TOKEN; logout → 200; refresh after logout → 401 INVALID_TOKEN; Redis 0 keys at the end; DB row: user + CUSTOMER + profile + `last_login`; 0 ERROR lines |
| Swagger `/v3/api-docs` | lists `/api/v1/auth/{register,login,refresh,logout}` |

**Checkpoint 2.2 results (2026-09-25):**

| Check | Result |
|---|---|
| `mvnw clean test` | **BUILD SUCCESS: 137 tests, 0 failures, 0 errors** (109 + 28 new security tests) |
| Hibernate schema validation | PASS; 0 `Schema-validation` lines |
| Dev startup **without** `JWT_SECRET` | fails as intended: "APPLICATION FAILED TO START … app.jwt.secret (environment variable JWT_SECRET) must be set and at least 32 bytes long" |
| Dev startup with a random 64-char `JWT_SECRET` | PASS (~5.5 s), 0 ERROR lines |
| CORS preflight `OPTIONS /api/v1/products` from `http://127.0.0.1:5500` (POST, authorization + content-type) | 200 with `Access-Control-Allow-Origin: http://127.0.0.1:5500`, Allow-Methods, Allow-Headers, Expose `WWW-Authenticate`, Max-Age 3600 |
| Preflight from `http://evil.example` | 403 |
| `GET /api/v1/products` without a token | 200 |
| `GET /api/v1/products` with `Bearer abc.def.ghi` | 401 + `WWW-Authenticate: Bearer error="invalid_token"…` + body `{"success":false,…,"error":{"code":"INVALID_TOKEN",…}}` |
| `/v3/api-docs`, `/swagger-ui/index.html` | 200 |
| Redis after the tests | 0 keys `auth:refresh:*` |
| Auth endpoints | NOT TESTED (not implemented until 2.3) |

**Checkpoint 2.1 results (2026-09-25):**

| Check | Result |
|---|---|
| `mvnw clean test` | **BUILD SUCCESS: 109 tests, 0 failures, 0 errors** (100 old + `AuthDataInitializerTest` 6 + `UserRepositoryTest` 3) |
| Hibernate schema validation (13 entities) | PASS; 0 `Schema-validation` lines in the test log |
| Dev startup (no `ADMIN_*`) | PASS (~5.2 s). Log: "Created role CUSTOMER/STAFF/ADMIN", then a WARN that the bootstrap admin was skipped. `GET /api/v1/products` → 200 |
| Bootstrap admin, end to end | ran the app on profile `test` twice with `ADMIN_*` = ` E2E-Admin@TechShopping.test ` / `E2E-Admin` / random password. Run 1 created user 4 `e2e-admin@techshopping.test` / `e2e-admin`, BCrypt hash (`$2a$10$`, 60 chars), role ADMIN. Run 2 created nothing (idempotent) |
| JWT / auth / Redis usage | NOT TESTED (not implemented yet; starts in 2.2) |

**Re-verified 2026-09-25 (start of session 2):** Docker containers started (`docker compose up -d`), `techshopping` and `techshopping_test` both have 35 tables, dev DB has 877 active + 2 soft-deleted products, `users` = 0 and `roles` = 0 rows, Redis PONG. `mvnw clean test`: exit 0, **100 tests, 0 failures, 0 errors** (same per-class counts as below). The E2E script was not re-run (it lived in the old scratchpad).

Results as of 2026-09-24, end of session 1:

| Check | Result |
|---|---|
| `mvnw clean test` | **BUILD SUCCESS: 100 tests, 0 failures, 0 errors** (re-run while writing this file) |
| Compile errors | none |
| Hibernate schema validation (`ddl-auto: validate`, 9 entities) | PASS; 0 `Schema-validation` errors |
| Spring Boot startup (dev) | PASS (~4.6 s), 0 ERROR lines in the log |
| PostgreSQL | PASS; both DBs have 35 tables |
| Redis | container answers PONG; **app usage NOT TESTED** (no code uses Redis yet) |
| Swagger UI / api-docs | PASS: HTTP 200, 42 operations, 9 query params shown on `GET /products` |
| E2E API script (63 steps, all 8 resources, 400/404/405/409 cases) | **63/63 PASS** |
| Search on real data | results match direct SQL counts (keyword, category, brand, price, isActive) |
| Runtime errors | none known |
| Security / auth | NOT TESTED (not implemented) |
| Load / performance | NOT TESTED (`GET /products` page ≈ 20–40 ms locally) |

**Test counts (current, 225):** Category 12, Brand 10, Product 14, Variant 17, Image 10, Attribute 7, AttributeValue 8, Specification 8, FilterSpecifications 9 (real DB), SlugUtil 4, contextLoads 1, AuthDataInitializer 6, UserRepository 3 (real DB), JwtProperties 4, JwtTokenService 6, RefreshTokenService 7, SecurityErrorHandlers 3, SecurityInfrastructureIntegration 6, RefreshTokenServiceRedisIntegration 2 (real Redis), AuthServiceImpl 15, AuthApiIntegration 5 (real DB + Redis), ProductSecurityIntegration 14 (8 parameterized + 6), GlobalExceptionHandlerSecurity 2, UserServiceImpl 12, UserApiIntegration 6, AdminUserServiceImpl 19, AdminUserApiIntegration 9. Changed in 2.5a: RefreshTokenService 7 → 10, RefreshTokenServiceRedisIntegration 2 → 3, UserRepository 3 → 5.

Tests need **both** Docker containers running (`docker compose up -d`): PostgreSQL `techshopping_test` for the JPA and `@SpringBootTest` tests, and Redis for `RefreshTokenServiceRedisIntegrationTest`.

# 7. Known Issues

1. **Timezone (environment).** Windows reports the JVM zone as `Asia/Saigon`, and PostgreSQL 16 rejects it on connect with `FATAL: invalid value for parameter "TimeZone"`.
   - Handled by `TimeZone.setDefault` in `TechApplication.main` and by surefire `argLine -Duser.timezone=Asia/Ho_Chi_Minh`.
   - **Running tests or the app from an IDE without Maven needs the VM option `-Duser.timezone=Asia/Ho_Chi_Minh`.**
2. **(Resolved in 2.4)** Security used to be wide open. Now catalogue writes need ADMIN (D4). Consequence: **there is no ADMIN in the dev DB yet**, so nobody can write catalogue data on dev until the user creates the bootstrap admin (§11).
3. **Import and E2E scripts are NOT in the repo.** They were written in session 1's temporary scratchpad (`C:\Users\Quang\AppData\Local\Temp\claude\...\scratchpad\`): `import-products.ps1`, `import-variants.ps1`, `import-attributes.ps1`, `link-variant-attributes.ps1`, `import-images.ps1`, `e2e-product-module.sh`. They may be gone in the next session. The dev data is already in the DB (a Docker volume), so nothing is lost unless the volume is reset.
   - The **session 2 browser test tools** (static server + headless Edge E2E scripts) were copied to **`docs/tools/e2e/`** at the end of session 2. `docs/` is untracked, so they stay local and out of git. See `docs/tools/e2e/README.md`.
4. **Test leftovers in the dev DB.** The API cannot remove them: products are soft-deleted only, and categories/brands that are still referenced cannot be deleted.
   - categories 10 `E2E Devices` and 11 `E2E Phones`; brand 56 `E2E Brand`;
   - soft-deleted products 878 `Laptop Test Checkpoint 2 v2` and 879 `E2E Phone X 12GB/256GB`, plus 1 image and 1 spec attached to 879.

   To clean up: `docker compose down -v && docker compose up -d`, then re-import (needs the scripts), or run a targeted SQL delete **with the user's approval**.
5. **Data quality of the crawled CSV** (data issues, not code bugs):
   - brands split apart ("MacBook", "iPhone (Apple)", "iPad (Apple)" separate from "Apple");
   - colors that differ only by case (`Vàng nhạt` / `Vàng Nhạt`) are separate attribute values;
   - the `rom` column in `tgdd_products_cleaned.csv` is wrong, because `extract_rom` in `Raw_data/clean_data.py` takes the first GB match, which is the RAM;
   - 116 products have no image; 11 have price 0.
6. **Design limitations (accepted for now):**
   - Keyword search uses `LIKE` on name/slug per word. The slug branch drops punctuation, so `100%` also matches "100W" products. There is no full-text search. The GIN index `idx_products_name` is not used.
   - `categoryId` filter matches that exact category only; subcategories are not included.
   - Attribute values sort alphabetically, so "12 GB" comes before "3 GB".
   - Deleting the primary image leaves the product without a primary image; no automatic re-selection.
   - The rule "a variant has at most one value per attribute" is enforced in the service. It is **not** a DB constraint.
7. **`.gitkeep` files.** The deletions of the backend `.gitkeep` files were committed by the user in `21d2cda`. `security/.gitkeep` and `service/.gitkeep` still exist.
8. **`JWT_SECRET` is now required in every profile except `test`.** The dev app will not start without it (see §11). Running from an IDE also needs this env var.
9. **Verification accounts in the test DB** (`e2e-admin`, `e2e.customer`, `fe.e2e` … `fe.e2e6`, see §3). They are harmless, but `techshopping_test` already has an ADMIN. Do not write tests that assume "no ADMIN exists" in the test DB; mock the repositories instead.
10. **No ADMIN in the dev DB yet** (dev `users` = 0 at the end of session 2). It is created the first time the user starts the app with `ADMIN_*` set (§11). The admin can later change the password with `PUT /api/v1/users/me/password` or on the account page (`customer/account.html`).
11. **Frontend issues found in the analysis** (not fixed; they belong to later phases):
    - `js/customer/recommendation.js` points at the missing image `../assets/images/smartphone.jpg`;
    - on `customer/products.html`, `data-name` differs from the shown name (e.g. the "Laptop Dell" card adds "Laptop Pro 14" to the cart). This goes away when 2.8 renders the cards from the API;
    - `css/customer/services.css` and `js/customer/services.js` are empty and not loaded.
12. **Behaviour of the Spring Security 7 resource server** (expected; not bugs):
    - a request with an invalid or expired bearer token gets **401 even on public endpoints**, so the frontend must drop or refresh a bad token (2.6);
    - authentications carry an extra authority **`FACTOR_BEARER`** next to the `ROLE_*` ones (MFA support). Use `hasRole`/`hasAuthority("ROLE_…")`, and do not assert the exact authority list in tests;
    - the 401 `WWW-Authenticate` header contains `resource_metadata="…/.well-known/oauth-protected-resource"`. That path is not served (it falls under `denyAll`); this is harmless for our clients.
13. **Known limits of the auth design** (accepted, as in D3):
    - **Access tokens are stateless.** A user who is disabled, deleted or given new roles keeps the old access token for up to 30 min. Refresh is refused immediately, because refresh re-checks the user.
    - Refresh tokens are issued inside the DB transaction. If a commit failed afterwards, an orphan Redis key would remain until its TTL ends. It is harmless, because refresh re-loads the user.
    - Since 2.5a there is a per-user refresh-token index (`revokeAll`), used by password change. There is still no "log out everywhere" endpoint; `/auth/logout` revokes only the token that was sent. The index is not atomic with the token keys, and a stale hash in the set is harmless.
    - `/users/me*` re-checks the account on every call. Other authenticated endpoints only trust the token until they do the same (future modules should load the user through a similar check).
    - There is no rate limiting on `/auth/login` yet (planned for phase 10).
14. **Port 5500 is occupied on the user's machine** (found 2026-09-25 in 2.6):
    - `Get-NetTCPConnection -LocalPort 5500` shows `TNSLSNR` (Oracle TNS Listener; 5500 is the Oracle EM Express default) listening on `::`;
    - binding 127.0.0.1:5500 fails with EACCES, so **VS Code Live Server cannot use 5500 either**;
    - **Resolved (user decision 2026-09-25):**
      - use Live Server on port **5501** (`liveServer.settings.port`, a per-user VS Code setting; `.vscode/` is git-ignored);
      - `application-dev.yml` CORS now lists 5500 **and** 5501 for both `127.0.0.1` and `localhost`;
      - verified on the dev profile: preflight from :5501 and :5500 → 200 with Allow-Origin, :5502 → 403;
      - this yml change is **not committed yet**.
15. **Frontend line endings:** the files edited in 2.6 were written with LF; the originals were CRLF. Git normalizes (`core.autocrlf`), so diffs are clean, and git only warns "LF will be replaced by CRLF".
16. **No favicon** in `frontend/` (pre-existing): browsers log a 404 for `/favicon.ico`. It is harmless.
17. **`.page-hero` is only styled on the products page** (pre-existing, found in the restructure screenshots):
    - `.page-hero` is defined only in `css/customer/products.css`;
    - `cart.html`, `contact.html`, `recommendation.html` and `services.html` also use `<section class="page-hero">` but do not load that CSS, so their title block has no grey background and no 120px padding (the title sits right under the header);
    - this was already so in the teammate's commit;
    - **Fixed in 2.7:** the rules (including the `@media ≤ 700px` h1 size) were moved to `style.css`. The user answered "duyệt" to the message that proposed it. Cart, contact, recommendation and services now have the same grey 120 px hero as products. This is easy to revert if the user prefers the old look.
18. **Old E2E script paths:** the scratchpad `e2e-frontend-auth.mjs` (2.6) uses the old flat paths (`/login.html` …) and is superseded by `e2e-frontend-layout.mjs`, which uses the new structure.

# 8. Important Decisions

- **The SQL file is the source of truth.** Entities map it exactly. There is no Hibernate DDL (`ddl-auto: validate` in every profile). Never modify `database/techshopping.sql`, or DROP/DELETE data, without the user's approval. If the schema looks wrong: stop and report the file, line, table, column, problem, impact and proposal.
- **Work in checkpoints.** After each checkpoint: compile, test, check Hibernate validation, report, then **stop and wait for the user**. The user approves each step explicitly, and does not want the next module started automatically.
- **DTOs are Java `record`s** with validation on the components. Entities are normal classes with Lombok `@Getter @Setter @NoArgsConstructor`: no `@Data`, and no equals/toString over relations.
- **Relations** are always `@ManyToOne(fetch = LAZY)`. There are no JPA cascades; the DB `ON DELETE CASCADE` does that work. Collections are not mapped; they are loaded with repository queries or `@EntityGraph`, which avoids N+1.
- **Join table without extra columns** (`variant_attribute_values`): mapped as an entity with `@EmbeddedId` + `@MapsId`, as the user requested, instead of `@ManyToMany`.
- **Mappers are hand-written** (no MapStruct). They map simple fields only; the service resolves relations, slugs and SKUs.
- **Product delete is a soft delete** (`deleted_at` + `is_active = false`). Soft-deleted products, and their variants/images/specs, are treated as 404. Unique checks (slug, sku) still include soft-deleted rows, because the DB constraint does.
- **Other deletes are hard deletes, guarded by the service:** category (no children, no products), brand (no products), attribute (no values) → 409 `RESOURCE_IN_USE`. `DataIntegrityViolationException` falls back to 409.
- **Slugs** are auto-generated from the name when blank (`SlugUtil`: Vietnamese diacritics removed, `+`→`plus`). A client-provided slug must match `^[a-z0-9]+(-[a-z0-9]+)*$`.
- **Blank SKUs** (product and variant) are stored as NULL, so they don't collide on the UNIQUE constraint.
- **Update semantics:** PUT replaces fields. For `isActive`, `stockQuantity`, `warrantyMonths` and `isPrimary`, null means "unchanged". The owner (product/attribute) of a child record cannot be changed by update.
- **Read-only product counters:** `rating`, `total_reviews` and `view_count` are never set from requests.
- **Primary image:** at most one per product, enforced in the service within one transaction.
- **Pagination:** list endpoints return `PageResponse`. `@PageableDefault(size = 20, …)` must be set explicitly, because `@PageableDefault` otherwise overrides the yml default with 10. The maximum is 100. Unknown sort property → 400.
- **Response wrapper** is named `ApiResult` (not `ApiResponse`) to avoid clashing with Swagger's `@ApiResponse`.
- **Security** uses Spring Security's OAuth2 resource server (Nimbus, HS256). There is **no** custom `JwtAuthenticationFilter` and **no** jjwt (D2). URL rules live in `SecurityConfig`. New modules must add their paths deliberately: anything under `/api/v1/**` that is not listed requires authentication, and anything outside `/api/v1` is denied. Use `@PreAuthorize` (method security is enabled) for per-endpoint rules such as "own data or ADMIN".
- **Phase 2 decisions D1–D6** (user-approved 2026-09-25) are listed in §10.4 and are binding.
- **Accounts:** email and username are stored trimmed + lower-case. Passwords use BCrypt, min 8 chars. Users are soft-deleted. The first ADMIN comes only from env vars; passwords and secrets are never put in source/yml.
- **Frontend:** keep developing the existing `frontend/` (plain HTML/CSS/JS). Do not rewrite it or switch to a framework. When the frontend and the API disagree, change the frontend. Report mismatches that would affect the architecture or the API before changing anything.
- **Redis:** used for refresh tokens (keys `auth:refresh:<sha256>` and `auth:user-refresh:<userId>`). Add `@Cacheable` (products/categories/brands) and rate limiting later (phase 10), only if it helps.
- **Future AI services** (Recommendation, Chatbot) will be Python/FastAPI and will **not** access the business DB directly. They go through Spring Boot APIs.

# 9. Current Git State

Checked 2026-09-25, after Checkpoint 2.7.
- **Working branch: `quang`** (local only, no upstream). **Nothing has been pushed.**
- History (newest first):
  - **`c9e2d6c` "Add Phase 2 backend: user accounts, JWT authentication and authorization"**: committed by Claude at the user's request. Contents: exactly the 74 files under `backend/` of checkpoints 2.1–2.5b. Not included: `docs/` and `frontend/`.
  - `2619767`: merges `origin/main` (frontend) into `quang`.
  - `21d2cda` "Backend product and add database" (the user, Phase 1). On local `main`, not pushed: `main` is ahead 1 / behind 1 of `origin/main`.
  - `f58f92d` "Frontend của khách hàng" (HoangPhuoc38): `frontend/`, on `origin/main`.
- Remotes: `origin/main` (= `f58f92d`), `origin/Yle`.
- **Not committed (checked after 2.7):**
  - **staged** (`git add -A frontend/` after the restructure, for rename detection): Checkpoint 2.6 + the whole restructure;
  - **not staged**:
    - 2.7 changes on already-staged files (`css/style.css`, `css/auth/login.css`, `css/customer/products.css`, `js/core/main.js`);
    - untracked `frontend/customer/account.html`, `frontend/js/customer/account.js`, `frontend/css/customer/account.css`;
    - `backend/Tech/src/main/resources/application-dev.yml` (CORS +5501).
  - When the user asks to commit: `git add -A frontend/ backend/Tech/src/main/resources/application-dev.yml`, check `git status` / `git diff --cached`, then commit. Suggested message: "Frontend: auth pages, role-based structure, shared layout, account page".
- `docs/` stays **untracked** (user decision 2026-09-25).
- Git-ignored, written by a VS Code extension: `.github/modernize/`, `backend/Tech/.github/`.
- **Do NOT commit or push** unless the user explicitly asks. The user has not answered the last "commit?" question: they only replied "duyệt" (approve) to the message that asked it together with 2.7.

# 10. EXACT NEXT STEP

## 10.1 Status

| Step | Status | Notes |
|---|---|---|
| Phase 2 inspection report, decisions D1–D6, frontend analysis (A–H) | **DONE** | §10.3, §10.4 |
| Commit Phase 1 | **DONE by the user** | `21d2cda` |
| **Checkpoints 2.1 – 2.5b** (the whole Phase 2 backend) | **DONE**, approved, **committed `c9e2d6c`** | 225/225 tests |
| **Checkpoint 2.6** (frontend auth) | **DONE** | Edge E2E 24/24 |
| Port 5500 / CORS | **DONE** (user chose 5501) | dev yml updated, uncommitted |
| **Frontend restructure** (role folders, mirrored JS/CSS, header/footer partials) | **DONE**, accepted (the user went on to approve 2.7) | Edge E2E 32/32 |
| **Checkpoint 2.7** (account page + `.page-hero` / form-message CSS moved to `style.css`) | **DONE and reported**; the user then stopped the session without approving it or answering the commit question | Edge E2E 31/31 + regression 32/32 (§6) |
| Checkpoint 2.8 (products from the API) | NOT STARTED | |

**How the open questions of the previous report were handled** (the user answered only "duyệt"):
- `.page-hero` fix → applied (it was the proposal, and the account page needs it; easy to revert);
- commit → **not done**, waiting for an explicit request;
- E2E scripts in the repo → **not added** (they stay in the scratchpad).

**2.7 implementation notes:**
- `account.js` loads `/users/me` and `/users/me/profile` in parallel (both `auth: true`; concurrent 401s share one refresh);
- `handleSessionError`: 401 → `clearAuth` + `redirectToLogin`; ACCOUNT_DISABLED → message + `clearAuth` + hide the content;
- empty profile fields are sent as `null` (the backend `@Pattern` on gender rejects `""`);
- after `PUT /users/me` the header name and `lahy_auth.user.fullname` are updated without a reload;
- after a password change: `clearAuth()`, then after 1.5 s → `siteUrl("auth/login.html")` (no `?redirect`, so the user lands on index after logging in again);
- the account page has no menu item (`data-active="account"` matches nothing), and it is reached through the header name.

## 10.2 EXACT NEXT STEP

**At the start of the next session:**
1. read `docs/PENDING_WORK.md` (the open work, in order, with details);
2. check that the repo still matches §9;
3. ask the user **(a)** to approve 2.7 and **(b)** whether to commit the uncommitted frontend + `application-dev.yml`.

**Only then implement Checkpoint 2.8: product listing from the API**, without backend changes (user decision). Details below and in `PENDING_WORK.md`.

1. **`frontend/customer/products.html`:**
   - remove the 8 hardcoded `.product-card`s but keep the `.filter-bar` buttons (`data-category` = `all` / `laptop` / `phone` / `tablet` / `accessory`) and an empty `.product-grid`;
   - add a "Xem thêm" button in a `.view-all` block (existing class), plus loading / empty / error messages;
   - keep the look.
2. **`frontend/js/customer/products.js`:**
   - `GET /api/v1/categories?size=100` (public, **no** `auth`) → map the button slugs to DB slugs: `laptop` → `laptop`, `phone` → `dien-thoai`, `tablet` → `may-tinh-bang`, `accessory` → `phu-kien`; then to `categoryId`. The DB also has `dong-ho-thong-minh` and `dong-ho-thoi-trang` (shown only under "Tất cả") and 2 E2E categories with 0 products;
   - `GET /api/v1/products?categoryId=&isActive=true&page=&size=12&sort=id` → render cards using the existing markup (`.product-card` > `.product-image` img + `.product-info` with `.product-category` = `categoryName`, h3 = name, `.product-price` = `formatPrice(discountPrice ?? basePrice)`, `.add-cart` with `data-name` / `data-price`);
   - images: for each card, `GET /api/v1/products/{id}/images` → the primary image or the first one; if there is none (116 products) or it fails → a local fallback by category, e.g. `../assets/images/laptop.png`, `phone.png`, `tablet.png`, `banphim.png`;
   - keep the `?category=` URL behaviour (links from the index category cards);
   - "Xem thêm" → next page;
   - after rendering, call `setupAddToCart(grid)`: change `setupAddToCart` in `js/core/main.js` to accept a root (default `document`) so new cards get the listener without double-binding.
3. **`frontend/index.html` featured grid** ("SẢN PHẨM NỔI BẬT"): replace the 4 hardcoded cards with the first 4 active products from the API (same card renderer). Consider a small shared helper, e.g. `js/core/products-ui.js`, used by both pages, **or** keep the renderer in `products.js` and add a tiny `home.js` part; pick the simpler option and document it.
4. **Things that stay unchanged:** the cart (localStorage, keyed by name, until Phase 3), recommendation, contact.
5. **Verify:**
   - the backend on the **dev** profile is fine here: only GETs, and the dev DB has the 877 real products;
   - headless Edge E2E: the grid renders 12, "Xem thêm" adds 12, each filter button shows only that category (compare counts with `GET /products?categoryId=`), `?category=phone` preselects, fallback images for products without images, add-to-cart works on rendered cards, the index featured grid shows 4, no JS errors; screenshots;
   - check the network: requests stay reasonable (12 image calls per page).
6. Update this file. **Stop and report.** Phase 2 then ends: suggest a final commit and a Phase 2 summary.

The user-approved plan is also saved outside the repo at `C:\Users\Quang\.claude\plans\pasted-content-id-5fa4-t-i-mu-n-cached-biscuit.md`.

**The Phase 2 backend is complete and committed (`c9e2d6c`); frontend 2.6, the restructure and 2.7 are done (uncommitted); next is 2.8, the last Phase 2 checkpoint.**

**Do NOT:**
- modify `database/techshopping.sql`, or change/delete data, without approval;
- start Cart/Order;
- rewrite the existing frontend pages (only targeted edits, as in 2.6);
- rewrite the frontend or add a framework;
- commit or push without an explicit request.

## 10.3 Phase 2 inspection summary (read from `database/techshopping.sql` lines 9–56 and 467–470)

**Tables**
- `roles`: `role_id` serial PK; `name` varchar(50) UQ NN; `description` text; `created_at` timestamp default now.
- `users`: `user_id` bigserial PK; `email` v120 UQ NN; `username` v50 UQ NN; `password_hash` v255 NN; `fullname` v120 NN; `phone` v20; `avatar_url` text; `is_active` boolean default true (**nullable**); `last_login`; `created_at` / `updated_at` default now; `deleted_at`. Indexes `idx_users_email`, `idx_users_username` (redundant with the UQ indexes; observation only), `idx_users_is_active`.
- `user_roles`: PK (`user_id` bigint NN FK users CASCADE, `role_id` int NN FK roles CASCADE); `assigned_at` default now.
- `customer_profiles`: `customer_id` bigint PK = FK users CASCADE; `date_of_birth` date; `gender` v10 (no CHECK); `address` v255; `city` / `district` / `ward` v100; `postal_code` v20; `default_shipping_address` text; `loyalty_points` int default 0; `total_spent` decimal(15,2) default 0; `created_at` / `updated_at`.
- No CHECK constraints, triggers or seed rows for this group. The view `v_customer_stats` reads `users` + `customer_profiles` and filters out `deleted_at`.
- Future tables that point at `users`: `carts` (UQ, CASCADE), `employees` (UQ, no cascade), `orders`, `warranty_requests`, `maintenance_requests`, `return_requests`, `user_interactions`, `recommendations`, `chat_sessions`, `support_tickets` (no cascade). Once orders exist, a hard delete of a user will fail, so **users are soft-deleted** (`deleted_at` + `is_active = false`).

**Issues and risks** (✔ = handled in 2.1)
1. ✔ No role rows in the SQL → `AuthDataInitializer` (D1).
2. The UNIQUE on email/username also covers soft-deleted users, so a deleted user's email cannot register again. This matches the Product slug policy.
3. ✔ The PostgreSQL UNIQUE is case-sensitive → email/username are stored trimmed + lower-case (initializer done; register in 2.3).
4. ✔ `is_active` is nullable → the entity defaults it to `true`.
5. `GlobalExceptionHandler` has a catch-all `@ExceptionHandler(Exception.class)`, so `AccessDeniedException` / `AuthenticationException` from `@PreAuthorize` would become 500 → handled in 2.2 / 2.4.
6. There is no refresh-token table → Redis (D3), in 2.2.
7. `app.jwt.secret` exists only in prod yml. Dev and test need their own values (≥ 32 bytes) → 2.2.
8. The frontend runs on another origin → CORS (2.2). Its comment uses `/api/auth/login` → the frontend will be changed to `/api/v1/auth/login` (2.6).
9. The "STAFF" role and the `employees` table (Phase 7) are separate: Phase 2 only creates the role.

**Backend files planned (2.2–2.5):**
- security: `JwtProperties`, `JwtConfig`, `JwtTokenService`, `RefreshTokenService`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`;
- auth DTOs `dto/request/auth/{RegisterRequest, LoginRequest(identifier, password), RefreshTokenRequest}`, `dto/response/auth/{AuthResponse(accessToken, refreshToken, tokenType, expiresIn, user), AuthUserResponse}`;
- `mapper/user/UserMapper`;
- `service/auth/AuthService` + `service/impl/auth/AuthServiceImpl`, `controller/auth/AuthController`;
- user DTOs `dto/request/user/*` and `dto/response/user/*`, `service/user/UserService` + impl, `controller/user/{UserController, AdminUserController}`;
- modified: `pom.xml`, `SecurityConfig`, `ErrorCode`, `GlobalExceptionHandler`, `OpenApiConfig`, `application-{dev,test,prod}.yml`.

**Planned endpoints:**
- `POST /api/v1/auth/{register,login,refresh,logout}`;
- `GET/PUT /api/v1/users/me`, `PUT /api/v1/users/me/password`, `GET/PUT /api/v1/users/me/profile`;
- ADMIN: `GET /api/v1/admin/users` (paginated), `GET /api/v1/admin/users/{id}`, `PATCH .../{id}/status`, `PUT .../{id}/roles` (only an ADMIN can assign STAFF/ADMIN), `DELETE .../{id}` (soft delete + revoke refresh tokens).

### Frontend analysis (2026-09-25; `frontend/` by HoangPhuoc38)

- **Structure:**
  - 7 pages;
  - `css/`: shared `style.css` (750 lines) + one file per page; `services.css` is empty;
  - `js/`: shared `main.js` + one file per page; `services.js` is empty;
  - `assets/images/` (11 images);
  - every page except login has the same header (logo, nav, `.cart-count`, `.login-btn`) and footer; font Montserrat.
- **Pages:**
  - `index.html`: hero, categories, 4 hardcoded featured products, AI teaser, services;
  - `products.html`: filter buttons `laptop` / `phone` / `tablet` / `accessory` + 8 hardcoded cards;
  - `cart.html`;
  - `login.html`: no header, does not load `main.js`;
  - `recommendation.html`, `services.html` (static), `contact.html`.
- **What works today (client-side only):**
  - `main.js`: cart in localStorage `lahy_cart` (`getCart`, `saveCart`, `addToCart(name, price)` keyed by *name*, `updateCartCount`), `setupAddToCart` for `.add-cart`, `formatPrice` (vi-VN);
  - `products.js`: filter + `?category=`;
  - `cart.js`: +/−, remove, checkout alert;
  - `login.js`: demo alert;
  - `recommendation.js`: keyword mock;
  - `contact.js`: alert;
  - `home.js`: scroll animation.
- **API calls: none.** The only one is a comment in `login.js`: `http://localhost:8080/api/auth/login`.
- **Mismatches with the backend** (fix on the frontend):
  - `login.js` → `POST /api/v1/auth/login {identifier, password}`;
  - `login.html`: the field is `type=email` "Email", but D5 allows email or username; the "Đăng ký" link is `#` → `register.html`;
  - the product pages are hardcoded, while the backend has `GET /api/v1/products?categoryId=&page=&size=`. The filter buttons need a map: frontend `laptop` / `phone` / `tablet` / `accessory` → DB slugs `laptop` / `dien-thoai` / `may-tinh-bang` / `phu-kien`, then to ids via `GET /api/v1/categories`. The DB also has 2 watch categories that have no button;
  - `ProductResponse` has **no image URL**: load one `GET /api/v1/products/{id}/images` per card, and use a local image of the same category if there is none (user decision: do not change the backend);
  - the backend has no CORS yet (2.2).
- **Not changed in Phase 2:** cart logic (Phase 3), recommendation (Phase 8), contact form (Phase 9), the "Quên mật khẩu" link (no reset flow planned).
- **Reuse:**
  - `.login-wrapper` / `.login-box` / `login.css` for register;
  - header, footer, `.page-hero`, `.section`, `.container`, `.btn*`, and `.contact-form` / `contact.css` for the account page;
  - `.product-grid` / `.product-card` / `.product-info` / `.add-cart` as the card template;
  - `.filter-btn` and `.view-all` for "Xem thêm";
  - `formatPrice`, `addToCart`, `updateCartCount`;
  - the render-then-bind pattern of `recommendation.js`.
- **Frontend files to modify (2.6+):**
  - `js/login.js`, `login.html`;
  - `js/main.js`: `renderAuthState()` for `.login-btn`; `setupAddToCart(root = document)`;
  - the 6 header pages: only add `<script src="js/api.js">` before `main.js`;
  - `js/products.js`, `products.html` (remove the hardcoded cards, keep the grid and filter), `index.html` (featured grid from the API);
  - `css/login.css` (error box), `css/products.css` (loading / empty states).
- **Frontend files to create:**
  - `js/api.js`: base URL `http://localhost:8080/api/v1`; `apiRequest` unwraps `ApiResult`; tokens in localStorage `lahy_auth`; on a 401, refresh once and retry; logout;
  - `register.html` + `js/register.js`;
  - `account.html` + `js/account.js`;
  - `css/account.css` only if the existing CSS is not enough.

## 10.4 Phase 2 decisions (answered by the user on 2026-09-25; binding)

| # | Decision |
|---|---|
| D1 | Roles CUSTOMER / STAFF / ADMIN are created at startup if missing. The first ADMIN comes from env `ADMIN_EMAIL`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`; the password is hashed with `PasswordEncoder`; no hardcoded password; no second admin if one exists; `techshopping.sql` is not changed. **Implemented in 2.1.** |
| D2 | `spring-boot-starter-oauth2-resource-server` (Nimbus JWT, supported by Spring Security). **No jjwt.** |
| D3 | Access token 30 min. Refresh token 7 days, stored in Redis, **rotated** on refresh (the old one becomes invalid), revoked/deleted on logout. No schema change. |
| D4 | GET public; POST/PUT/DELETE ADMIN only. Applies to **all 8 Product resources** (categories, brands, products, product-variants, attributes, attribute-values, product-images, product-specifications). Enforced by JWT once auth is complete (2.4). |
| D5 | `/api/v1/auth/register`, `/login`, `/refresh`, `/logout`. Login with email **or** username. |
| D6 | Register creates User + UserRole CUSTOMER + CustomerProfile in **one transaction**. STAFF/ADMIN cannot self-register; only an ADMIN can assign them. |
| Other | The frontend runs with VS Code Live Server (originally `:5500`; on this machine **`:5501`**, because Oracle holds 5500; see §7 #14). The product pages are connected to the API **without backend changes**. Work on branch `quang`. `docs/` stays untracked. The backend auth comes first; frontend integration starts after the Auth API is tested. |

# 11. Instructions for Next Claude Session

"Before doing anything, read docs/PENDING_WORK.md (open work) and docs/CLAUDE_CONTEXT.md, and inspect the current codebase.

Do not assume previous work that is not reflected in the code.

Continue from EXACT NEXT STEP.

Do not redo completed work.

Do not modify unrelated modules.

After making changes, run the appropriate tests/build and update docs/CLAUDE_CONTEXT.md again."

Practical notes:
- Build and test from `backend/Tech` with `mvnw.cmd clean test` (PowerShell) or `./mvnw clean test` (Git Bash). Run the app with `mvnw spring-boot:run`, dev profile, port 8080. Docker containers `techshopping-postgres` and `techshopping-redis` must be up. If Docker Desktop is not running, start it first (`C:\Program Files\Docker\Docker\Docker Desktop.exe`), then run `docker compose up -d`.
- **Creating the dev ADMIN** (one time; the user picks the values):
  - PowerShell: `$env:ADMIN_EMAIL="..."; $env:ADMIN_USERNAME="..."; $env:ADMIN_PASSWORD="..."; .\mvnw.cmd spring-boot:run`;
  - Git Bash: `ADMIN_EMAIL=... ADMIN_USERNAME=... ADMIN_PASSWORD=... ./mvnw spring-boot:run`.

  The password needs at least 8 characters. After the first run the variables are no longer needed. Since 2.2, `JWT_SECRET` (≥ 32 bytes) is also **required** in dev (see below).
- **`JWT_SECRET` (required since 2.2 to run the app outside tests):** use at least 32 random bytes and keep the same value between restarts, otherwise issued tokens become invalid. Never commit it.
  - PowerShell: `$env:JWT_SECRET = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Max 256 }))`.
  - Git Bash: `export JWT_SECRET=$(head -c 48 /dev/urandom | base64)`.
  - IntelliJ / VS Code run configs need it as an environment variable too.
  - Claude's own verification runs use a throwaway random secret for each run.
- **Frontend:** open it with VS Code Live Server on port **5501**: in the user's VS Code settings, `"liveServer.settings.port": 5501`. Port 5500 is taken by Oracle. Start the backend first (dev profile + `JWT_SECRET`). Pages need HTTP because of the header/footer partials. Entry page: `frontend/index.html`; login at `frontend/auth/login.html`.
- To stop an app started in the background: find the process that listens on 8080 (`Get-NetTCPConnection -LocalPort 8080 -State Listen`) and `Stop-Process` it.
- Git Bash heredocs that contain Vietnamese characters sometimes fail to parse. Use the Write/Edit tools for files with Vietnamese text.
- Windows PowerShell 5.1 reads `.ps1` files without a BOM as ANSI, which corrupts Vietnamese literals. Save scripts as UTF-8 **with BOM**.
- Send JSON with Vietnamese text from a UTF-8 file (`curl --data-binary @file.json`), not inline on the Git Bash command line.
- The user communicates in Vietnamese; reply in Vietnamese.
