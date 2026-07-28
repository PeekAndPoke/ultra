# Email canonicalization on lookup (framework auth gap)

**Status:** ✅ CLOSED 2026-07-26 by **Option C** — see `.claude/tasks/20260726-value-class-emailaddress.md`.

> The decision this task was waiting for was made by scheduling Step 4 of the value-class-ids
> migration. Option C ("an `EmailAddress` value type … strongest (wrong usage unrepresentable)") was
> judged premature when this was written; it became the plan, so all five gaps below are now closed
> STRUCTURALLY rather than by remembering to canonicalize:
> `loadByEmail`/`findByEmail` take an `EmailAddress`, which can only be built canonically, so a
> non-canonical value can no longer reach a lookup. Options A and B were not taken.
>
> Gap-by-gap: password-reset init, Google SSO login + signup-existence, and GitHub SSO login +
> signup-existence all now go through `EmailAddress.of` / `parseOrNull`. The add-member leaf fix
> (`c5398382`) is now redundant with the type and was folded in.
>
> ⚠️ Follow-up that came OUT of that work and is NOT closed here: SSO signup links to an existing
> local account without checking the provider's `email_verified` — see
> `.claude/tasks/20260726-sso-email-verification.md`.
**Found via:** the add-member-by-email question (2026-07-24). The leaf bug (`B2bMembersApi` add lookup)
is already FIXED (`c5398382`); this task is the BROADER framework gap.
**Type:** framework correctness (auth).



## Commits & files changed

<!-- Generated 2026-07-28 from `git log --follow --name-status` over this task file.
     Commits that merely renamed the doc into the archive (R100) are excluded, since
     their code belongs to whatever task shipped alongside the move. -->

| Commit | Date | Files | Subject |
|---|---|---:|---|
| `0f29d239` | 2026-07-26 | 52 | feat(security): EmailAddress value class — case-insensitive email lookup by type |

### Files changed (52)

**docs-site/src**
- `docs-site/src/data/llms/funktor.md`
- `docs-site/src/pages/ultra/funktor/auth.astro`

**funktor-demo/adminapp**
- `funktor-demo/adminapp/src/jsMain/kotlin/pages/ProfilePage.kt`

**funktor-demo/b2b-app**
- `funktor-demo/b2b-app/src/jsMain/kotlin/pages/DashboardPage.kt`
- `funktor-demo/b2b-app/src/jsMain/kotlin/pages/MembersPage.kt`

**funktor-demo/b2b2c-app**
- `funktor-demo/b2b2c-app/src/jsMain/kotlin/pages/DashboardPage.kt`

**funktor-demo/common**
- `funktor-demo/common/src/commonMain/kotlin/AdminUserModel.kt`
- `funktor-demo/common/src/commonMain/kotlin/B2b2cUserModel.kt`
- `funktor-demo/common/src/commonMain/kotlin/B2bUserModel.kt`
- `funktor-demo/common/src/commonMain/kotlin/OperatorUserModel.kt`
- `funktor-demo/common/src/commonMain/kotlin/b2b/B2bMembersModels.kt`

**funktor-demo/server**
- `funktor-demo/server/src/main/kotlin/admin/AdminUser.kt`
- `funktor-demo/server/src/main/kotlin/admin/AdminUserRealm.kt`
- `funktor-demo/server/src/main/kotlin/admin/AdminUsersRepo.kt`
- `funktor-demo/server/src/main/kotlin/b2b/B2bMembersApi.kt`
- `funktor-demo/server/src/main/kotlin/b2b/B2bRealm.kt`
- `funktor-demo/server/src/main/kotlin/b2b/B2bUser.kt`
- `funktor-demo/server/src/main/kotlin/b2b/B2bUsersRepo.kt`
- `funktor-demo/server/src/main/kotlin/b2b2c/B2b2cRealm.kt`
- `funktor-demo/server/src/main/kotlin/b2b2c/B2b2cUser.kt`
- `funktor-demo/server/src/main/kotlin/b2b2c/B2b2cUsersRepo.kt`
- `funktor-demo/server/src/main/kotlin/operator/OperatorRealm.kt`
- `funktor-demo/server/src/main/kotlin/operator/OperatorUser.kt`
- `funktor-demo/server/src/main/kotlin/operator/OperatorUsersRepo.kt`
- `funktor-demo/server/src/test/kotlin/B2bMembersApiTest.kt`

**funktor/all**
- `funktor/all/src/jvmTest/kotlin/FunktorApiSpec.kt`
- `funktor/all/src/jvmTest/kotlin/OrgIsolationE2eSpec.kt`
- `funktor/all/src/jvmTest/kotlin/StoredParamMigrationE2eSpec.kt`
- `funktor/all/src/jvmTest/kotlin/TestUserRealm.kt`

**funktor/auth**
- `funktor/auth/src/commonMain/kotlin/model/AuthUser.kt`
- `funktor/auth/src/jvmMain/kotlin/AuthRealm.kt`
- `funktor/auth/src/jvmMain/kotlin/AuthUserAdapter.kt`
- `funktor/auth/src/jvmMain/kotlin/domain/AuthRecord.kt`
- `funktor/auth/src/jvmMain/kotlin/provider/EmailAndPasswordAuth.kt`
- `funktor/auth/src/jvmMain/kotlin/provider/GithubSsoAuth.kt`
- `funktor/auth/src/jvmMain/kotlin/provider/GoogleSsoAuth.kt`
- `funktor/auth/src/jvmTest/kotlin/index_jvmTest.kt`
- `funktor/auth/src/jvmTest/kotlin/provider/EmailAndPasswordAuthSpec.kt`
- `funktor/auth/src/jvmTest/kotlin/provider/EmailCanonicalizationSpec.kt`
- `funktor/auth/src/jvmTest/kotlin/provider/GithubSsoAuthSpec.kt`
- `funktor/auth/src/jvmTest/kotlin/provider/GoogleSsoAuthSpec.kt`

**funktor/rest**
- `funktor/rest/src/jvmMain/kotlin/auth/Caller.kt`
- `funktor/rest/src/jvmTest/kotlin/auth/CallerSpec.kt`
- `funktor/rest/src/jvmTest/kotlin/auth/EstimateCtxSpec.kt`

**ultra/security**
- `ultra/security/src/commonMain/kotlin/jwt/JwtUserData.kt`
- `ultra/security/src/commonMain/kotlin/user/EmailAddress.kt`
- `ultra/security/src/commonMain/kotlin/user/UserRecord.kt`
- `ultra/security/src/jvmMain/kotlin/jwt/builder.kt`
- `ultra/security/src/jvmMain/kotlin/jwt/extract.kt`
- `ultra/security/src/jvmTest/kotlin/jwt/JwtGeneratorSpec.kt`
- `ultra/security/src/jvmTest/kotlin/user/EmailAddressSpec.kt`
- `ultra/security/src/jvmTest/kotlin/user/UserRecordSpec.kt`

## The gap
Emails are STORED canonically lowercased — every user-creation path routes through
`AuthUserAdapter.CreateUserForSignupParams.of(...)` which does `email.trim().lowercase()`
(email+password signup, Google SSO signUp, GitHub SSO signUp).

But LOOKUP canonicalization is a **per-caller convention**, not enforced at the seam. `loadByEmail`
(all realms) and the repos' `findByEmail` are **case-sensitive exact matches**
(`FILTER(user.email EQ email)`) and trust the caller to pass the canonical form. Callers are
inconsistent:

| Call site | Canonicalizes? | Consequence if not |
|---|---|---|
| `EmailAndPasswordAuth:221` login | ✓ `.trim().lowercase()` | — |
| `EmailAndPasswordAuth:247` signup existence | ✓ (uses canonical `createParams.email`) | — |
| `EmailAndPasswordAuth:309` password-reset init | ✗ raw `request.email` | mixed-case reset request → no user found → **no reset email sent** (silently) |
| `GoogleSsoAuth:163` login | ✗ raw `payload.email` | mixed-case provider email → login fails to match |
| `GoogleSsoAuth:187` signup existence | ✗ raw `email` (creation lowercases) | existence check misses → **duplicate user** / unique-index error |
| `GithubSsoAuth:179` login, `:204` signup existence | ✗ raw | same as Google |
| `B2bMembersApi:177` add-member | ✗ was `.trim()` only | **FIXED** `c5398382` |

Root cause: the case-insensitivity rule lives at N call sites instead of one choke point.

## Fix options (decide)
- **A — Canonicalizing choke point (recommended).** Add a framework `AuthUserAdapter.loadByEmailCanonical(raw)`
  default = `loadByEmail(canonicalEmail(raw))`, plus a shared `canonicalEmail(raw) = raw.trim().lowercase()`.
  Point ALL auth providers (login/reset/SSO) at the canonical entry; per-realm `loadByEmail` stays raw
  (still available for exact lookups). Fixes every provider gap uniformly + makes the next caller safe.
- **B — Shared helper only.** Introduce `canonicalEmail(raw)` and use it at the 3 missed framework call
  sites + `CreateUserForSignupParams`. Centralizes the RULE but still relies on callers remembering.
- **C — `EmailAddress` value type.** A domain type that is always canonical (constructed via
  trim+lowercase), used in signup params, lookups, and stored on the user model. Strongest (wrong usage
  unrepresentable) but the biggest refactor — likely premature per the "fool me once" principle.

Recommendation: **A** — it fixes the real correctness bugs (SSO duplicate users, reset-email-not-sent)
at one seam and hardens future callers, without the churn of C. Then this is a framework change → its
own 3-agent review + a doc-collector note.

## Test evidence (when implemented)
- [ ] Unit/e2e: login/reset/SSO resolve a mixed-case input to the lowercased stored user; SSO signup
      existence check with mixed case does NOT create a duplicate.
- [ ] `canonicalEmail` unit test (trim + lowercase + idempotent).

## Notes
- Local-part case-sensitivity is technically RFC-permitted, but this codebase already commits to
  lowercase-on-store, so lookups MUST match that — this is about internal consistency, not RFC nuance.
- The unique-email index is per-realm repo; canonical storage + canonical lookup together are what make
  it reliable.
