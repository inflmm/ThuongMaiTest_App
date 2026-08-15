# Architecture

## System Overview

```
[Client / Browser]
        |
        v
[Spring Boot App — Docker container on Render.com]
   |-- Thymeleaf views (admin panel, storefront pages)
   |-- REST controllers (/api/**)
   |-- VisitorInterceptor (analytics tracking)
        |
        |-- JPA / Hibernate --> [PostgreSQL (Supabase-hosted)]
        |-- Storage API     --> [Supabase Storage (bucket): images, blog content]
```

App can run either as a Docker container (Render, or locally via
`docker build`/`docker run`) or directly from an IDE (e.g. Eclipse) with the
same environment variables configured. Multiple instances can run
concurrently since session/cart state resolves through `userId` and the
database rather than in-memory per-instance state.

## Components

### Controller layer
- REST controllers under `/api/**` (storefront, cart, checkout, admin) and Thymeleaf-backed controllers for server-rendered admin/storefront pages
- **Owns:** request/response mapping, input binding
- **Does not own:** business rules (delegated to service layer) or persistence (delegated to repositories)

### CartController / CartService
- **Owns:** add/update/remove item, guest cart handling, guest→user cart merge, cart count
- **Does not own:** pricing snapshot logic at checkout time (handled during order creation)

### Checkout / Order flow
- **Owns:** converting a cart into an `Order` + `OrderItem` rows with snapshot pricing (`productSlug`, `productName`, `quantity`, `unitPrice` captured at time of purchase, decoupled from live `Product` data)
- **Why snapshot pricing:** protects order history integrity if product price/name changes later

### Product / Category / Collection management
- **Owns:** catalog CRUD, category tree (self-referential `parent`/`children`), many-to-many product↔collection via `collection_product` join table, dynamic specifications (`ProductSpecification`), ordered image variants (`ProductImage`)
- Soft delete (`deleted` flag) throughout rather than hard delete

### Auth / Security (`SecurityConfig`)
- **Owns:** session-based authentication, JSON login/logout responses, public vs. admin route protection (`/admin/**`, `/api/admin/**`), role check via `User.role` as a Spring Security authority (`ROLE_ADMIN`, `ROLE_USER`)
- CSRF currently disabled — see Known Limitations

### Media management (`AdminImageController` + image services)
- **Owns:** image upload, resize/convert (Thumbnailator), folder organization, raw (non-processed) upload path, push to Supabase bucket
- **Constraint:** resize/convert is CPU/memory-intensive and unreliable on Render's free tier — see docs/deployment.md

### Blog system
- **Owns:** publish state, featured flag, slug lookup, search/filter, soft delete; article body stored file-backed (`contentPath`), loaded into a `@Transient content` field for rendering

### Analytics (`VisitorInterceptor`, `DailyAnalytics`, `UserSessionLog`)
- **Owns:** cookie-based session/visit tracking, daily traffic + unique-session aggregation, admin dashboard read endpoints
- Deliberately built in-app rather than pulling in a separate analytics service

### AsyncConfig
- **Owns:** a small bounded task executor (core pool 2, max pool 4, queue 50) for async work, sized deliberately small to avoid unbounded thread growth on a constrained host

## Data Model

- **User** — login identity + role (`ROLE_ADMIN`/`ROLE_USER`); unique on username/email/phone. Not JPA-linked to cart/order — those reference `userId` as a plain string identifier rather than a relation.
- **Category** — self-referential (`parent` → many `children`), `level`, `displayOrder`, `slugPath`, soft-deletable
- **Product** — belongs to one `Category`; has many `ProductSpecification`, many `ProductImage`; many-to-many with `Collection`; soft-deletable
- **ProductSpecification** — key/value pairs per product
- **ProductImage** — `objectPath`, `publicUrl`, `displayOrder`, `variant`, soft-deletable
- **Collection** — many-to-many with `Product` via `collection_product`
- **CartItem** — references `Product`; owned by `userId` (string), `quantity`, soft-deletable
- **Order** — owned by `userId` (string); `totalAmount`, `status` (`OrderStatus` enum), `orderDate`
- **OrderItem** — linked to `Order` via `orderId`; references `Product`; stores a pricing/name snapshot independent of live product data
- **Blog** — `title`, `slug`, `thumbnail`, `summary`, `contentPath`, publish/featured flags, soft-deletable
- **Notes** — simple standalone note entity
- **DailyAnalytics** — `date`, `totalTraffic`, `uniqueSessions`
- **CustomRoute** — URL → page mapping for storefront custom routes
- **UserSessionLog** — session-level activity tracking
- **BaseEntity** — shared base adding `created_time`/`updated_time` via `@CreatedDate`/`@LastModifiedDate`

## Key Technical Decisions

| Decision | Choice | Alternative considered | Why |
|---|---|---|---|
| GC algorithm | SerialGC | G1GC (Spring Boot/JVM default) | Lower native memory overhead on a 512MB / 0.1 CPU host |
| Connection pool size | Hikari: max 3, min-idle 1 | Hikari default (max 10) | Memory footprint + Supabase free-tier connection ceiling |
| Web server threads | Tomcat: max 8, min-spare 2, accept-count 20 | Tomcat default (max 200) | Thread stacks are a major non-heap memory cost; 0.1 CPU can't usefully serve 200 threads anyway |
| Async execution | Custom bounded `taskExecutor` (core 2 / max 4 / queue 50) | Spring's default `SimpleAsyncTaskExecutor` (unbounded) | Prevents unbounded thread growth under async load |
| Auth model | Session-based (Spring Security) | JWT/stateless | Simpler for a monolith with server-rendered admin views; trade-off noted below |
| Pricing at checkout | Snapshot fields on `OrderItem` | Live join to `Product` at read time | Order history stays accurate even if product price/name changes later |
| Deletes | Soft delete (`deleted` flag) | Hard delete | Preserves records for admin recovery/audit without extra tables |
| Media processing | Thumbnailator, in-process | External image service | Simpler for a solo project, but revealed a real resource constraint — see limitations |

## Known Limitations / Trade-offs

- **CSRF is disabled.** Reasonable for a stateless API surface, but this app uses session-based auth with a browser client, so CSRF exposure needs a deliberate decision (re-enable with token handling, or document why it's accepted) rather than being left as a default-off.
- **User is not a JPA relation from Cart/Order.** `userId` is a plain string on both sides. Works, but loses referential integrity checks at the DB level and requires manual consistency in the service layer.
- **Image processing is not reliable on the current production host.** Resize/convert (Thumbnailator) has failed on Render's free tier (512MB/0.1 CPU) under real usage — see docs/deployment.md for the specific failure and current workaround (processed locally / on a stronger host).
- **No caching layer.** Acceptable at current traffic; would revisit with a cache (e.g. Caffeine/Redis) for the category tree and product listings if traffic grew.
- **No explicit GC tuning conflict check** — GC flags live in the Dockerfile `ENTRYPOINT`, not in application config, so anyone running the JAR directly (outside Docker) won't get the same memory bounds unless they pass the flags manually.
