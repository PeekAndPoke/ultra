# Two-phase auth evaluation + ConsistentParam — no loads before auth, no cross-org param spoofing

**Status:** TODO (designed + agreed 2026-07-22) — depends on `20260722-apiroutes-auth-floor.md`
**Plan:** part 3 of the auth-hardening quartet
**Security-critical:** YES — request-data isolation (IDOR) + pre-auth resource access.

## Review protocol (user directive, 2026-07-22)

Same LOOP as `20260722-authorize-rule-builder.md`: full 3-agent gate → fix ALL confirmed findings
→ FULL re-review with fresh reviewers → repeat until a zero-findings round. One pass is not
enough. ESCALATION: if a round shows the direction itself is wrong, stop and consult the user.

## Problem A — everything is loaded before the auth checks (MUST fix; user-confirmed)

`funktor/rest/src/jvmMain/kotlin/routing.kt:172-174` converts params BEFORE `checkAccess`.
Param conversion includes `IncomingVaultConverter` (`funktor/core/.../broker/vault/vault.kt`),
which does `repository.findById(value)` — a DB read. Consequences on any protected route with a
`Stored<T>` param:

- **Pre-auth DB reads** for unauthenticated callers (cost + DoS amplification).
- **Existence oracle:** unresolvable id ⇒ ktor `NotFoundException` ⇒ 404 (from
  `IncomingConverter.kt:55-66`), while a valid id proceeds to auth ⇒ 401. An anonymous prober
  enumerates entity ids by 404-vs-401. Latent today (no route uses Stored params yet); goes LIVE
  with part 4's migration — therefore this task is a HARD prerequisite of part 4.

## Problem B — referential consistency of multi-entity params is per-handler discipline

`/orgs/{org}/entities/{entity}`: nothing forces the handler to verify `entity.org == org`.
Forgetting the check = cross-org IDOR via param spoofing (consistent-looking request, foreign
entity). The check must be structural, not remembered.

## Design (DECIDED 2026-07-22)

### Two-phase rule evaluation (Problem A)

1. **Phase 1 — caller-only rules, BEFORE param conversion.** The existing type split already
   classifies: `PermissionsCheck` / `AccessLevelCheck` evaluate `EstimateCtx` (caller only, no
   params). The mandatory floor (part 2) is caller-only by shape, so the floor ALWAYS runs before
   any DB is touched. Phase-1 failure ⇒ 401/403, zero reads, nothing learned.
2. **Param conversion second** (unchanged mechanics: concurrent per-param, not-found ⇒ 404 —
   now only reachable by callers who passed the floor).
3. **Phase 2 — param-dependent rules** (`CallCheck`, the auto-rules below) after conversion.
4. **Composite classification:** an AND splits its members across phases (each member runs in its
   natural phase); an OR containing any param-dependent member cannot be decided early ⇒ whole OR
   is phase 2. Verify every existing rule impl classifies cleanly; document the law.
5. **Failure semantics:** consistency/caller-binding failures (below) answer **404, byte-identical
   to not-found** — a 403 here would be an existence oracle across orgs ("exists, just not
   yours"). Other phase-2 rules keep their current failure shape.

### ConsistentParam (Problem B)

6. **Interface** (place near the broker converters): `interface ConsistentParam { fun
   isConsistent(): Boolean }` — **no default implementation** (a `= true` default would recreate
   the silent hole; forced bodies keep review attention on the small, greppable implementations).
   ```kotlin
   data class MyParams(
       val org: Stored<Organisation>,
       val entity: Stored<Entity>,
   ) : ConsistentParam {
       override fun isConsistent(): Boolean = entity.value.orgId == org._key
   }
   ```
7. **Auto-appended phase-2 rule** whenever `PARAMS : ConsistentParam` — appended by the
   framework at mount, never by the author. No way to forget it.
8. **Boot-time forcing** in `ValidateRoutesOnAppStarting` (precedent: it already validates
   outgoing-converter compatibility per route): a param class with **≥ 2 resolved `Stored<*>` /
   `Storable<*>` ctor fields MUST implement `ConsistentParam`** (reuse the converter's
   type-inspection); otherwise boot fails. Trivial one-entity/no-entity params stay clean — no
   rubber-stamp `= true` noise, and the requirement lands exactly where inconsistency is possible.
9. **Caller-binding companion (the second half of IDOR):** `isConsistent()` checks the loaded
   graph against itself — a foreign org's entity requested via the foreign org's own URL is
   "consistent" yet cross-tenant. Add an org-carrying param interface (e.g. `OrgScopedParam {
   val orgId: String }`-shaped; exact shape + module placement — `funktor/saas` knows
   `Organisation`, the broker does not — is an implementation decision) with an auto-appended
   phase-2 rule checking the param org against the CALLER's session (`permissions.org` /
   `canAccessOrg`). Same trigger pattern as ConsistentParam: interface ⇒ rule, structural.
   NOTE: this cannot live in the ctor seed (the seed is caller-only/param-agnostic) — it is
   interface-triggered per param type, which is the precise mechanism anyway.
10. **CSRF check item:** `IncomingConverter` validates CSRF tokens against `params.toString()`;
    once params contain `Stored` entities the string includes entity content. Verify no
    CSRF-protected broker route uses entity params (or fix the token basis) — rule it out, do
    not assume it.

## Spec

- [ ] Phase-1/phase-2 evaluation in the REST dispatch (`routing.kt`): floor + caller-only rules
      before conversion; conversion; param rules.
- [ ] Composite phase classification implemented + documented (AND splits, OR promotes).
- [ ] `ConsistentParam` + auto-rule; failure ⇒ 404 identical to not-found.
- [ ] Caller-binding interface + auto-rule; failure ⇒ 404.
- [ ] Boot validation: ≥ 2 resolved entities ⇒ interface required.
- [ ] CSRF/toString interaction ruled out or fixed.
- [ ] Full backend e2e suites green.

## Test evidence

- [ ] Unit: phase classification per rule type; composite laws.
- [ ] e2e (AppSpec, BOTH DB backends — storage is touched): anonymous request to a protected
      `Stored`-param route: (a) performs NO repository read (observable via a counting/spy repo
      or insights), (b) returns identical 401 for existing and non-existing ids — the oracle test.
- [ ] e2e: inconsistent param pair ⇒ 404 identical to not-found; consistent + authorized ⇒ 200;
      consistent but foreign-org caller ⇒ 404 (caller-binding).
- [ ] Boot test: two-entity param without `ConsistentParam` ⇒ app start fails.

## Cross-references

- Depends on parts 1+2 (`20260722-authorize-rule-builder.md`, `20260722-apiroutes-auth-floor.md`).
- Prerequisite of part 4 (`20260722-stored-param-migration.md`) — the oracle goes live without this.
- `20260718-redteam-saas-orgs.md` — org-IDOR scenarios this closes structurally; update the
  red-team doc's assumptions when this lands.
