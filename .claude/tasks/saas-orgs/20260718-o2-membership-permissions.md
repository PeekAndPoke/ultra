# O2 — Membership + permissions plumbing

**Status:** IN REVIEW — implementation complete + green (2026-07-18); review batched with O3
**Plan:** `.claude/tasks/saas-orgs/20260717-auth-orgs-foundation.md` → Phase O2
**Security-critical:** yes (permission construction) — review with O3, red-team covered by the auth-orgs sweep

## Spec / done

- [x] `OrgMembership(orgId, branchIds, roles)` + `HasOrgMemberships` — placed in **`ultra/security`**
      (next to `UserPermissions`), NOT `funktor/saas`. Rationale: avoids an inverted `auth → saas`
      dependency (auth + saas + rest all already see `ultra/security` via `funktor/core`).
- [x] `SelectedOrg(orgId, membership, planPermissions)` + `buildOrgPermissions(memberships, selected)`
      → `UserPermissions` (Model C: org, accessibleOrgs, selected-org branches/roles, plan perms).
      `ultra/security`. 3 unit tests (`OrgPermissionsSpec`) green.
- [x] `OrgPolicy` (None / Required(onSignup)) + `SignupOrgBehavior` (None / AutoJoin / CreateOwnOrg)
      — `funktor/auth`.
- [x] `AuthRealm.orgPolicy` (default None) + `AuthRealm.getMemberships(user)` (default via
      `HasOrgMemberships`) — non-breaking default methods.
- [x] `OrgPlan(name, featurePermissions)` + `plan` field on `Organisation` entity — `funktor/saas`.

## Deferred (recorded)

- `getMemberships` e2e test → lands with O3/demo (real realm + `HasOrgMemberships` user logging in).
- Branch-id server-minting/immutability → deferred until branch-management UX exists; not on the
  org-based login critical path. In-org uniqueness + non-blank already enforced (O1).

## Test evidence

- [x] `:ultra:security:jvmTest` green — `OrgPermissionsSpec` (3/3: no-selection, selected slice, empty).
- [x] `:funktor:auth:jvmTest` green (AuthRealm default-method additions non-breaking).
- [x] `:funktor:saas:jvmTest` green (entity `plan` field, KSP regen, storage unchanged).
- [x] `:funktor:all:jvmTest` green (full aggregate, no regression).

## Review record

Batched with O3 `/feature-review` (the permission-building + login-flow form one reviewable unit;
`buildOrgPermissions` is unit-tested, exercised e2e by O3's sign-in).
