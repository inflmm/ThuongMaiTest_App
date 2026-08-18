# ThuongMai Ecommerce Backend

This is AI generated report

Demo links: [Homepage](https://thuongmaitest-app.onrender.com/homepage) | [Admin](https://thuongmaitest-app.onrender.com/admin/login) - employee account: user_guest | 123456

A Spring Boot ecommerce backend: storefront catalog, cart/checkout, role-based
admin (ADMIN/EMPLOYEE/USER), blog/content management, and visitor analytics —
built as a portfolio project and deployed under real resource constraints
(512MB RAM / 0.1 CPU free tier). Includes a full stateless JWT + CSRF auth
migration and a from-scratch diagnosis of two separate memory-exhaustion bugs
in production.

## Overview

A monolithic Spring Boot ecommerce backend covering the storefront (product
catalog, categories, collections, custom routes), cart and checkout,
stateless JWT authentication with three roles (ADMIN/EMPLOYEE/USER), an admin
panel for content and product management, a blog system with Supabase-backed
media storage, and lightweight built-in visitor analytics. What makes it
non-trivial: it was deployed and hardened on a genuinely memory-constrained
host (Render free tier), which forced real diagnostic work — two distinct
memory-exhaustion bugs (a hard OOM kill, then a metaspace cap) were found,
root-caused with production telemetry, and fixed — and a full migration from
session-based to stateless JWT authentication, including the non-obvious
CSRF/stateless interaction bugs that come with it.

## Tech Stack

- **Language / Build:** Java 17, Maven (`./mvnw`)
- **Backend:** Spring Boot 3.5.8, Spring Cloud BOM 2025.0.0, Spring Web MVC, Spring Data JPA, Spring Security 6
- **Auth:** Stateless JWT (jjwt 0.12.6), httpOnly cookie transport, cookie-based CSRF protection
- **Templating:** Thymeleaf + Thymeleaf Spring Security extras
- **Database:** PostgreSQL (Hibernate/JPA), H2 for tests
- **Storage:** Supabase Storage API (product images, blog article content)
- **API docs:** springdoc OpenAPI Starter WebMVC UI 2.6.0
- **Media processing:** Thumbnailator, imageio-core (JPEG); `cwebp` CLI (WebP, local-only — see Deployment)
- **Other:** YAUAA (user-agent parsing for analytics)
- **Deployment:** Docker, Render.com (free tier — 512MB RAM / 0.1 CPU); also runnable directly via Eclipse IDE

## Features

- **Storefront catalog** — product listing/detail, lookup by slug/ID, nested category tree, collection pages, custom route resolution
- **Product management** — admin CRUD with soft delete, images with variants/ordering, dynamic key/value specifications, stock and visibility control
- **Cart & checkout** — guest and registered cart support, guest-to-user cart merge, checkout converts cart into an order with snapshot pricing
- **Auth & authorization** — stateless JWT (httpOnly cookie), three roles (`ROLE_ADMIN`, `ROLE_EMPLOYEE`, `ROLE_USER`), CSRF-protected writes, self-service password change (USER and EMPLOYEE), out-of-band admin password reset
- **Blog system** — publish state, featured flag, slug lookup, search/filtering, soft delete, file-backed content
- **Media management** — bounded-memory image decode/resize pipeline (JPEG on Render, WebP local-only), progressive downscaling, Supabase bucket storage
- **Analytics** — request/session tracking interceptor, daily traffic + unique-session aggregation (day-boundary aligned to Asia/Ho_Chi_Minh), admin dashboard endpoints
- **Observability** — hourly JVM memory/class-loading snapshots logged to the database, used to diagnose both production memory incidents (see docs/deployment.md)
- **Admin content tools** — categories, collections, folders/files tree, custom routes CRUD (route writes are ADMIN-only)

## Architecture

Layered MVC (`controller` → `service` → `repository` → JPA entity), stateless
JWT auth via a custom filter (not Spring's session-based `formLogin`), and a
soft-delete pattern across most entities. See
[docs/architecture.md](docs/architecture.md) for the full component
breakdown, data model, role model, and the reasoning behind key decisions —
including several non-obvious CSRF/stateless-auth interaction bugs that are
worth reading before touching `SecurityConfig`.

## Running Locally

**Option A — Docker (matches production/Render setup):**
```bash
docker build -t thuongmai-app .
docker run -p 8080:8080 --env-file .env thuongmai-app
```

**Option B — Directly via IDE (e.g. Eclipse):**
Import as a Maven project, configure the environment variables below, then
run the Spring Boot application class directly. The app can also run as
multiple instances simultaneously since all state (cart, auth, analytics) is
externalized to Postgres/Supabase/the JWT itself rather than kept in-memory
per instance.

> **Note:** WebP image processing is intentionally **local-only** — it isn't
> deployed to Render. See [docs/deployment.md](docs/deployment.md) for why.

> **Note:** `auth_token`'s `secure` cookie flag requires HTTPS. Testing login
> over plain `http://localhost` will silently fail to set/send the cookie —
> flip `secure` off only for a local dev profile if this bites you.

### Environment Variables

| Variable | Description |
|---|---|
| `PORT` | HTTP port (default `8080`) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | PostgreSQL connection |
| `SUPA_URL` | Supabase project base URL |
| `SUPA_SERVICE_KEY` | Supabase service-role key (storage auth) |
| `SUPA_BUCKET_NAME` | Supabase bucket for image/article uploads |
| `SECRET_CRON_TOKEN` | Shared secret for cron traffic through analytics checks |
| `ANALYTICS_ENABLED` | Enable/disable analytics tracking (default `true`) |
| `app.jwt.secret` | JWT signing key — long, random, base64-safe. Never commit. |
| `app.jwt.expiration-ms` | Access token lifetime (default 1 hour) |

Variables can be set in the shell or in a local `.env` file.

## Running Tests

```bash
./mvnw test -q
```

## Building

```bash
./mvnw -q -DskipTests package
java -jar target/ThuongMaiEurekaProduct-0.0.1-SNAPSHOT.jar
```

## API Documentation

Interactive API docs available at `/swagger-ui.html` when running locally.

## Deployment

Deployed on Render.com's free tier via Docker. See
[docs/deployment.md](docs/deployment.md) for the deployment setup, both
memory-exhaustion incidents (OOM kill, then metaspace cap) and how each was
root-caused, and the JWT/CSRF production verification checklist.

## Project Status

Core hardening pass complete: automated observability (hourly memory
snapshots), resolved memory constraints, and a full stateless-auth migration
with role-based admin access are all in place and verified in production.
Remaining candidates: automated test coverage, OpenAPI doc polish, and
optionally moving admin controllers into a dedicated package for
organization.
