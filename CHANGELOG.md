# Changelog

<!-- Newest entries first. Dates are placeholders — fill in actual timing where known. -->

## [Unreleased]
### Planned
- Automated test coverage for cart/checkout
- OpenAPI/Swagger documentation polish
- Optional: move admin controllers into a dedicated `controller.admin` package

## [2026-08-19] Stateless auth migration (JWT + CSRF) and EMPLOYEE role
### Added
- Migrated login from Spring Security session-based `formLogin` to stateless
  JWT (httpOnly cookie), motivated by per-user session memory cost on a
  512MB host and correctness under multiple concurrent instances
- Added `ROLE_EMPLOYEE` alongside existing `ROLE_ADMIN`/`ROLE_USER`, with
  admin-panel access split at both the URL level (`hasAnyRole`) and method
  level (`@PreAuthorize`) for admin-exclusive actions (custom route writes,
  folder deletion)
- Re-enabled CSRF protection (cookie-based, no server-side session required)
- Self-service password change for USER (`AuthController`) and EMPLOYEE
  (`AdminAccountController`, deliberately excluding ADMIN)
- Out-of-band admin password reset (local BCrypt hash generation + direct
  Supabase update) — keeps the highest-privilege credential off the HTTP
  surface entirely
### Fixed
- Three non-obvious CSRF/stateless-auth interaction bugs found during
  production testing: the CSRF cookie was never actually written (deferred
  token never resolved — fixed with `CsrfCookieFilter`), the default CSRF
  request handler didn't support the plain cookie→header pattern JS clients
  use (switched to `CsrfTokenRequestAttributeHandler`), and the CSRF token
  was silently rotating on every single authenticated request due to
  `SessionManagementFilter` treating stateless auth as a fresh login each
  time (fixed with `RequestAttributeSecurityContextRepository`)
- Fixed an `IllegalStateException` ("request object has been recycled") in
  visitor/session logging, caused by passing `HttpServletRequest` into
  `@Async` methods — now extracts plain values synchronously before the
  async handoff
- Fixed `visited_session` analytics cookie using a fixed 1-hour rolling
  window instead of expiring at the calendar-day boundary, which caused
  inconsistent unique-visitor counts relative to `DailyAnalytics`' daily
  buckets — now expires at midnight in the app's configured timezone
### Verified in production
- Cookie flags, httpOnly enforcement, back-to-back-write regression test,
  role separation, logout cookie clearing, and multi-device session
  independence — see docs/deployment.md
### Fixed (found during production verification)
- `AuthService.changePassword` looked up users by the wrong column
  (`username` instead of `userId`, the app's actual principal identifier) —
  broke password change for both USER and EMPLOYEE self-service paths
- Login-event analytics logging read `userName`/`userId` from request
  attributes that don't exist yet on the login request itself (they're only
  populated from an existing token, and login is what creates the token) —
  now reads directly from the already-fetched `User` entity
- `visited_session` cookie upgraded from a static `"true"` flag to a real
  per-session UUID, also used as the session log's `sessionId` — gives
  genuine same-day-visitor correlation instead of an always-null field
### Confirmed stable over multiple days
- ~3.75 days of continuous production uptime (see docs/deployment.md) show
  the metaspace fix holding: usage plateaued in a stable band well under
  the cap, no drift, no recurrence — the strongest evidence yet that
  incident 2 (below) is genuinely resolved, not just quiet in the short term

## [2026-08-14] Resource tuning: two separate memory incidents
### Fixed
- Resolved intermittent hard OOM kills (status 137) on Render free tier by
  bounding non-heap JVM memory (metaspace, direct memory, code cache,
  thread stacks), switching from G1GC to SerialGC, and capping Tomcat thread
  pool and Hikari connection pool sizes
- A second, distinct incident surfaced after the above fix: metaspace
  OutOfMemoryError after ~1 day of uptime. Root-caused (not just
  patched) using a purpose-built hourly JVM monitoring service
  (`MemoryUsageLog`) — confirmed as organic one-time class-loading growth
  plateauing, not a leak (flat `unloadedClassCount` ruled out a
  `spring-boot-devtools` classloader leak specifically). Fixed by raising
  `MaxMetaspaceSize` to a size matching the stack's genuine steady-state
  need; verified healthy via the same monitoring afterward
### Added
- `MemoryMonitorService` — hourly heap/metaspace/class-loading snapshots,
  kept running as ongoing health monitoring beyond the incident that
  motivated it

## [YYYY-MM-DD] Image pipeline rewrite
### Fixed
- Rewrote the JPEG/WebP upload pipeline: was re-decoding the source image at
  full resolution once per output size (up to 4x redundant decode cost);
  now decodes once, bounded via `ImageIO` subsampling regardless of source
  resolution, and chains progressively smaller variants from the previous
  output rather than the original
- Fixed unsafe WebP processing: replaced `Runtime.exec(String)` (naive
  whitespace tokenizing, shell-injection-adjacent) with `ProcessBuilder`
  (array-form arguments), added a timeout and output draining to prevent
  process hangs
### Changed
- WebP conversion moved to local-only processing — `cwebp` was failing under
  Render's free-tier resource limits; JPEG (now with the bounded/chained
  pipeline above) is the only format processed on the deployed server
- Unified the previously-duplicated JPG and WebP upload methods into one
  format-agnostic pipeline; fixed filename structure to the intended
  `[prefix_]core[_suffix].ext` (previously injected an unrelated random
  string into every filename regardless of naming mode)
- Decided: never upscale past the source's real resolution — a variant
  larger than the source is capped to the source's actual size rather than
  blown up or silently skipped

## [YYYY-MM-DD] Repurposed as portfolio project
### Changed
- Project scope shifted from learning exercise to CV/portfolio piece; began
  prioritizing reliability and code quality over feature breadth

## [YYYY-MM-DD] Initial feature set
### Added
- Storefront catalog (products, categories, collections, custom routes)
- Cart and checkout with guest/user merge and order snapshot pricing
- Session-based auth with role-based access (later migrated — see above)
- Blog system with Supabase-backed content
- Admin media management with image resize/convert pipeline
- Built-in visitor analytics (daily traffic, unique sessions)
