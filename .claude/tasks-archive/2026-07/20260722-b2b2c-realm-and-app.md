# b2b2c realm (end-user tenant users) + b2b2c-app + shared org hooks

**Status:** DONE (2026-07-22) — reviewed (3-agent gate), all confirmed findings fixed, tests green.
**Plan:** `.claude/tasks/20260719-demo-restructure-three-apps.md` → b2b2c realm + app (third of three)
**Security-critical:** yes (third auth realm; tenant end-user sessions)

## What shipped

- `B2b2cUser` (`@Vault`, `AuthUser` + `HasOrgMemberships` + `language`) + `B2b2cUsersRepo`
  (`b2b2c_users`, unique email) + `B2b2cUserModel` (common, `USER_TYPE = "B2b2cUser"`).
- `B2b2cRealm`: `OrgPolicy.Required`, invite-only (`AuthError.notSupported()` on signup), 1h JWT.
- **`saas_org_hooks.kt` (shared)** — the hardened org-hook logic extracted from the reviewed b2b
  realm, now used by BOTH tenant realms (they cannot drift): `accessibleActiveOrgs` (active-only +
  dedupe), `resolveActiveSelectedOrg` (active-only + deterministic union-merge of duplicate
  membership rows, with a marked plan-permission divergence seam), `buildVettedOrgPermissions`
  (JWT accessibleOrgs claim from the vetted set — extracted after this feature's review).
- Fixtures: `noorg@` / `single@` / `multi@b2b2c.test` over the shared acme/globex orgs with the
  **distinct role vocabulary `"end-user"`** (NOT b2b's `"admin"`/`"member"` — see review).
- `b2b2c-app` (dev-server 36592): violet dedicated `LoggedOutLayout` ("End-user portal") wrapping
  the shared `AuthLogin`; dashboard; default reset page. CORS/baseUrls were pre-wired.
- `B2b2cAuthFlowTest`: 0/1/n sign-in, cross-store rejection (b2b2c creds at the b2b realm ⇒ 403),
  suspended-org denial. Lean by design — the deep selection-security matrix runs over shared
  code locked by `B2bAuthFlowTest` (the impl reviewer verified the one realm-local path,
  `generateJwt`, is exercised by the single-org Success case).

## Review record (/feature-review, 2026-07-22, 3× Opus)

| Reviewer | Verdict | Findings |
|---|---|---|
| 1. Impl & style | PASS | Faithful-clone audit clean (zero copy-paste leftovers; all realm ids/types/ports/keys verified); hooks extraction byte-equivalent; lean-test rationale sound. LOW: `generateJwt` vetting duplicated across realms |
| 2. Domain | PASS | Extraction shape + shared orgs domain-correct. MEDIUM: end-users reused role `"member"` over the same orgs → populations distinguishable only by the `type` claim (the cross-realm role-collision trap). LOW: shared hook grants full plan permissions to every population — mark the divergence seam |
| 3. Security | PASS, zero findings | Cross-store credential isolation traced BOTH directions on both DB backends (realm+owner-scoped lookups); selection tokens realm-bound; vetted JWT clone correct; no token leak in the app. Standing reminder: any future b2b/b2b2c-specific API surface must combine its permission rule with `forUserType(...)` |

**Fixed in this pass:** role vocabulary → `"end-user"` (MEDIUM); `buildVettedOrgPermissions`
extracted into the shared hooks and both realms delegate (LOW); divergence-seam comment on the
plan-permission grant (LOW).

## Test evidence

- [x] `:funktor-demo:server:compileKotlin` + `:compileTestKotlin`, `:funktor-demo:common` (JVM+JS),
      `:funktor-demo:b2b2c-app:compileKotlinJs` green.
- [x] Full `:funktor-demo:server:test` green incl. `B2b2cAuthFlowTest` (both realm suites re-run
      after the review fixes).

## Cross-references

- `tasks-archive/2026-07/20260720-b2b-realm.md` — the reviewed template this clones; its review
  drove the shared-hooks extraction.
- `.claude/tasks/saas-orgs/20260719-cross-realm-authz-and-tests.md` — role-collision risk (defused here via
  vocabulary) and the pending realm-boundary work for future tenant API surfaces.
- `.claude/tasks/20260720-auth-frontend-composability.md` — b2b2c re-copies the reset-route mount
  (third copy), strengthening the case for the deferred chrome-slot API.
