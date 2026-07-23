# Migrate framework APIs from handler findById to Stored params

**Status:** IN PROGRESS (started 2026-07-23) — part 3 landed (DONE), the hard dependency is met.
HARD dependency on `20260722-two-phase-auth-consistent-params.md` (shipping this first would turn
the pre-auth load/oracle problem live)
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

## Implementation plan (ordered, 2026-07-23)

Findings from the pre-implementation sweep:
- Sites: OrgsApi (get:35, update:83) + FunktorConfApi/AdminApi (11 sites over Event/Speaker/Attendee).
- All routes are `/{id}`; clients send `"id" to id`. **The ctor field must stay named `id`** (the
  incoming converter matches `routeParams[ctorParam.name]`), so retype the field, keep the name.
- `mount(X::class)` captures the params type via reified `kType<PARAMS>()` — nested `Stored<T>` ctor
  types are preserved. `IncomingVaultConverter` converts `Stored<X>` when `db.hasRepositoryStoring(X)`
  — true once a backend is selected (`useKarango`/`useMonko`; conf repos are Monko `@Vault`).
- **Envelope parity:** a binding miss throws `NotFoundException` → `ApiStatusPages` →
  `ApiResponse.notFound<Any>().withError("The Resource … was not found")` = **404, data null**,
  the same STATUS the handler's `okOrNotFound(null)` / `notFound()` produced (the binding path adds a
  harmless error message). Assert on status.
- **Ordering:** floors here are `isSuperUser()` / `public()` / `authenticated()` — all caller-only
  (phase 1), so they gate before conversion. No pre-auth load for a denied caller.

Steps (each builds + commits):
1. Migrate **OrgsApi** (saas): `IdParam(val id: Stored<Organisation>)`; `get` → `ok(params.id.asApiModel())`;
   `update` → drop the null branch, `params.id.modify { … }`. Build `funktor:saas`.
2. Migrate **FunktorConfApi + FunktorConfAdminApi** (demo): split `FunktorConfIdParams` into
   `EventIdParams`/`SpeakerIdParams`/`AttendeeIdParams` (`Stored<Event/Speaker/Attendee>`); simplify
   the 9 handlers (get ×3, update ×3, delete ×3). No ≥2-entity routes here → no `ConsistentParam`.
   Build `funktor-demo:server`.
3. **e2e (funktor:all)** `StoredParamMigrationE2eSpec`: BOTH backends via self-contained counting
   repos (`KaThing`/`MoThing`, floor-protected `Stored<Thing>` route) — assert denied-caller → no
   repo read (ordering/oracle), real id → 200, missing id → 404 (envelope parity). PLUS the real
   `/api/orgs/{id}` (karango, super-user): real → 200, missing → 404.
4. **e2e (funktor-demo:server, Monko)** `FunktorConfApiTest`: real conf endpoints post-migration —
   getEvent real → 200 / missing → 404; admin update real → 200 / missing → 404; anonymous on an
   admin route → 401.
5. Full backend suites green (`funktor:rest`, `funktor:saas`, `funktor:all` both backends,
   `funktor-demo:server`); verify codegen/docs unchanged. Then the FULL review loop to zero findings.

## Cross-references

- Depends on: `20260722-two-phase-auth-consistent-params.md` (hard), which depends on
  `20260722-apiroutes-auth-floor.md` → `20260722-authorize-rule-builder.md`.
- Proving ground for the whole quartet: after this, a new endpoint gets floor + phase ordering +
  consistency enforcement by construction, with nothing to remember.
