# Mandatory auth floor on ApiRoutes — structural default-deny

**NOTE (2026-07-23, part 4):** the ctor param was renamed `defaultAuth` → `authFloor` (it is a floor
route rules can only STRENGTHEN, not an overridable default). This doc's historical references to
`defaultAuth` describe the API as originally built; the current name is `authFloor`.

**Status:** DONE (2026-07-22) — review loop terminated on round 3 (zero confirmed findings across
all three reviewers). Mechanism + full 19→23-group sweep; all rounds' findings fixed; backend suites
green. Depends on `20260722-authorize-rule-builder.md` (DONE).

## Review record (review LOOP, 2026-07-22, 3× Opus per round)

- **Round 1:** security+impl found the SAME MEDIUM (floor-presence was disciplinary: public
  `addRoute` + empty chain = served public). Fixed structurally — `addRoute` is now the single
  `@PublishedApi internal` floor-applying choke point (non-empty + whole-chain validation);
  mount/route no longer pre-apply the floor; boot validator got a non-empty check. Domain MEDIUM:
  realm-scoping flag was OrgsApi-only → broadened to all 10 framework `{ isSuperUser() }` groups +
  recorded the missing app-strengthening seam. + 3 LOWs (FQCN/imports, distinct admin group names,
  floor-combinator tests).
- **Round 2:** security ZERO; domain 1 LOW (doc "6×"→"7×"); impl 1 LOW (`validateOrThrow`
  throw/aggregation coverage lost in the R1 spec rewrite → restored via the converter path since
  the auth path is now unreachable-by-construction) + 1 INFO (`java.util.Base64` FQCN). All fixed.
- **Round 3:** impl, domain, security ALL ZERO — "the design is right; the loop should terminate
  here." Loop terminated.

Deferred (recorded, not part-2 blockers): the framework `{ isSuperUser() }` groups remain
cross-realm-reachable (byte-identical to pre-floor; no NEW exposure) — per-group realm-scoping +
an app-level floor-strengthening seam are owned by `20260719-cross-realm-authz-and-tests.md`.
**Plan:** part 2 of the auth-hardening quartet
**Security-critical:** YES — this decides the minimal auth of every API endpoint in the framework.

## Review protocol (user directive, 2026-07-22)

Same LOOP as `20260722-authorize-rule-builder.md`: full 3-agent gate → fix ALL confirmed findings
→ FULL re-review with fresh reviewers → repeat until a zero-findings round. One pass is not
enough. ESCALATION: if a round shows the direction itself is wrong, stop and consult the user.

## Problem

Route guards are per-route opt-in. Forgetting one means public (no rules ⇒ `checkAccess`
succeeds) or realm-agnostic (the operator-console cross-realm HIGH: `isSuperUser()` without
`forUserType` admitted admin-realm super-users). Discipline does not scale; the default must.

## Design (DECIDED 2026-07-22)

1. **Mandatory ctor param** on `ApiRoutes`: a `defaultAuth` initializer that seeds every route's
   rule chain (the accumulator from part 1) as its INITIAL state. Every `ApiRoutes` group must
   declare its minimal auth — reading the class head tells you the floor.
   ```kotlin
   class OperatorApi : ApiRoutes(
       name = "operator",
       defaultAuth = {
           isSuperUser()
           forUserType(OperatorUserModel.USER_TYPE)
       },
   )
   ```
2. **Hard floor — no per-route escape.** Routes can only strengthen (append); nothing can clear
   or weaken the seed ("closest to no magic"). A genuinely public endpoint moves to its own
   `ApiRoutes` group seeded `defaultAuth = { public() }`. Which group a route is mounted in is
   the reviewable, greppable declaration of its audience.
3. **ApiFeatures bundle multiple ApiRoutes groups** (application reality — features are bundles,
   confirmed 2026-07-22): audience-split means one feature can carry e.g. an admin group, a
   tenant group, and a public group. Enforcement lives on `ApiRoutes` (where mount lives);
   the feature level adds nothing and stays out of it.
4. **Per-route `authorize {}` becomes OPTIONAL** — pure strengthening. A route needing only the
   floor declares nothing (today's `isSuperUser()`-on-every-route boilerplate in e.g.
   `IntrospectionApi` collapses into one seed).
5. **Non-empty guarantee:** RESOLVED via part 1 (factories return Unit, block-style DSL): the
   guarantee is the boot-time non-empty check on the seed chain — an undeclared or empty floor
   aborts app start. (The compiler-forced-return variant died with value-style composition.)
6. **Variance/shape:** the seed must apply to routes of any PARAMS/BODY, so it is a REPLAYABLE
   lambda over caller-only rule factories (`isSuperUser`, `forUserType`, roles/permissions —
   rules that read the caller, not the request data), replayed into each route's typed builder at
   mount. Caller-only-by-shape also means the floor is phase-1 by construction (see part 3:
   it runs BEFORE any param conversion / DB load).
7. **Docs/estimation:** seeded rules appear in each route's generated docs and access estimation
   exactly like declared rules (they are ordinary chain members).
8. `acceptedUserTypes` (the original idea) is subsumed: it is just a common seed
   (`{ forUserType(...) }`); no dedicated ctor param.

## The sweep (the deliberate cost)

Every existing `ApiRoutes` group must declare its floor — this forces audience decisions that are
currently implicit. Known groups to sweep (complete during implementation with a grep):

- `funktor/auth` AuthApi — public group (`{ public() }`) for sign-in/select-org/recover; any
  authenticated-user routes (profile/change-password) into an authenticated group.
- `funktor/saas` OrgsApi — currently all `isSuperUser()`; DECIDE the audience (operators-only?
  also admin-app?) — this was flagged as genuinely unclear; do not guess, surface to the user
  if ambiguous during the sweep.
- `funktor/insights` + `funktor/inspect` (IntrospectionApi etc.) — super-user floors; interacts
  with `20260720-insights-gui-auth-gate.md` (other agent's task — coordinate, do not modify it).
- `funktor-demo`: OperatorApi (floor = superuser + OperatorUser type), ShowcaseApi,
  FunktorConfApi, AuthShowcaseApi.
- `funktor/core` broker `Crud`/app routes if they flow through the same mount machinery — verify.

## Spec

- [ ] Mandatory `defaultAuth` on `ApiRoutes`; seed replayed into every route chain; hard floor
      (no clearing API exists).
- [ ] Per-route `authorize` optional.
- [ ] Public groups declare `{ public() }` explicitly; `public()` marker + floor coexistence
      validated (a group seeded public() must not ALSO seed restrictive rules — boot check).
- [ ] Sweep completed: every ApiRoutes group in the repo declares its floor; ambiguous audiences
      escalated, not guessed.
- [ ] Generated docs show floor rules per route.
- [ ] Full backend e2e suites green; the cross-realm locks (OperatorApiTest) still pass with the
      guard expressed as a floor instead of per-route.

## Test evidence

- [ ] Unit: seed replay per route; strengthening appends; no weakening path exists.
- [ ] e2e: a route with no per-route authorize is enforced at the floor (401/403 matrix).
- [ ] Boot: missing/empty floor impossible (compile or boot failure, per part-1 decision).

## Implementation notes (2026-07-22)

- **Mechanism:** `FloorAuthRuleBuilder` (`funktor/rest/.../auth/`) — a RESTRICTED builder exposing
  only caller-only factories (no `forCall`, no `appendRule`), so the floor is structurally phase-1
  and type-agnostic (materialized as `List<AuthRule<Any?, Any?>>`, cast per route). `ApiRoutes`
  gets a mandatory `defaultAuth` ctor param, materialized+validated once (`build(name)` → non-empty
  + `validateChain`). `ApiRoute.withFloor(floor)` (abstract, covariant overrides) PREPENDS the
  floor; injected in all 7 `mount` overloads AND the low-level `route {}` path (verified: 131
  routes all use `.mount`, zero use `route {}` today, but both are covered). Reuses part-1's
  whole-chain `validateChain` at boot, so a public-floored group + a route adding a restrictive
  rule is a boot error.
- **Audience decisions from the sweep:**
  - 15 uniform groups → floor + removed the now-redundant per-route `authorize` (33 blocks in the
    framework, ~8 in demo). Framework: OrgsApi, IntrospectionApi, LoggingApi, 7× cluster APIs →
    `{ isSuperUser() }`. Demo showcase reads → `{ public() }`; OperatorApi →
    `{ isSuperUser(); forUserType(OperatorUser) }`.
  - **4 mixed groups SPLIT by audience** (user decision 2026-07-22, "honor hard floor"): `AuthApi`
    → `AuthApi` (`{ public() }`, sign-in/up/recover/select-org) + `AuthUserApi`
    (`{ authenticated() }`, set-password/refresh/my-api-access) — both under `AuthApiFeature`;
    `MessagingShowcaseApi`/`ClusterShowcaseApi`/`FunktorConfApi` each split into a public reads
    group + a super-user `*AdminApi`/`*AdminShowcaseApi` writes group. `authenticated()` is the
    right cross-realm floor for the self-service auth routes (any logged-in user, realm in token).
  - **Framework `{ isSuperUser() }` groups — realm-scoping FLAGGED, not guessed (broadened per R1
    domain review):** OrgsApi AND IntrospectionApi, LoggingApi, and the 7 cluster APIs are all
    floored `{ isSuperUser() }` — realm-agnostic. Since all realms share one JWT key, an
    admin-realm super-user can reach every such surface cross-realm (same class as the part-1 HIGH;
    IntrospectionApi is arguably the higher-value target). Behavior is byte-identical to the
    pre-floor per-route `isSuperUser()`, and framework code genuinely cannot reference an app
    realm's `USER_TYPE` — so this is a documented deployment decision, not a code defect. TWO gaps
    to resolve (cross-ref `20260719-cross-realm-authz-and-tests.md`): (1) decide per framework group
    whether it must be realm-scoped; (2) there is currently **no seam** for a mounting app to add
    `forUserType` to a framework-owned group's floor (per-route `authorize` can only strengthen
    within the group's own declarations, and the floor is a hard ctor seed) — either add such a
    seam or adopt an explicit "framework admin APIs assume a single super-user-minting realm"
    deployment contract.

## Structural hardening (R1 security + impl finding, fixed)

Both reviewers found: floor-PRESENCE rested on discipline (every mount path remembering `withFloor`),
and `addRoute` was a public path that could register a route with an empty chain → served PUBLIC.
Fixed by making **`addRoute` the single, `@PublishedApi internal` choke point** that applies the
floor itself (`route.withFloor(floorRules)`), asserts the chain is **non-empty** (an empty chain
serves public — now impossible for a registered route), then runs the whole-chain `validateChain`.
`mount`/`route{}` no longer apply the floor — they only build the route and hand it to `addRoute`.
The boot validator (`ValidateRoutesOnAppStarting`) gained the same non-empty check as defense-in-
depth. Floor-presence is now structural: a route cannot be registered without its floor.

Split-group `name`s made distinct for docs-matrix clarity (`showcase-cluster-admin`,
`showcase-messaging-admin`, `funktor-conf-admin`); AuthApi's two groups keep `"login"` (one logical
login API split by auth level). Names are display-only (routing is by URI), so this is cosmetic.

## Cross-references

- Builds on `20260722-authorize-rule-builder.md` (the accumulator IS the floor mechanism).
- `20260719-cross-realm-authz-and-tests.md` — the floor is how forUserType stops being per-route
  opt-in; update that task when this lands.
- Part 3 (`20260722-two-phase-auth-consistent-params.md`) relies on the floor being caller-only.
