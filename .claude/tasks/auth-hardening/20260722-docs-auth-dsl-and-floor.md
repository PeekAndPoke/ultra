# Docs follow-up: authorize DSL rewrite + ApiRoutes floor (auth-hardening quartet)

**Status:** TODO — COLLECTOR for a later documentation pass (do not write the prose here, capture
what changed + anchors). Created 2026-07-22 per the "framework change → doc task" standing rule.
**Type:** documentation
**Triggering changes:** `52512bd4` (part 1: block-style authorize DSL), `2b6d274d` (part 2: mandatory
ApiRoutes floor). Parts 3–4 (two-phase auth + ConsistentParam; findById→Stored-param migration) are
NOT built yet — add to this task when they land.

## Why this exists

The funktor auth/rest docs describe the OLD authorization model and are now wrong in ways a copied
snippet won't compile. Capture the new model while it's fresh.

## Stale doc locations (audited 2026-07-22)

1. **`docs-site/src/pages/ultra/funktor/rest.astro` → `<h2>Authorization DSL</h2>` (lines ~136-165)**
   — the most wrong. Currently shows:
   - `"Rules compose with and / or."` — **FALSE now.** The infix `AuthRule.or`/`and` are DELETED
     (zero call sites). Composition is block-style only.
   - `forRole("admin") or forGroup("staff")` — **won't compile.** New form:
     `forAny { forRole("admin"); forGroup("staff") }`.
   - `.authorize { isSuperUser() }` per route with no mention that a route now INHERITS its group's
     floor and `authorize` only STRENGTHENS (append/AND).
   - The built-in-rules comment list is still accurate (public/authenticated/forbidden/isSuperUser/
     forRole/forGroup/forPermission/forOrganisation) — plus NEW: `forUserType("...")`.
2. **`docs-site/src/pages/ultra/funktor/rest.astro` → `<h2>API features</h2>` (~184)** — should
   mention that an `ApiFeature` can bundle MULTIPLE `ApiRoutes` groups (audience split), and that
   each `ApiRoutes` now REQUIRES a `defaultAuth` floor.
3. **`docs-site/src/pages/ultra/funktor/auth.astro`** — realms/JWT/providers sections are fine; add
   a pointer to the floor as the "default-deny" story if auth-model overview lives here.
4. **`docs-site/src/data/llms/funktor.md`** (LLM mirror template — edit the TEMPLATE, never
   `docs-site/public/`) — mirror all of the above; grep it for `authorize`, `or(`, `isSuperUser`.

## The NEW model to document (mental model + anchors)

### Part 1 — block-style `authorize {}` (anchors: `funktor/rest/.../auth/AuthRuleBuilder.kt`, `AuthRule.kt`)
- **Every statement is enforced (AND).** `authorize { a(); b() }` requires BOTH — the old
  last-expression-wins footgun is gone.
- **Composition is blocks, not operators.** `forAll { }` = all must pass (AND), `forAny { }` = at
  least one (OR); nest for mixed trees. Statements inside `forAny` are disjuncts. Example:
  ```kotlin
  authorize {
      forAny {
          isSuperUser()
          forAll { forUserType(B2bUserModel.USER_TYPE); forRole("org-admin") }
      }
  }
  ```
- Infix `or`/`and` and value-arg `forAll(a, b)` are REMOVED. `AuthRule.Companion` factories remain
  for programmatic composition.
- **`forUserType(type)`** is the NEW realm-boundary rule (checks the JWT `user/type` claim) — all
  realms share one signing key, so `isSuperUser()` alone is realm-agnostic.
- Constants `public()`/`forbidden()` must be the SOLE rule of a chain. One `authorize {}` per route.
- `@RestDsl` `@DslMarker` makes wrong-level use (`public()` inside `forAny {}`, `docs {}` inside
  `authorize {}`) a COMPILE error.

### Part 2 — mandatory `defaultAuth` floor (anchors: `funktor/rest/.../ApiRoutes.kt`, `auth/FloorAuthRuleBuilder.kt`, `ApiRoute.withFloor`, `ValidateRoutesOnAppStarting.kt`)
- **Structural default-deny.** Every `ApiRoutes` group MUST declare a floor:
  `ApiRoutes("name", defaultAuth = { isSuperUser() })`. Omitting it is a compile error.
- The floor is the INITIAL auth chain PREPENDED to every route; per-route `authorize` can only
  STRENGTHEN it (append/AND), never weaken. A public group declares `defaultAuth = { public() }`.
- **Floor is caller-only** (`FloorAuthRuleBuilder` has no `forCall`/`appendRule`) → evaluated before
  request data (phase-1, matters for part 3).
- **Mixed audiences → separate groups.** An append-only floor cannot mix `public()` with a
  restrictive rule, so a group serving both public and protected endpoints must be SPLIT into two
  `ApiRoutes` groups (e.g. `AuthApi { public() }` + `AuthUserApi { authenticated() }`), both under
  one `ApiFeature`. Document this as the canonical pattern (it's why AuthApi is now two classes).
- `addRoute` is the single choke point that applies the floor + validates the whole chain at
  construction; a floor/route conflict aborts server boot with an actionable message.

### Docs for consumers migrating their own ApiRoutes
- Before: `class MyApi : ApiRoutes("my")` + per-route `authorize { isSuperUser() }` on each route.
- After: `class MyApi : ApiRoutes("my", defaultAuth = { isSuperUser() })` + drop the per-route blocks
  (or keep only strengthening ones). Public + protected routes → two groups.

### Part 3 — two-phase auth + pluggable checks + org-isolation (LANDED 2026-07-23; anchors below)
Document, with a mental-model diagram of the phase-1/phase-2 split:
- **Two-phase evaluation** — caller-only rules (floor + permission checks) run in PHASE 1, BEFORE
  param conversion (`findById`); param-dependent rules run in PHASE 2, after. Kills pre-auth DB reads
  and the 404-vs-401 existence oracle. Anchors: `funktor/rest/.../auth/AuthPhase.kt` (`isCallerOnly`,
  `flattenTopLevelAnds`), `routing.kt`, `ApiRoute.phase1Denials`/`checkParamPhase`.
- **`RouteBootCheck`** (boot) + **`RouteParamsGuard`** (phase-2 per request) — the extension points.
  `ValidateRoutesOnAppStarting` is a runner over `getAll(RouteBootCheck)`; modules add domain checks.
  Anchors: `funktor/rest/.../RouteBootCheck.kt`, `RouteParamsGuard.kt`, `ConverterCompatBootCheck.kt`,
  `AuthChainBootCheck.kt`.
- **Org-isolation (saas)** — `OrgAware { val org: Ref<Organisation> }` on entities, `OrgAwareParam
  { val org: Stored<Organisation> }` on route params. Boot-forced (`OrgIsolationBootCheck`: an
  OrgAware entity ⟹ params must be `OrgAwareParam`) + runtime `OrgIsolationGuard` (caller-binds the
  SELECTED session org; every OrgAware entity must belong to it). 404-hidden. Document the CONTRACT:
  `OrgAware.org` carries the canonical `_id`; the interfaces are inert without the saas module.
  Anchors: `funktor/saas/.../isolation/OrgIsolation.kt`, `OrgIsolationBootCheck.kt`, `OrgIsolationGuard.kt`.
- **`ConsistentParam`** — now PURE OPT-IN, for NON-org referential consistency only (org isolation is
  the separate, forced mechanism). Anchor: `funktor/core/.../broker/ConsistentParam.kt`.

### Part 4 — `Stored<T>` route params replacing handler-side `findById` (entity-resolving param binding).

## Do NOT
- Do not edit `docs-site/public/` LLM mirrors — edit `docs-site/src/data/llms/*.md` templates.
- Do not write the prose in THIS file — this is the collector; the doc pass writes the pages.
