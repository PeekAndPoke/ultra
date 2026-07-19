# Cross-realm authorization boundary + security tests

**Status:** DESIGN GAP flagged + tests TO BUILD (2026-07-19). Security-critical.
**Test bed:** the three-realm `funktor-demo` (operators / b2b / b2b2c) — write these as `AppUnderTest`
e2e tests that authenticate as one realm and assert rejection at another realm's endpoints.

## The gap (current behaviour)

- All realms share ONE JWT signing key (`funktor.auth.jwt.signingKey`), so any valid JWT passes the
  shared `authenticate(AUTH_JWT, AUTH_ANON)` gate on ANY endpoint. `jwtCaller` does not check the
  issuing realm.
- Authorization is permission-based (`AuthRule`s on roles/permissions/isSuperUser/org) — realm-agnostic.
  Realm B rejects a realm A user only if the permission check excludes them, NOT because of realm.
- Only existing cross-realm guard: `refreshToken`'s `expectedUserType` check.
- Discriminator available but unenforced: the JWT `type` claim (`*Model.USER_TYPE`).

## Recommended primitive (build before/with the tests)

1. Near-term: a `forUserType(type)` / `forRealm(realmId)` `AuthRule` checking the token `type` claim,
   applied per `ApiFeature` (e.g. an `OperatorApiFeature` requires `type == OperatorUserModel.USER_TYPE`).
2. Stronger: per-realm signing keys + per-host `jwtCaller` (host `ops.*` only accepts operator tokens)
   → wrong-realm tokens fail signature verification outright.

## Decision to make

Are operators omnipotent (super-user satisfies every gate on every app), or should even operators be
blocked from tenant-user endpoints except via an explicit, audited **impersonation** path? Recommend
the latter for anything that mutates tenant data.

## Security-test matrix (write against funktor-demo)

Realms: `operators`, `b2b`, `b2b2c`. For every ordered pair (issuer ≠ target):

- [ ] Token from realm A → a realm-B-only endpoint ⇒ **401/403** (6 pairs). Until the primitive lands,
      these fail — that's the point; they lock the boundary once built.
- [ ] **Role-name collision:** a `b2b` user with role `"admin"` → a `b2b2c` endpoint gated on
      `"admin"` ⇒ rejected (realm boundary must dominate a matching role string).
- [ ] **Cross-realm refresh:** `refreshToken` with a realm-A token against realm B ⇒ rejected
      (`expectedUserType` guard) — regression-lock the one guard that exists.
- [ ] **Cross-realm org-selection:** an `OrgSelectionToken` minted in realm A used at realm B's
      `select-org` ⇒ rejected (`findByToken` is realm-scoped — assert it).
- [ ] **Same email, different realms:** the same email seeded in all three stores; a token from one
      realm must act only as that realm's identity, never grant the others' access.
- [ ] **Operator scope:** operator token → tenant mutation endpoint ⇒ behaviour matches the decision
      above (blocked, or allowed-and-audited).
- [ ] Baseline crypto: forged/tampered signature ⇒ 401; expired JWT ⇒ 401 (already covered by
      `AuthApiSpec` for one realm — extend across realms).

## Cross-references

- `20260718-redteam-saas-orgs.md` (org IDOR / isSuperUser escalation) — overlaps; this is the
  realm-level superset.
- `20260717-auth-orgs-foundation.md` (realm ≠ tenant; `type` claim; refresh guard).
- `20260719-demo-restructure-three-apps.md` (the three realms this suite runs against).
