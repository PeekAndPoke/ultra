# O3 — Org-aware sign-in (0/1/n)

**Status:** IN PROGRESS — backend + frontend-compat done + green (2026-07-18); org-flow tests +
picker UI + demo realm/fixtures pending
**Plan:** `.claude/tasks/saas-orgs/20260717-auth-orgs-foundation.md` → Phase O3
**Security-critical:** yes (login flow, selection token) — `/feature-review` + red-team when the
demo login flow is complete

## Done (backend + frontend-compat, all compiles + full `funktor/all` suite green)

- [x] `AuthSignInResponse` sealed: `Success(token, realm, user, org?)` / `OrgSelectionRequired(
      selectionToken, organisations)`; new `AuthOrgRef` + `AuthSelectOrgRequest` models.
- [x] `AuthError.noOrganisationAccess`; `AuthRecord.OrgSelectionToken`; `RealmTokenConfig
      .orgSelectionTokenLifetime = 5.min`; `AuthRealm.tokenConfig` wired.
- [x] 0/1/n `issueSignIn(user)` in `AuthRealm` (single choke point → SSO free). **Auth stays free of
      a `saas` dep**: realm hooks `getAccessibleOrgs(memberships)` + `resolveSelectedOrg(orgId, …)`
      (default no-op). `selectOrg(token, orgId)` consumes the single-use token.
- [x] `generateJwt(user, selectedOrg)` signature; `refreshToken(userId, type, currentOrgId)`
      re-derives the same org; `AuthSystem.selectOrg` + `AuthApi` `select-org` endpoint + `AuthApiClient`.
- [x] Frontend `AuthState` handles both variants: `pendingOrgSelection` + `selectOrg(orgId)`.
- [x] All `generateJwt` implementers updated (AdminUserRealm, TestUserRealm, index_jvmTest,
      FunktorApiSpec token builders); `AuthApiSpec` reads narrowed to `Success`.

## Pending

- [ ] `Session.org` — deferred to Phase F (sessions not wired at sign-in yet).
- [ ] `forSelectedOrganisation` AuthRule (minor; `forOrganisation` already works via O0).
- [ ] **Org-flow tests**: a `Required` test realm implementing the two hooks; assert 0→noOrgAccess,
      1→auto-select, n→OrgSelectionRequired + select-org (single-use/expiry/wrong-org), refresh keeps org.
- [ ] **Demo**: end-user realm (`OrgPolicy.Required`) + `getAccessibleOrgs`/`resolveSelectedOrg` via
      `OrgsStorage`; fixtures — 2 orgs (a chain + branches), users with 1 and 2 orgs; `AutoJoin`/
      `CreateOwnOrg` signup wiring; login-page org-picker UI.
- [ ] **Unified frontend auth state** (`AuthContextData` stream = AuthState ⊕ mapAsync getMyApiAccess
      → ApiAcl) + fix the demo's missing `jwtDecoder` wiring (client permissions currently empty).

## Test evidence

- [x] `:funktor:auth` compiles (jvmMain, jvmTest, JS); `:funktor:all:jvmTest` full suite green;
      `:funktor-demo:server` compiles. Existing org-less flows (incl. admin login) unchanged.
- Note: observed a **pre-existing flaky** full-suite failure once (`AuthApiSpec` signup→signin
      `password4j: Invalid hashed value`) — shared ArangoDB test-DB state across specs; passes in
      isolation and on clean re-runs. Not an O3 regression. Worth a separate test-isolation task.
