# Deployment

## Environment

- **Host:** Render.com, free tier (512MB RAM, 0.1 CPU)
- **Runtime:** Docker, multi-stage build (Maven build stage → JRE runtime stage)
- **Database:** Supabase (Postgres), free tier
- **Storage:** Supabase Storage API (images, blog article content)
- **Alternative run mode:** directly via IDE (e.g. Eclipse) with required libraries and environment variables configured — used mainly for local development and for image-processing work that the Render tier can't reliably handle (see below)
- **Scaling note:** the app can run as multiple instances concurrently, since cart/session state resolves through `userId` + the database rather than in-memory per-instance state

## Dockerfile

Two-stage build: `maven:3.9.6-eclipse-temurin-17` compiles the JAR, then
`eclipse-temurin:17-jre` runs it as a minimal runtime image.

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Xms64m", "-Xmx220m", \
  "-XX:MaxMetaspaceSize=96m", \
  "-XX:MaxDirectMemorySize=32m", \
  "-XX:ReservedCodeCacheSize=48m", \
  "-Xss256k", \
  "-XX:+UseSerialGC", \
  "-jar", "app.jar"]
```

Note: `-DskipTests` is used in the build stage to keep image build time and
build-stage memory down; tests are run separately (`./mvnw test`) before
building, not as part of the Docker build itself.

## The memory constraint problem

Under Render's free tier (512MB RAM), the app was intermittently killed with
**status 137** — an out-of-memory kill by the container's cgroup limit (the
orchestrator killing the process, not a Java `OutOfMemoryError`).

### Diagnosis

The initial JVM flags only bounded heap size (`-Xmx320m`), but total container
memory includes several things outside the heap that weren't constrained:

- **Metaspace** — unbounded by default
- **Thread stacks** — Spring Boot/Tomcat defaults to ~200 threads, each reserving ~1MB of stack space by default
- **Direct/NIO buffers** used by the servlet container's connectors
- **GC native overhead** — G1GC carries more native bookkeeping than simpler collectors, which matters on small heaps
- **JIT code cache and other JVM internals**

Heap could stay well under its limit while total resident memory (RSS) still
exceeded the container's 512MB ceiling.

### Fix

Applied at two levels:

**JVM flags** (Dockerfile `ENTRYPOINT`, shown above) — bound heap, metaspace,
direct memory, code cache, and per-thread stack size explicitly; switched from
G1GC to SerialGC for lower native memory overhead at this heap size.

**Application-level tuning** (`application.properties`):
```properties
# Cap the servlet thread pool — default sizing assumes far more CPU/RAM than 0.1 CPU / 512MB provides
server.tomcat.threads.max=8
server.tomcat.threads.min-spare=2
server.tomcat.accept-count=20

# Cap the DB connection pool — Supabase free tier has a low connection ceiling too
spring.datasource.hikari.maximum-pool-size=3
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=30000
```

**Async task executor** (`AsyncConfig`) bounded to core pool size 2 / max pool
size 4 / queue capacity 50, to avoid unbounded thread creation for background
work.

### Why SerialGC over G1GC

G1GC is designed for larger heaps with concurrent, low-pause collection. On a
sub-300MB heap with 0.1 CPU, its native bookkeeping overhead works against the
constraint it's meant to help with. SerialGC is lighter on both CPU and native
memory — the standard recommendation for small containerized JVMs.

### Verification

<!-- TODO: how did you confirm the fix worked? e.g. "monitored Render's memory
metrics over N days with no 137 kills" / "load tested with X concurrent requests" -->

## Known constraint: image processing on Render free tier

Image resizing/conversion (Thumbnailator-based, used for product images and
media management) is CPU- and memory-intensive and **has failed in practice on
Render's free tier**. Current workaround: image processing is done on a
stronger host (local machine) rather than in the deployed Render instance.

<!-- TODO: specifics worth capturing here for the writeup —
- What did the failure actually look like (timeout? OOM kill again? specific error)?
- Is there a size/volume threshold where it starts failing?
- Is there a plan to offload this (e.g. process before upload, or a separate worker)? -->

## Environment Variables

| Variable | Purpose |
|---|---|
| `PORT` | HTTP port (default `8080`) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | PostgreSQL connection |
| `SUPA_URL` | Supabase project base URL |
| `SUPA_SERVICE_KEY` | Supabase service-role key (storage auth) |
| `SUPA_BUCKET_NAME` | Supabase bucket for image/article uploads |
| `SECRET_CRON_TOKEN` | Shared secret allowing cron traffic through analytics checks |
| `ANALYTICS_ENABLED` | Enable/disable analytics tracking (default `true`) |

Loaded from shell env or an optional local `.env` file.

## Monitoring / Known Constraints Going Forward

<!-- e.g. "No alerting configured yet — memory usage checked manually via Render
dashboard" / "Cold start on Render free tier can take Ns after idle" -->
