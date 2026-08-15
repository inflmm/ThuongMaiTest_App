# Changelog

<!-- Newest entries first. Dates are placeholders — fill in actual timing where known. -->

## [Unreleased]
### Planned
- Automated test coverage for cart/checkout
- CSRF/validation hardening
- Structured logging + request correlation IDs
- Resolve/offload image processing so it works reliably on the Render free tier

## [2026-08-14] Resource tuning under production constraints
### Fixed
- Resolved intermittent OOM kills (status 137) on Render free tier by bounding
  non-heap JVM memory (metaspace, direct memory, code cache, thread stacks),
  switching from G1GC to SerialGC, and capping Tomcat thread pool and Hikari
  connection pool sizes.
### Known issue
- Image resizing/conversion (Thumbnailator) fails under Render's free-tier
  resource limits; currently processed on a stronger host instead.

### Changed
- Project scope shifted from learning exercise to CV/portfolio piece; began
  prioritizing reliability and code quality over feature breadth.

### Added
- Storefront catalog (products, categories, collections, custom routes)
- Cart and checkout with guest/user merge and order snapshot pricing
- Session-based auth with role-based access (`ROLE_ADMIN`, `ROLE_USER`)
- Blog system with Supabase-backed content
- Admin media management with image resize/convert pipeline
- Built-in visitor analytics (daily traffic, unique sessions)
