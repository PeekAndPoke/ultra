# Migrate framework APIs from handler findById to Stored params

**Status:** TODO (designed + agreed 2026-07-22) — HARD dependency on
`20260722-two-phase-auth-consistent-params.md` (shipping this first would turn the pre-auth
load/oracle problem live)
**Plan:** part 4 of the auth-hardening quartet
**Security-critical:** YES — changes when/where entities load relative to auth on real endpoints.

## Review protocol (user directive, 2026-07-22)

Same LOOP as `20260722-authorize-rule-builder.md`: full 3-agent gate → fix ALL confirmed findings
→ FULL re-review with fresh reviewers → repeat until a zero-findings round. One pass is not
enough. ESCALATION: if a round shows the direction itself is wrong, stop and consult the user.

## What & why

The entity-resolving param binding already exists end to end (`IncomingVaultConverter` →
`convertIncomingParameters` → typed REST params), but every API loads by hand instead:
`val found = funktorSaas.findById(params.id)` + a null branch. Migrating params to `Stored<T>`
fields:

- deletes the handler load + null boilerplate (binding 404s before the handler),
- makes params the SINGLE entry point of entities into a request — the precondition for
  `ConsistentParam`/caller-binding to bite,
- removes double-loads (binding + handler),
- unifies not-found semantics.

Client contract is untouched: JS/codegen clients keep sending string ids matched by URI-pattern
name; the param CLASS is server-side (jvmMain) only. Verify codegen output is byte-identical.

## Sweep scope (grep `findById(params.` at implementation time; known today)

- `funktor/saas` OrgsApi:39 (get), :91 (update) → `Stored<Organisation>` param.
- `funktor-demo` FunktorConfApi — 11 sites (events ×3, speakers ×3, attendees ×3 + detail
  routes) → typed entity params; multi-entity routes (if any pair org/branch/entity) implement
  `ConsistentParam` per part 3's boot rule.
- Any `funktor/insights` / `funktor/inspect` routes loading by path id — sweep and decide
  (coordinate with the other agent's in-flight insights tasks; do NOT touch their task docs).
- Deliberate non-goals: lookups by non-id keys (findByEmail, findBySlug) stay in handlers;
  routes with legitimate maybe-semantics use `Stored<T>?` (nullable) and keep their explicit
  branch.

## Acceptance criteria

- **Envelope parity:** the binding's `NotFoundException` must surface as the SAME ApiResponse
  envelope/status the handler's `okOrNotFound` produces today — verify the `ApiStatusPages`
  mapping and lock it with e2e assertions on a missing id per migrated route group.
- **Ordering:** all migrated routes demonstrably evaluate the floor before loading (part 3
  machinery) — reuse the no-read-when-anonymous e2e pattern on at least one migrated route.
- **Both DB backends** for storage-touching e2e (CLAUDE.md rule; MatrixTest2d pattern).
- Generated clients (TS/Dart) and docs unchanged.

## Spec

- [ ] OrgsApi migrated (get + update) — params carry `Stored<Organisation>`.
- [ ] FunktorConfApi migrated (11 sites) — entity params; ConsistentParam where ≥ 2 entities.
- [ ] Sweep of remaining `findById(params.` sites: migrated or explicitly exempted with reason.
- [ ] Envelope-parity e2e per migrated group; no-read-when-anonymous e2e on one migrated route.
- [ ] Full backend suites green on both DB backends.

## Cross-references

- Depends on: `20260722-two-phase-auth-consistent-params.md` (hard), which depends on
  `20260722-apiroutes-auth-floor.md` → `20260722-authorize-rule-builder.md`.
- Proving ground for the whole quartet: after this, a new endpoint gets floor + phase ordering +
  consistency enforcement by construction, with nothing to remember.
