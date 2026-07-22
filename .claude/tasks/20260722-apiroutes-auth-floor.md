# Mandatory defaultAuthRule floor on ApiRoutes — structural default-deny

**Status:** TODO (designed + agreed 2026-07-22) — depends on `20260722-authorize-rule-builder.md`
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

## Cross-references

- Builds on `20260722-authorize-rule-builder.md` (the accumulator IS the floor mechanism).
- `20260719-cross-realm-authz-and-tests.md` — the floor is how forUserType stops being per-route
  opt-in; update that task when this lands.
- Part 3 (`20260722-two-phase-auth-consistent-params.md`) relies on the floor being caller-only.
