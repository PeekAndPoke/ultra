# Migrate framework APIs from handler findById to Stored params

**Status:** DONE (2026-07-23) — review loop CLOSED at round 3 (zero findings across all three fresh
reviewers). Migration + entity-based param naming + `defaultAuth`→`authFloor` floor rename. Green:
rest 105, saas 31, all 125 (both DB backends), demo 35. 8 commits on `auth-increments`, all UNPUSHED.
HARD dependency on `20260722-two-phase-auth-consistent-params.md` was met (part 3 DONE).
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

- [x] OrgsApi migrated (get + update) — params carry `Stored<Organisation>`. (2026-07-23)
- [x] FunktorConfApi migrated (11 sites) — entity params. No ≥2-entity route here → no `ConsistentParam`.
- [x] Sweep of remaining `findById(params.` sites: ZERO remaining (13 targets migrated; insights/inspect
      have no by-path-id handlers). Confirmed by grep + both domain & impl reviewers.
- [x] Envelope-parity e2e per migrated group; no-read-when-anonymous e2e on the migrated pattern
      (read + write shapes, both backends) — `StoredParamMigrationE2eSpec`.
- [x] Full backend suites green on both DB backends: rest 105, saas 31, all 125, demo 31.

## Review record

**Round 1 (3-agent gate, 2026-07-23) — 0 CRITICAL/HIGH/MEDIUM.** All findings LOW/INFO.
- FIXED (commit `aea7bfb2`, test-only): lock the no-double-load invariant (`counter==1` on the
  authorized read); assert the response body is the bound entity (not just status); add a
  `WithBodyAndParams` write-shape route with anonymous→401+`counter==0` ordering. `StoredParamMigrationE2eSpec` 6→10 tests.
- ESCALATED → user (design direction): no-backend saas 500-vs-404. Both security + domain flagged it
  (LOW, degenerate misconfig; framework already accepts the identical `OrgAwareParam`-without-saas
  tradeoff). **Resolution (user, 2026-07-23):** don't bolt on a one-off boot check — redesign funktor
  module config as a self-validating composable builder (each builder asserts its own invariants,
  fails boot actionably). Follow-up: `.claude/future-plans/funktor-module-config-builder.md`. Interim:
  loud KDoc on `OrgsApi.OrgParam`.
- NO-ACTION (INFO, verified not part-4 regressions): 404 error echoes request-uri (pre-existing part-3
  `ApiStatusPages`); shared read-counter (mirrors accepted `OrgIsolationE2eSpec` pattern, kotest
  sequential); `id` field holding `Stored<T>` (required by name↔segment match, documented).

**Naming (user feedback, 2026-07-23):** param classes named by ENTITY (not generic `IdParam`, which
collides across modules and muddles imports): `OrgsApi.OrgParam`, `EventParam`/`SpeakerParam`/
`AttendeeParam` (commit `8f7089c8`). Confirmed `Organisation` is NOT `OrgAware` (it is the tenant root,
not org-owned), so `OrgParam` is correctly NOT `OrgAwareParam` and the boot check does not force it; the
super-user floor is the right guard for org-root CRUD. (Pre-existing `JobIdParam` in funktor/cluster +
showcase `ItemParams`/`EchoParams`: user decided 2026-07-23 to KEEP as-is — `JobIdParam` is a fine name
for completion/import; the showcase may be reworked anyway. No sweep.)

**Round 2 (fresh 3-agent gate, 2026-07-23):** security — 0 findings; domain — 0 findings; impl — 1 LOW
(per-group envelope-parity e2e covered only the Event group). FIXED (commit `af9488f4`): added
`getSpeaker`/`getAttendee` binding (200) + envelope-parity (404) — `FunktorConfApiTest` 7→11 tests.

**Floor param rename (user feedback, 2026-07-23):** `ApiRoutes(defaultAuth = …)` → `authFloor` (it is a
floor route rules can only STRENGTHEN, not an overridable default) — commit `66e6f572`, ~23 groups +
tests + the actionable error messages/KDoc; no behavior change.

**Round 3 (fresh 3-agent gate over the full diff incl. both renames, 2026-07-23):** impl — 0; security
— 0; domain — 0 (one editorial KDoc nit, fixed). **LOOP CLOSED — zero findings.** Confirmed the
`authFloor` rename is name-only (every group's floor byte-identical; actionable messages/KDoc updated),
the entity-param renames are clean, no org-owned route slips isolation, and the migration semantics
(update precedence, delete-returns-entity, facade consistency, envelope parity, no pre-auth load on
both route shapes) all hold.

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
