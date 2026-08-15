# Project Plan — Ecommerce Backend Hardening (CV Focus)

## Why this plan exists

The AI-generated `projectDetail.md` treats this like a roadmap toward a real commercial store: payments, shipping, tax, growth analytics, content publishing. That's the right plan *if you're launching a business*. It's the wrong plan if the goal is a strong CV entry for a fullstack role where you want to lead with backend depth.

This plan reorders the work around a different question: **what makes a reviewer or interviewer conclude "this person can be trusted with production backend code"?** That's mostly about testing, security, reliability, and being able to talk in detail about real engineering decisions — not about feature completeness.

**Goal of this plan:** turn the repo + your ability to talk about it into evidence of backend competence, in roughly 2–4 weeks of focused part-time work, without chasing full e-commerce completeness.

**Non-goal:** building a launch-ready store. Payment integration, shipping/tax, coupons, blog publishing, analytics dashboards are explicitly out of scope here — revisit them later if you have time left over, not before.

---

## How to use this document

Each phase below has:
- **Goal** — what this phase is *for*, in terms of CV/interview impact
- **What to think through first** — decisions you should make deliberately, not by default, before writing code
- **Work items** — concrete, scoped tasks
- **Definition of done** — the specific, checkable bar that means "stop, this phase is finished." Resist the urge to keep polishing past this bar — diminishing returns kick in fast, and the next phase matters more than a perfect version of this one.

Work through phases in order. Each is designed to be independently shippable — you can pause after any phase and already have something better than before.

---

## Phase 1: Automated Testing

### Goal
This is the single highest-leverage change available. "No tests" is the fastest way for a reviewer to downgrade an otherwise solid repo; a focused test suite on your most business-critical flow is the fastest way to change that impression.

### What to think through first
- **What actually needs testing vs. what's just easy to test.** Don't start with trivial getter/setter coverage — it pads a number without proving anything. Prioritize checkout and cart logic: the code where a bug has real consequences (double charges, stock going negative, order totals miscalculated).
- **Unit vs. integration.** Decide per-layer: service logic with mocked repositories (fast, isolated) vs. `@SpringBootTest` / Testcontainers-style integration tests against a real (or in-memory) database for repository and transactional behavior. You likely want both, but be deliberate about which for which class.
- **What "critical path" means for your domain.** Write down the 5–8 scenarios that would be embarrassing to get wrong in an interview if asked "what happens if...?" — e.g., adding more items to cart than stock allows, checkout with an empty cart, concurrent stock decrements, cart merge on login.

### Work items
- Unit tests for `CheckoutService`, `CartService`, `OrderService` — happy path + at least 2 edge cases each
- Integration test for the full cart → checkout → order flow
- Repository tests for anything with custom queries (e.g., category tree, product search/filter)
- Wire tests into your build (`mvn test` / `./gradlew test`) so they actually run, not just exist

### Definition of done
- Checkout and cart logic have test coverage for both success and at least one failure/edge case each
- Tests run and pass in CI or at minimum locally via one command
- You can explain, out loud, what each test file is protecting against — if you can't, it's not done, it's just code that exists

---

## Phase 2: Security & Validation Hardening

### Goal
CSRF disabled and partial validation are the kind of thing a reviewer notices in 5 minutes of skimming `SecurityConfig`. This phase closes the obvious gaps and, just as importantly, gives you a defensible answer for *why* you made each security decision — "I disabled CSRF because it's a stateless token-based API" is a fine answer; "I didn't think about it" is not.

### What to think through first
- **Is your API stateless (JWT/token) or session-based?** This determines whether CSRF protection should be re-enabled or whether disabling it is actually correct for your architecture. Don't just flip it back on without understanding which situation you're in — know the reason either way.
- **Where does validation actually belong?** Bean Validation annotations on DTOs (`@NotBlank`, `@Positive`, etc.) catch shape problems at the boundary; business-rule validation (stock availability, valid state transitions) belongs in the service layer. Mixing these up creates either an anemic domain model or scattered, inconsistent checks.
- **What's your error response contract?** Decide the shape once (e.g., `{ timestamp, status, error, message, path }`) before writing a global exception handler, so every endpoint returns errors consistently instead of ad hoc.
- **Secrets handling.** Confirm nothing is hardcoded or committed — DB credentials, Supabase keys, JWT secrets should all come from environment variables, and this should be visible/documented, since it's a common interview question about deployment practices.

### Work items
- Explicit, documented decision on CSRF (re-enable properly, or document why it's off)
- Bean Validation annotations on all request DTOs (registration, product create/update, checkout, cart operations)
- A global `@ControllerAdvice` exception handler with the consistent error shape you decided on
- Review role-based access rules for completeness — confirm admin endpoints actually reject non-admin users (write a quick test for this, it folds nicely into Phase 1 too)
- Audit for secrets in code/config; move anything sensitive to environment variables if not already

### Definition of done
- Every public-facing write endpoint validates its input and returns a consistent, structured error on failure
- You can state your CSRF/session decision and the reasoning behind it in one sentence
- Admin-only endpoints are covered by at least one test proving a non-admin request is rejected
- No secrets in source control (confirm, don't assume)

---

## Phase 3: Observability & Documenting the Deployment Work You Already Did

### Goal
You already have a genuinely strong story here: diagnosing OOM kills (status 137) on a memory-constrained free-tier host, tuning JVM heap/metaspace/thread-stack limits, capping Tomcat threads and the Hikari connection pool. Most junior/1–3yr candidates have nothing like this to talk about. Right now it's invisible — it lives in your memory, not in the repo. This phase makes it visible and adds the logging that should have caught the problem faster in the first place.

### What to think through first
- **What's the story arc?** Symptom → hypothesis → diagnosis → fix → verification. Write it that way, not as a list of flags you set. Interviewers care more about the diagnostic process than the final config.
- **What would you log if this happened again?** If you didn't have clear visibility into memory/thread state when it crashed, that's the gap logging should close — not just "add logs everywhere."
- **Correlation, not noise.** Decide on a lightweight correlation ID (per-request) early, so logs across a request are traceable, rather than adding print-style logging that's hard to follow under load.

### Work items
- Add structured logging (SLF4J + your existing framework's log format) at key points: request entry/exit for critical endpoints, checkout flow steps, error paths
- Add a simple request correlation ID (filter or interceptor) so a single request's logs can be traced together
- Write up the OOM/JVM-tuning investigation as a short doc or README section: what broke, how you found out, what you tried, what worked, what you'd monitor going forward
- Optional if time allows: a `/actuator/health` or simple custom health-check endpoint, since it's cheap with Spring Boot Actuator and is a common "do you know this exists" question

### Definition of done
- Critical flows (checkout, order creation, auth) produce log output that would let you reconstruct what happened during an incident
- The deployment/memory-tuning story exists as a written artifact (README section or `docs/deployment.md`), not just something you remember
- You can walk through that story in under 2 minutes if asked in an interview

---

## Phase 4: API Consistency & Documentation

### Goal
Lowest effort, real payoff for "looks like a real API" signal. This is largely annotation and structure work on top of what already exists, not new logic.

### What to think through first
- **Consistency over completeness.** A fully documented but inconsistent API (three different pagination styles, mixed status codes for the same kind of error) reads worse than a smaller, consistent one. Audit for consistency before adding docs on top of it.
- **Who's the doc for?** OpenAPI/Swagger is mostly for you being able to say "yes, it's documented" and for anyone (including an interviewer) to explore the API without reading source — optimize for that, not for exhaustive descriptions of every field.

### Work items
- Add springdoc-openapi (or equivalent) for auto-generated Swagger docs from your existing controllers
- Standardize response shapes for lists (pagination format), errors (from Phase 2), and success payloads
- Add a top-level `README.md` section: what the project is, tech stack, how to run it locally, how to run tests, architecture overview (even a simple diagram)

### Definition of done
- Swagger/OpenAPI UI is reachable and reflects real endpoints with request/response schemas
- A stranger could clone the repo and get it running locally from the README alone
- Pagination and error responses follow one shape across the whole API, not per-controller variations

---

## After Phase 4: What's genuinely worth revisiting

Once the above is solid, if you still want to extend the project, prioritize by CV-recognizability per unit of effort:

1. **Payment integration (Stripe test mode)** — the one "business feature" worth doing, because it's a resume-scannable line item and touches real integration/webhook-handling skills
2. **CI pipeline** (GitHub Actions running your Phase 1 tests on push) — cheap now that tests exist, and "has CI" is a quick positive signal
3. Everything else in the original plan (shipping/tax, coupons, blog, analytics, admin UX) — treat as optional polish, not a priority, unless a specific job description calls for it

## How to know the whole plan is "finished" for CV purposes

Not when every checkbox above is ticked with maximum polish — when you can do all of the following without hesitation in a live conversation:
- Explain a specific bug you found and fixed, with the diagnostic process
- Justify a security decision you made (not just describe what you did)
- Point to a test and explain what real-world failure it prevents
- Walk through what happens end-to-end on a checkout request, including error cases

That's the actual bar. It's a much smaller and more finishable target than "production-ready ecommerce platform."
