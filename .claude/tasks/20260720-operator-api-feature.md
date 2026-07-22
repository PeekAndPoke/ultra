# OperatorApiFeature — real dashboard stats (+ groundwork for users-by-org)

**Status:** DONE (2026-07-20) — dashboard stats shipped; users-by-org deferred (see notes)
**Plan:** `.claude/plans/20260719-demo-restructure-three-apps.md` → ops-app "real dashboard stats"
**Security-critical:** yes (super-user operator API; new routes guard the operator surface)

## Spec

- [ ] `OperatorApiFeature` (server) exposing `GET /api/operator/dashboard/stats`, guarded by
      `authorize { isSuperUser() }`.
- [ ] `OperatorDashboardStats` + `OperatorApiClient` in `funktor-demo/common` (shared with ops-app).
- [ ] Server aggregates: org count, branch count, orgs-by-status, operator-user count.
- [ ] ops-app `DashboardPage` shows the real stats via `Apis.operator.getDashboardStats()`.
- [ ] e2e: super-user gets stats (200); anonymous/non-super-user rejected.

## Scope notes

- **users-by-org is deferred**: it needs the b2b/b2b2c realms + a cross-realm user-lookup-by-org
  design, and those realms don't exist yet. Recorded as a follow-up; org memberships live on tenant
  users, which only appear once those stores exist.
- First cut aggregates from `funktorSaas.findAll()` server-side (compact stats out); dedicated
  count/aggregation queries on `OrgsStorage` are a later optimization if org counts grow.
- Heed the kontainer SemiDynamic singleton trap — the API feature/routes are stateless, so no
  shared-state ctor-dep concern here.

## Coordination

Built while the security agent is on standby. Touches only my lane (`funktor-demo/common`,
`funktor-demo/server/operator`, `funktor-demo/ops-app`); routes are explicitly `isSuperUser()`-guarded
so the security agent's route-auth-default-deny work does not interact. Commits stage only these files
— the 8 in-flight security task docs are left untouched.

## Test evidence

- [x] `:funktor-demo:common` (JVM+JS), `:funktor-demo:server:compileKotlin`,
      `:funktor-demo:ops-app:compileKotlinJs` green.
- [x] e2e `OperatorApiTest`: anonymous ⇒ 401 (also proves mount); operators-realm super-user ⇒ 200
      with count assertions (`operators ≥ 1`, `orgs ≥ 2`, by-status buckets sum to the org total);
      **admin-realm super-user ⇒ 401** (realm boundary); **b2b tenant user ⇒ 401**. Green 2026-07-22.

## Review record (/feature-review catch-up gate, 2026-07-22, 3× Opus)

The feature shipped with coordinator self-review only (recorded below as superseded); the full
3-agent gate ran 2026-07-22. Both the domain and security reviewers independently confirmed the
same HIGH; the security reviewer traced full reachability:

| Reviewer | Verdict | Key findings |
|---|---|---|
| 1. Impl & style | 1 HIGH | HIGH happy-path e2e missing AND its "no sign-in helper" justification was wrong (all primitives existed: `signIn` over HTTP, `authenticate(token)`, seeded super-user fixture); MEDIUM DashboardPage blank on API error (loader can't reach its error state); LOW needless `asApiModel()` to count |
| 2. Domain | 1 HIGH | HIGH realm-agnostic guard (below); MEDIUM flat per-status fields fight a growable `OrgStatus` enum; LOW response model without defaults; LOW load-and-map-to-count teaches the wrong pattern |
| 3. Security | 1 HIGH | **HIGH cross-realm privilege escalation**: guard = `isSuperUser()` only; all realms share one signing key; the ADMIN realm mints `isSuperUser=true` tokens (seeded fixture = live exploit) ⇒ admin-realm user reads the operators-only console. B2B not exploitable (never sets isSuperUser). MEDIUM: no test locked the boundary |

**All confirmed findings fixed (2026-07-22):**
- **`forUserType(type)` AuthRule primitive** added to funktor/rest (companion + builder), checking
  the JWT `user/type` claim — the realm-boundary primitive `20260719-cross-realm-authz-and-tests.md`
  prescribed. Guard is now `forAll(isSuperUser(), forUserType(OperatorUserModel.USER_TYPE))`.
- e2e extended: operators-realm happy path (200 + counts), admin-realm super-user ⇒ 401,
  b2b user ⇒ 401 — the regression locks for the escalation.
- Stats model: `orgsByStatus: Map<String, Int>` (key = `OrgStatus.name`; `funktor-demo/common`
  cannot depend on `funktor:saas`, and string keys keep older clients deserializing newer servers)
  + all fields defaulted. Handler counts over domain objects (no `asApiModel()`).
- DashboardPage: `data!!` so non-2xx engages the loader error/retry state; by-status tiles render
  the map additively (known statuses always shown, future ones appended).
