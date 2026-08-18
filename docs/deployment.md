# Deployment

## Environment

- **Host:** Render.com, free tier (512MB RAM, 0.1 CPU)
- **Runtime:** Docker, multi-stage build (Maven build stage → JRE runtime stage)
- **Database:** Supabase (Postgres), free tier
- **Storage:** Supabase Storage API (images, blog article content)
- **Alternative run mode:** directly via IDE (Eclipse) — used for local development and for WebP image processing (see below)
- **Scaling:** multiple instances can run concurrently — auth is stateless and all other state lives in Postgres/Supabase, so there's no session-affinity requirement

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
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:MaxDirectMemorySize=32m", \
  "-XX:ReservedCodeCacheSize=48m", \
  "-Xss256k", \
  "-XX:+UseSerialGC", \
  "-jar", "app.jar"]
```

Application-level tuning (`application.properties`):
```properties
server.tomcat.threads.max=8
server.tomcat.threads.min-spare=2
server.tomcat.accept-count=20

spring.datasource.hikari.maximum-pool-size=3
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=30000
```

Plus a bounded async task executor (`AsyncConfig`): core pool 2, max pool 4, queue 50.

---

## Incident 1: hard OOM kills (status 137)

Under Render's free tier, the app was intermittently killed with status 137
— the container's cgroup OOM-killer, not a Java `OutOfMemoryError`.

**Root cause:** early JVM flags only bounded heap (`-Xmx320m`) — but total
container memory also includes metaspace (unbounded by default), thread
stacks (Tomcat's default ~200-thread pool, ~1MB stack each), direct/NIO
buffers, and GC native overhead (G1GC carries more of this than simpler
collectors at small heap sizes). Heap could stay well under its own limit
while total RSS still exceeded the 512MB container ceiling.

**Fix:** the JVM flags above (explicit metaspace/direct-memory/code-cache/
stack-size caps, SerialGC instead of G1GC) plus the Tomcat thread cap and
Hikari pool cap. Resolved — no further 137 kills observed.

## Incident 2: metaspace OOM (after the fix above)

With the hard kill resolved, a *different* failure appeared after roughly a
day of uptime: `Metaspace OutOfMemoryError`, hitting the (then) 96MB
`MaxMetaspaceSize` cap.

**Diagnosis process:** built `MemoryMonitorService` (hourly JVM snapshot →
`MemoryUsageLog` table: heap, non-heap, metaspace, loaded/unloaded class
counts, thread count) specifically to get real data rather than guess.

Two competing hypotheses were considered:
- **A — undersized cap, not a leak:** metaspace grows as more of the app's
  code paths get exercised for the first time (Hibernate proxies, AOP
  proxies, query plans, etc.), then plateaus once most classes that will
  ever load have loaded.
- **B — a real leak**, most plausibly `spring-boot-devtools`' restart
  classloader failing to release old classes on reload.

**Ruling out B:** confirmed `spring-boot-devtools` was `<optional>true</optional>`
and showed no console indication of being active in production — ruled out.

**Confirming A with the logging data:** three snapshots (before, during, and
~1h11m after a JPEG resize operation) showed `metaspaceUsed` climbing
90MB → 95MB → 97MB while `unloadedClassCount` stayed **exactly flat at 166**
across all three — the key signature. A real leak would show classes being
unloaded and reloaded in a churn (rising `unloadedClassCount` alongside
`totalLoadedClassCount`); flat `unloadedClassCount` with `loadedClassCount`
growth that was already decelerating (+761 classes around the resize
operation, only +132 over the following hour) is exactly the "one-time
warm-up, then plateau" shape.

**Fix:** raised `MaxMetaspaceSize` to 128MB — generous enough for this
stack's genuine steady-state need. Verified healthy afterward via the same
logging: usage plateaued well under the new cap, no further incidents.

---

## WebP: local-only by design

Image resizing/conversion is CPU/memory-intensive. WebP conversion (via the
`cwebp` CLI) specifically failed under Render's free-tier constraints and
was removed from the deployed path — WebP processing now happens locally
(Eclipse) only, with JPEG conversion (rewritten with bounded decode +
progressive downscaling — see docs/architecture.md) as the only image format
processed on the deployed server. This is a deliberate, documented split,
not a workaround left in by accident.

## JWT / CSRF migration

Login moved from Spring Security's session-based `formLogin` to stateless
JWT in an httpOnly cookie, with CSRF re-enabled (see docs/architecture.md's
"CSRF stack" section for the three cooperating fixes this required — a
naive stateless-JWT + CSRF setup has real, non-obvious bugs around token
writing and token rotation that were found and fixed during this migration).

**Production verification performed:**
- Cookie flags confirmed correct via DevTools (`auth_token`: httpOnly +
  Secure; `XSRF-TOKEN`: JS-readable by design; `visited_session`: httpOnly)
- `document.cookie` confirmed `auth_token` is NOT JS-accessible (httpOnly
  genuinely enforced)
- Back-to-back authenticated writes both succeed (the specific regression
  test for the token-rotation bug that was fixed)
- Role separation confirmed: EMPLOYEE reaches the admin panel but gets 403
  on ADMIN-exclusive actions
- Logout confirmed to clear the cookie and re-lock protected routes
- Multiple concurrent logins (different browsers) confirmed independent —
  the actual payoff of going stateless

## Environment Variables

| Variable | Purpose |
|---|---|
| `PORT` | HTTP port (default `8080`) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | PostgreSQL connection |
| `SUPA_URL` / `SUPA_SERVICE_KEY` / `SUPA_BUCKET_NAME` | Supabase storage |
| `SECRET_CRON_TOKEN` | Shared secret allowing cron traffic through analytics checks |
| `ANALYTICS_ENABLED` | Enable/disable analytics tracking (default `true`) |
| `app.jwt.secret` | JWT signing key — long, random, never committed |
| `app.jwt.expiration-ms` | Access token lifetime (default 1 hour) |

## Monitoring Going Forward

`MemoryUsageLog` (hourly snapshots) is kept running as ongoing health
monitoring, not just incident response — the same table that diagnosed the
metaspace issue is now the first place to check if memory behavior looks off
again.
