# UserPermissions org refactor (Phase O0)

**Status:** IN REVIEW — implementation complete + tests green (2026-07-17)
**Plan:** `.claude/tasks/20260717-auth-orgs-foundation.md` → Phase O0
**Security-critical:** yes (auth permission model — red-team follow-up required)

## Spec

Refactor `UserPermissions` to the Model C shape. Everything else in the plan builds on this.

- [x] `organisations: Set<String>` → `org: String?` (single selected org)
- [x] add `accessibleOrgs: Set<String>` (all orgs the user may log into)
- [x] rewrite organisation helpers: `hasOrganisation(x)` single-valued, add `canAccessOrg(x)`;
      drop `hasAllOrganisations`; **kept** `hasAnyOrganisation` (redefined "active org ∈ set" — keeps
      `AuthRule.forAnyOrganisation` unchanged and is still meaningful)
- [x] `mergedWith`: `org = other.org ?: this.org` (fallback avoids clearing on org-less merge);
      `accessibleOrgs` unioned like the other set fields
- [x] leave `branches`/`groups`/`roles`/`permissions` helpers untouched
- [x] JWT `builder.kt`/`extract.kt`: `org` string claim + `accessibleOrgs` array claim
- [x] downstream call sites: `AuthRule` needed **no change**; fixed `AuthState.kt` JS decode
      (funktor/auth), demo `AdminUserRealm`, funktor/auth `index_jvmTest`
- [x] update specs: `UserPermissionsSpec` (+ new `canAccessOrg` + merge-fallback cases), `UserSpec`,
      `JwtGeneratorSpec`, `JwtPermissionsRoundTripSpec`

## Implementation notes

Step 1a: `ultra/security` core + specs green in isolation (`:ultra:security:jvmTest`).
Step 1b: downstream call sites + their module tests green.
`AuthRule.forOrganisation`/`forAnyOrganisation` (`funktor/rest/.../auth/AuthRule.kt:65,73`) compiled
unchanged because both helpers were preserved.

## Test evidence

- [x] `:ultra:security:jvmTest` green (baseline confirmed green first, then post-refactor green)
- [x] `:funktor:rest:jvmTest`, `:funktor:auth:jvmTest` green
- [x] `:funktor:auth:compileKotlinJs` (JS decode) + `:funktor-demo:server:compileKotlin` compile
      (transitively builds `funktor:all` + deps) — full command green in 4m07s

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

**Red-team follow-up:** to be created on completion (permission-model tampering, org claim forgery).
