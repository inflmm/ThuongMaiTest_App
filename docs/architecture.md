# Architecture

## System Overview

```
[Client / Browser]
        |
        |  auth_token (httpOnly, Secure, JWT) + XSRF-TOKEN (JS-readable) cookies
        v
[Spring Boot App — Docker container on Render.com]
   |-- JwtAuthenticationFilter (stateless auth, runs before Spring Security's own filters)
   |-- CsrfCookieFilter (forces the deferred CSRF token to actually resolve into a cookie)
   |-- Thymeleaf views (admin panel, storefront pages)
   |-- REST controllers (/api/**)
   |-- VisitorInterceptor (analytics tracking)
        |
        |-- JPA / Hibernate --> [PostgreSQL (Supabase-hosted)]
        |-- Storage API     --> [Supabase Storage (bucket): images, blog content]
```

Runs either as a Docker container (Render, or locally via `docker build`/`docker
run`) or directly from an IDE (Eclipse) with the same environment variables.
Multiple instances can run concurrently — auth is stateless (no server-side
session to desync between instances), and cart/analytics state resolves
through the database rather than per-instance memory.

## Components

### Controller layer
REST controllers under `/api/**` (storefront, cart, checkout, admin) and
Thymeleaf-backed controllers for server-rendered admin/storefront pages.
Admin controllers use constructor injection throughout and are grouped
internally into `Reads` / `Writes` sections for consistency.

### Auth (`SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`)
- **Owns:** stateless JWT issuance/validation, CSRF protection, route-level
  and method-level (`@PreAuthorize`) authorization
- **Why the JWT is a cookie, not an Authorization header:** the admin panel
  is served as full server-rendered Thymeleaf pages (see `WebController`) —
  a plain browser page navigation has no way to attach a custom header, only
  a cookie rides along automatically. This is also why CSRF protection is
  mandatory here rather than optional: a cookie auto-attaches to *any*
  request to the domain, including ones a malicious page triggers.
- **Three roles:** `ROLE_ADMIN`, `ROLE_EMPLOYEE`, `ROLE_USER` — single role
  per user (`User.role`), no hierarchy bean wired (see Key Technical
  Decisions for why that's deliberate).

### CSRF stack — three cooperating pieces, each fixing a real bug
1. **`CookieCsrfTokenRepository.withHttpOnlyFalse()`** — stores the CSRF
   token in a JS-readable cookie instead of `HttpSession` (works without
   sessions).
2. **`CsrfCookieFilter`** — `CsrfFilter` stores the token as a *deferred*
   value; nothing actually writes the `XSRF-TOKEN` cookie until something
   calls `csrfToken.getToken()`. Nothing in this app's Thymeleaf templates
   does that (no `_csrf` form field references), so without this filter the
   cookie would never be written at all. This is Spring Security's own
   documented fix for SPA-style CSRF.
3. **`CsrfTokenRequestAttributeHandler`** (overriding the default
   `XorCsrfTokenRequestAttributeHandler`) — the default BREACH-protects the
   token via XOR-masking, which breaks a plain "copy the cookie value into a
   header" pattern. The plain handler makes that contract exact.
4. **`RequestAttributeSecurityContextRepository`**, wired into both
   `SecurityConfig` and `JwtAuthenticationFilter` — without this, every
   stateless request looks like a brand-new login to `SessionManagementFilter`
   (nothing persists between requests to say otherwise), which incorrectly
   fires `CsrfAuthenticationStrategy`'s token-rotation logic on *every*
   request — silently invalidating a token the client just read moments
   before using it.

### CartController / CartService
- **Owns:** add/update/remove item, guest cart handling, guest→user cart merge, cart count

### Checkout / Order flow
- **Owns:** converting a cart into an `Order` + `OrderItem` rows with snapshot pricing (protects order history if product price/name changes later)

### Product / Category / Collection management
- **Owns:** catalog CRUD, category tree (self-referential `parent`/`children`), many-to-many product↔collection, dynamic specifications, ordered image variants. Soft delete throughout.

### Media management (`ImageUploadService`)
- **Owns:** a single, format-agnostic pipeline for both JPEG and WebP output.
  Decodes the source exactly once, bounded to a max dimension via `ImageIO`
  subsampling (protects peak memory regardless of the source's native
  resolution — file size is a poor proxy for decode cost). Generates
  variants largest-to-smallest, chaining each smaller size from the previous
  output rather than re-decoding the original per size. Never upscales past
  the source's real resolution.
- **JPEG** runs on Render; **WebP** (via `cwebp`) is deliberately local-only
  — see docs/deployment.md.
- Filename structure is exactly `[prefix_]core[_suffix].ext` (prefix from
  the caller, core from the original filename/random string, suffix from
  the variant label).

### Blog system
- **Owns:** publish state, featured flag, slug lookup, search/filter, soft delete; article body stored file-backed, loaded into a `@Transient content` field for rendering

### Analytics (`VisitorInterceptor`, `SessionLogService`, `DailyAnalytics`, `UserSessionLog`)
- **Owns:** cookie-based session/visit tracking (`visited_session`, expires
  at midnight in the app's configured `Clock` zone — Asia/Ho_Chi_Minh — not
  a fixed rolling window, so it aligns with how `DailyAnalytics` buckets by
  calendar day), daily traffic + unique-session aggregation, admin dashboard
  read endpoints (shared ADMIN/EMPLOYEE access).
- **Async-safety fix:** `SessionLogService`'s logging methods take only
  plain values (`String`/`boolean`), never an `HttpServletRequest`. The
  caller (`VisitorInterceptor`) extracts everything it needs synchronously,
  on the original request thread, before handing off to `@Async` code —
  passing the request object itself into async code risks
  `IllegalStateException` once Tomcat recycles it for the next connection.

### Observability (`MemoryMonitorService`)
- **Owns:** an hourly snapshot of heap/non-heap/metaspace usage and
  class-loading counts, persisted to the database — built specifically to
  diagnose the metaspace incident below with real data instead of guesswork,
  and kept running afterward as ongoing health monitoring.

### AsyncConfig
- **Owns:** a small bounded task executor (core pool 2, max pool 4, queue 50), sized deliberately small to avoid unbounded thread growth on a constrained host

## Data Model

- **User** — login identity + single role (`ROLE_ADMIN`/`ROLE_EMPLOYEE`/`ROLE_USER`); unique on username/email/phone. Not a JPA relation from Cart/Order — those reference `userId` as a plain string.
- **Category** — self-referential hierarchy, soft-deletable
- **Product** — belongs to one `Category`; has many `ProductSpecification`, many `ProductImage`; many-to-many with `Collection`
- **CartItem** / **Order** / **OrderItem** — order lines store a pricing/name snapshot independent of live product data
- **Blog** — publish/featured flags, file-backed content, soft-deletable
- **DailyAnalytics** — `date`, `totalTraffic`, `uniqueSessions`
- **UserSessionLog** — session-level activity, including a `isCronPing` flag
- **MemoryUsageLog** — hourly JVM snapshot (heap/metaspace/class counts)
- **BaseEntity** — shared JPA auditing base (`created_time`/`updated_time`)

## Key Technical Decisions

| Decision | Choice | Alternative considered | Why |
|---|---|---|---|
| Auth model | Stateless JWT, httpOnly cookie | Session-based (`HttpSession`) | No per-user server memory on a 512MB host; correctness under multiple concurrent instances (sessions would desync without a shared store) |
| Role checks | Explicit `hasAnyRole("ADMIN","EMPLOYEE")` everywhere, no `RoleHierarchy` bean wired | A `ROLE_ADMIN > ROLE_EMPLOYEE` hierarchy | Making a hierarchy apply to both URL-level and `@PreAuthorize` checks requires explicit wiring into multiple expression handlers; getting that subtly wrong would silently misroute access. This also lets `AdminAccountController` cleanly express "EMPLOYEE only, not ADMIN" for free. |
| Admin password changes | Out-of-band: local BCrypt hash tool + direct Supabase update | A self-service web endpoint, same as EMPLOYEE/USER | Keeps the single highest-privilege credential's change path off the HTTP surface entirely — defense in depth against any future bug in that endpoint, not a response to a specific known flaw |
| GC algorithm | SerialGC | G1GC (JVM default) | Lower native memory overhead on a 512MB / 0.1 CPU host |
| Image decode | Bounded via `ImageIO` subsampling, then progressive chained downscale | Full-resolution decode per variant | File size doesn't predict decode cost; re-decoding the original per variant multiplies both cost and peak memory |
| WebP processing | Local-only, not deployed | Processing on Render | `cwebp` failed under Render's free-tier resource limits; local processing is a deliberate, documented split, not a workaround left in by accident |
| Pricing at checkout | Snapshot fields on `OrderItem` | Live join to `Product` at read time | Order history stays accurate even if product price/name changes later |
| Deletes | Soft delete (`deleted` flag) | Hard delete | Preserves records for admin recovery/audit |

## Known Limitations / Trade-offs

- **No JWT revocation mechanism.** A token stays valid until it naturally
  expires (default 1 hour), even if the password is changed or the account
  should be locked out immediately. Acceptable for now; would need a `jti`
  claim plus a server-side revocation list if immediate revocation becomes a
  requirement (reintroduces a small amount of server-side state).
- **No concept of distinct sessions/devices.** Plain JWTs with no `jti` are
  purely "valid or not" — there's no way to list or revoke one specific
  device's login without adding that same `jti` + revocation-list mechanism.
- **`User` is not a JPA relation from Cart/Order.** `userId` is a plain
  string on both sides — works, but loses DB-level referential integrity.
- **No caching layer.** Acceptable at current traffic; would revisit with a
  cache for the category tree and product listings if traffic grew.
- **WebP is local-only by design** (see Key Technical Decisions) — not a gap, but worth remembering it's not part of the deployed pipeline.
