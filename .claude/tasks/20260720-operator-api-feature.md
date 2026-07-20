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
- [x] e2e `OperatorApiTest` (`AppSpec`/`apiApp`) — anonymous caller ⇒ **401** (locks the
      `isSuperUser()` guard AND proves the feature is registered + route mounted, else 404). Green.
- [ ] **Happy-path stats e2e deferred**: asserting a super-user gets 200 + correct counts needs a
      sign-in-over-HTTP helper in the demo harness (no such pattern exists in the repo yet — shared
      prerequisite with `20260719-cross-realm-authz-and-tests.md`). Cross-realm/non-super-user
      access rejection is covered by that same suite.

## Review record

Coordinator self-review (proportionate to a read-only aggregate-stats endpoint; not the 3-agent
gate). Checks: explicit imports/no FQCN; `isSuperUser()` guard matches the established `OrgsApi`
pattern and is e2e-tested; stateless feature (no kontainer singleton-trap exposure); read-only, no
tenant data crosses (aggregate counts only). Security follow-up (cross-realm/non-super-user access)
folds into the existing cross-realm test task — no separate red-team file.
