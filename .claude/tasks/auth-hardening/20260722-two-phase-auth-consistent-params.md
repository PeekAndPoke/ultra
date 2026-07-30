# Two-phase auth evaluation + org-isolation — no loads before auth, no cross-org param spoofing

**Status:** DONE (2026-07-23) — two-phase eval + pluggable route checks + saas org-isolation.
**Review LOOP CLOSED at round 5 (zero findings).** Rounds: 1 reworked on 2 HIGH direction findings
(caller-binding authority + structural forcing); 2 confirmed the idiomatic route airtight, fixed
edge/DiD (runtime-value org detection, docs, tests); 3 fixed LOW/INFO (guard↔loader reflection
alignment, no-saas KDoc accuracy, `_key`/`_id` contracts); 4 caught a regression round-3 introduced
(ctor-NAME filter dropped aliased non-`val` ctor entities) → fixed via backing-field superset +
regression test; 5 empirically verified the superset closes it — EMPTY. Green: rest 105, saas 31,
all 115 (both DB backends), demo 24. Commits 551e4364, 625b3d7a, f0038e1b, c5e63d06 (+ docs), all
UNPUSHED. Depends on `20260722-apiroutes-auth-floor.md`.
**Plan:** part 3 of the auth-hardening quartet
**Security-critical:** YES — request-data isolation (IDOR) + pre-auth resource access.

## Review protocol (user directive, 2026-07-22)

Same LOOP as `20260722-authorize-rule-builder.md`: full 3-agent gate → fix ALL confirmed findings
→ FULL re-review with fresh reviewers → repeat until a zero-findings round. One pass is not
enough. ESCALATION: if a round shows the direction itself is wrong, stop and consult the user.

Round 1 ran (3 Opus reviewers). Mechanism confirmed sound; two HIGH findings on the caller-binding
half drove the rework below. A fresh **round 2** runs over the WHOLE reworked diff when this lands.

## Problem A — everything is loaded before the auth checks (DONE)

`routing.kt` converted params (incl. `IncomingVaultConverter.findById` — a DB read) BEFORE
`checkAccess`. Consequences on a `Stored<T>`-param route: pre-auth DB reads for unauthenticated
callers, and a 404-vs-401 existence oracle. **Closed** by two-phase evaluation (below).

## Problem B — cross-org param spoofing / referential consistency (REWORKING)

`/orgs/{org}/entities/{entity}`: nothing forces `entity.org == org`, nor that the caller may access
`{org}`. Forgetting either = cross-org IDOR. Must be structural, not remembered.

---

## Design — two-phase eval (DONE) + pluggable route checks (TODO)

### Two-phase rule evaluation (Problem A) — DONE

1. **Phase 1 — caller-only rules, BEFORE param conversion.** Classified by `isCallerOnly()`
   (`AuthPhase.kt`): `PublicRule`/`ForbiddenRule`/`PermissionsCheck`/`AccessLevelCheck` and all-
   caller-only composites. Evaluated via `AuthRule.estimate` (needs no params); `estimate ≡ check`
   for every caller-only type. The mandatory floor (part 2) is caller-only, so it always gates here.
2. **Param conversion** second (findById now only for callers past the floor).
3. **Phase 2 — param-dependent rules** (`CallCheck`, the interface-triggered auto-rules, and the
   injected guards below) after conversion.
4. **Top-level AND flattening** (`flattenTopLevelAnds`): a top-level `forAll { caller; param }` is
   split so the caller-only conjunct still gates phase 1 (realizes "an AND splits its members"; ORs
   stay whole — not associative with the outer AND).
5. **Failure semantics:** consistency / org-isolation failures answer **404 byte-identical to
   not-found** (`HideFailureAsNotFound` throws the SAME `NotFoundException` a converter miss throws).

### Pluggable route checks (Problem B) — the rework

Two **extension points** replace the hard-coded checks + the org-specific auto-rules. The REST core
stays org-agnostic; saas owns org semantics with CONCRETE types (no `Storable<*>` star projection,
so `Storable.hasSameIdAs` is usable):

```kotlin
// funktor/rest — boot
interface RouteBootCheck   { fun validate(route: ApiRoute<*>): List<String> }        // errors, empty = ok
// funktor/rest — phase 2, per request
interface RouteParamsGuard { fun guard(params: Any, permissions: UserPermissions): GuardVerdict }  // Pass | DenyAsNotFound
```

`ValidateRoutesOnAppStarting` becomes a thin **runner**: inject `Lazy<List<RouteBootCheck>>`, run
each over every route, aggregate, throw one actionable `AppStartException`. The phase-2 dispatch runs
`getAll(RouteParamsGuard)` (from the request kontainer) after `checkParamPhase`; any `DenyAsNotFound`
→ `NotFoundException` (404).

**Org-isolation lives in `funktor/saas`** (concrete `Organisation`), two separate classes:

```kotlin
// funktor/saas
interface OrgAware      { val org: Ref<Organisation> }     // on ENTITIES (dev opt-in; review/linter enforces)
interface OrgAwareParam { val org: Stored<Organisation> }  // on route PARAMS

class OrgIsolationBootCheck : RouteBootCheck   // params resolving an OrgAware entity MUST be OrgAwareParam
class OrgIsolationGuard     : RouteParamsGuard // caller-binding + org-consistency, DenyAsNotFound
//   caller-binding : permissions.hasOrganisation(param.org._key)          (SELECTED session org, not accessibleOrgs)
//   org-consistency: for each OrgAware entity field  entity.org hasSameIdAs param.org
```

### Decisions locked with user (2026-07-22)

- **Two separate interfaces** (`RouteBootCheck` + `RouteParamsGuard`), and org-isolation ships as
  **two separate classes**, not one implementing both — concerns fully separated.
- **`ConsistentParam` = pure opt-in.** Keep the interface + its construction-appended auto-rule
  (`ConsistentParamRule`, runs in phase 2), but **DROP the `≥2-entities` boot-forcing** and any
  "uncovered params" auto-detection (too magic). A dev opts in; review/linter catches misses.
- **Caller-binding binds the SELECTED org** (`hasOrganisation`), never `accessibleOrgs` (which is
  documented "Non-authz"). Fixes round-1 finding.
- **`OrgAware` on entities is opt-in** (the param-side forcing only fires once an entity is marked);
  a lint/boot rule to flag org-owned entities missing `OrgAware` is **future scope** (follow-up task).
- Migrate the boot-RESIDENT checks (converter-compat, auth-chain) onto `RouteBootCheck` now; the
  construction-time `validateUriPattern` (param-names) moves in the follow-up (it's a behavior shift).

---

## Implementation plan (ordered)

**A. Extension points (funktor/rest)**
- [ ] `RouteBootCheck.kt` — the boot interface.
- [ ] `RouteParamsGuard.kt` — the request interface + `GuardVerdict { Pass, DenyAsNotFound }`.

**B. Migrate boot checks onto the runner (funktor/rest)**
- [ ] `ConverterCompatBootCheck : RouteBootCheck` (takes `OutgoingConverter`) — wraps
      `TypedRoute.validateConverterCompatibility`.
- [ ] `AuthChainBootCheck : RouteBootCheck` — the empty-chain + `validateChain` checks.
- [ ] Rewrite `ValidateRoutesOnAppStarting` as a runner injecting `Lazy<List<RouteBootCheck>>`;
      keep the actionable aggregated `AppStartException` header + the multiline-indent formatting fix
      (see `20260722-actionable-boot-error-messages.md`).
- [ ] Register both checks in the rest module (`funktor/rest/.../index_jvm.kt`).

**C. Request-time guard wiring (funktor/rest)**
- [ ] `routing.kt` phase 2 (param-bearing variants only — WithParams/WithBodyAndParams/Sse): after
      `checkParamPhase`, run `call.kontainer.getAll(RouteParamsGuard)`; `DenyAsNotFound` → throw
      `NotFoundException`. Keep the existing `HideFailureAsNotFound`/401 handling for authRules.

**D. Remove the org auto-rules + ConsistentParam forcing (funktor/rest + core)**
- [ ] Delete `OrgScopedParam` (core `ConsistentParam.kt`) and `CallerScopedParamRule`
      (`ParamAutoRules.kt`) and its append in `ApiRoutes.addRoute`.
- [ ] Keep `ConsistentParam` + `ConsistentParamRule` + its opt-in append; delete
      `consistencyForcingError` from the boot validator (no more `≥2` forcing).

**E. Org-isolation (funktor/saas)**
- [ ] `OrgAware` + `OrgAwareParam` interfaces (concrete `Ref<Organisation>` / `Stored<Organisation>`).
- [ ] `OrgIsolationBootCheck` — reflect `route.typedRoute.reifiedParamsType` via
      `entityRefParams()`; if any entity-ref inner type is `OrgAware` and the params type is not
      `OrgAwareParam` → actionable error.
- [ ] `OrgIsolationGuard` — `params as? OrgAwareParam ?: Pass`; caller-binding via
      `hasOrganisation(param.org._key)`; org-consistency via reflected OrgAware entity fields (cache
      the field list per params `KClass`), `entity.org hasSameIdAs param.org`; fail → `DenyAsNotFound`.
- [ ] Register both in the saas module (`funktor/saas/.../index_jvm.kt`).

**F. Tests**
- [ ] Unit (rest): `RouteBootCheck` runner aggregation; `ConverterCompatBootCheck` +
      `AuthChainBootCheck` still catch what the old inline checks did; `ConsistentParam` opt-in
      (rule appended + runs, NO forcing); keep `AuthPhaseSpec` (flattening etc.).
- [ ] Unit (saas): `OrgIsolationBootCheck.validate` (OrgAware entity w/o OrgAwareParam → error; with
      → ok; non-org param → ok); `OrgIsolationGuard.guard` (wrong selected org → deny; foreign entity
      org → deny; matching → pass; non-OrgAwareParam → pass/abstain).
- [ ] e2e (funktor:all, BOTH backends): rework `ConsistentParamE2eSpec` → seed real `Organisation`s
      (acme/globex) via `OrgsStorage`; `Widget : OrgAware(Ref<Organisation>)`; params
      `: OrgAwareParam(Stored<Organisation>)`. Assert: anonymous → 401 identical + no read (oracle);
      authed matching org → 200; caller selected other org (even if accessible) → 404; foreign
      entity org spoofed into own-org URL → 404; boot-force: an OrgAware-entity route whose params
      omit `OrgAwareParam` fails app start.

**G. Docs + hygiene + gate**
- [ ] Update `ConsistentParam.kt` KDoc (opt-in; point to saas `OrgAware` for isolation); saas
      interface KDoc; the docs collector `20260722-docs-auth-dsl-and-floor.md`.
- [ ] Create follow-up task: migrate `validateUriPattern` onto `RouteBootCheck` + the entity-`OrgAware`
      linter (see Cross-references).
- [ ] Run the FULL round-2 gate over the whole reworked diff; loop to zero findings.

## Test evidence

- [ ] Unit + saas-unit green (above).
- [ ] e2e (AppSpec, BOTH DB backends) green (above).
- [ ] Boot test: OrgAware-entity route without `OrgAwareParam` ⇒ app start fails (actionable).
- [ ] Full backend suites green: `funktor:rest`, `funktor:all`, `funktor-demo:server`.

## Cross-references

- Depends on parts 1+2 (`20260722-authorize-rule-builder.md`, `20260722-apiroutes-auth-floor.md`).
- Prerequisite of part 4 (`20260722-stored-param-migration.md`) — the oracle goes live without this.
- Pairs with `20260722-actionable-boot-error-messages.md` (the runner owns the aggregated boot error;
  fold the multiline-indent fix here).
- FOLLOW-UP: `20260722-route-check-followups.md` — migrate `validateUriPattern` onto `RouteBootCheck`,
  the entity-`OrgAware` linter, and branch-level (sub-org) isolation.
- `20260718-redteam-saas-orgs.md` — org-IDOR scenarios this closes structurally; update its assumptions.
