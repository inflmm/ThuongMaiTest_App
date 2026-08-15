# ThuongMai Ecommerce Backend

This is AI generated report

Demo links: [Homepage](https://thuongmaitest-app.onrender.com/homepage) | [Admin](https://thuongmaitest-app.onrender.com/admin/login) - contact to the author for admin account

A Spring Boot ecommerce backend: storefront catalog, cart/checkout, role-based
admin, blog/content management, and visitor analytics — built as a portfolio
project and deployed under real resource constraints (512MB RAM / 0.1 CPU free
tier), including diagnosing and fixing intermittent OOM kills.

## Overview

This is a monolithic Spring Boot ecommerce backend covering the storefront
(product catalog, categories, collections, custom routes), cart and checkout,
session-based auth with role-based access control, an admin panel for content
and product management, a simple blog system with Supabase-backed media
storage, and lightweight built-in visitor analytics. What makes it non-trivial:
it was deployed and hardened on a genuinely memory-constrained host (Render
free tier), which forced real decisions around JVM tuning, connection pool
sizing, and thread pool limits rather than defaults.

## Tech Stack

- **Language / Build:** Java 17, Maven (`./mvnw`)
- **Backend:** Spring Boot 3.5.8, Spring Cloud BOM 2025.0.0, Spring Web MVC, Spring Data JPA, Spring Security 6
- **Templating:** Thymeleaf + Thymeleaf Spring Security extras
- **Database:** PostgreSQL (Hibernate/JPA), H2 for tests
- **Storage:** Supabase Storage API (product images, blog article content)
- **API docs:** springdoc OpenAPI Starter WebMVC UI 2.6.0
- **Media processing:** Thumbnailator, imageio-core
- **Other:** YAUAA (user-agent parsing for analytics)
- **Deployment:** Docker, Render.com (free tier — 512MB RAM / 0.1 CPU); also runnable directly via Eclipse IDE

## Features

- **Storefront catalog** — product listing/detail, lookup by slug/ID, nested category tree, collection pages, custom route resolution
- **Product management** — admin CRUD with soft delete, images with variants/ordering, dynamic key/value specifications, stock and visibility control
- **Cart & checkout** — guest and registered cart support, guest-to-user cart merge, checkout converts cart into an order with snapshot pricing
- **Auth & authorization** — session-based login (Spring Security), JSON login/logout responses, `ROLE_ADMIN` / `ROLE_USER` with protected `/admin/**` and `/api/admin/**`
- **Blog system** — publish state, featured flag, slug lookup, search/filtering, soft delete, file-backed content
- **Media management** — image upload, resize/convert, folder organization, raw upload for unprocessed images, Supabase bucket storage
- **Analytics** — request/session tracking interceptor, daily traffic + unique-session aggregation, admin dashboard endpoints
- **Admin content tools** — categories, collections, folders/files tree, custom routes CRUD

## Architecture

Layered MVC (`controller` → `service` → `repository` → JPA entity), session-based
Spring Security auth, soft-delete pattern across most entities, and a shared
`BaseEntity` for audit timestamps. See [docs/architecture.md](docs/architecture.md)
for the full component breakdown, data model, and the reasoning behind key
decisions.

## Running Locally

Two supported ways to run this:

**Option A — Docker (matches production/Render setup):**
```bash
docker build -t thuongmai-app .
docker run -p 8080:8080 --env-file .env thuongmai-app
```

**Option B — Directly via IDE (e.g. Eclipse):**
Import as a Maven project with the required libraries installed, configure the
environment variables below (via `.env` or IDE run configuration), then run
the Spring Boot application class directly.

The app can also run as multiple instances simultaneously (e.g. for local
load-testing or horizontal scaling experiments) since state is externalized to
Postgres/Supabase rather than kept in-memory per instance.

> **Note:** image resizing/processing (Thumbnailator-based) is memory- and
> CPU-intensive and has failed under Render's free-tier constraints. Use a
> stronger host (e.g. local machine) for workflows involving bulk image
> processing; see [docs/deployment.md](docs/deployment.md) for details.

### Environment Variables

| Variable | Description |
|---|---|
| `PORT` | HTTP port (default `8080`) |
| `DB_HOST` | PostgreSQL hostname |
| `DB_PORT` | PostgreSQL port (default `5432`) |
| `DB_NAME` | PostgreSQL database name |
| `DB_USER` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `SUPA_URL` | Supabase project base URL |
| `SUPA_SERVICE_KEY` | Supabase service-role key (storage auth) |
| `SUPA_BUCKET_NAME` | Supabase bucket for image/article uploads |
| `SECRET_CRON_TOKEN` | Shared secret for cron traffic through analytics checks |
| `ANALYTICS_ENABLED` | Enable/disable analytics tracking (default `true`) |

Variables can be set in the shell or in a local `.env` file
(`spring.config.import=optional:file:.env[.properties]`).

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

Interactive API docs available at `/swagger-ui.html` when running locally
(springdoc OpenAPI).

## Deployment

Deployed on Render.com's free tier via Docker. See
[docs/deployment.md](docs/deployment.md) for the deployment setup and the
memory-tuning work done to resolve intermittent OOM kills (status 137) under
the 512MB constraint.

## Project Status

Actively hardening for portfolio use — prioritizing test coverage, security
validation, and observability over new features. See `projectPlan.md` for the
current roadmap.
