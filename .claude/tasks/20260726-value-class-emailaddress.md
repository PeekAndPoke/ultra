# `EmailAddress` value class (value-class-ids Step 4)

**Status:** IN REVIEW — gate round 2 findings applied (2026-07-26)
**Plan:** `.claude/tasks/20260724-value-class-ids-migration.md` → Step 4
**Closes:** `.claude/tasks/20260724-email-canonicalization.md` — this IS that task's **Option C**
("an `EmailAddress` value type … strongest (wrong usage unrepresentable)"), which was deferred at the
time as likely premature. It is now scheduled, so the 5 documented lookup gaps get fixed BY
CONSTRUCTION rather than by remembering to call `.trim().lowercase()`.
**Security-critical:** yes — a missed canonicalization silently fails to find a user (no password-reset
email sent) or silently creates a duplicate account on SSO signup.

## Naming: `EmailAddress`, not `Email`

`funktor/messaging/src/jvmMain/kotlin/Email.kt` already defines `Email` (the MESSAGE). Reusing the name
would force an import alias at every site that sends mail — including `AuthRealm.DefaultMessaging`,
which needs both. `EmailAddress` is also what the canonicalization doc's Option C called it, and it is
the more precise word: an address, not a message.

## Placement: `ultra/security` commonMain (same package as `UserId` / `OrgId`)

`ultra/common` would be the intuitive home — `isEmail()` and `EmailRegex` live there — but that module
has **no kotlinx-serialization plugin and no `@Serializable` class at all**, so putting a serializable
value class there means adding the plugin to a foundational module for no immediate gain.
`ultra/security` already has serialization + slumber, already owns `UserRecord.email`, and is already
visible to every consumer (`funktor:auth`, `funktor:saas`, `funktor-demo/common`). It can still use
`isEmail()` because `ultra:security` declares `api(project(":ultra:common"))`. One package now answers
"where do the value types live?".

## The `init` / `of` split — the point of this step

`init` VALIDATES (accept or reject, never transform); `of` NORMALIZES. `init` runs on every
construction INCLUDING deserialization, so transforming there would silently rewrite bad stored data.
Therefore:
- raw input (request body, SSO claim) → `EmailAddress.of(raw)`
- already-canonical value (DB row, another `EmailAddress`) → the constructor, which throws if wrong

Consequence, intentional: a stored non-canonical or invalid address now FAILS TO DECODE instead of
flowing on. Fail-loud, per the migration plan's cross-cutting caveat. All demo seeds are lowercase.

## Spec

- [x] `EmailAddress` in `ultra/security` commonMain — `init` requires canonical + `isEmail()` + ≤254
      (RFC 5321); `localPart` / `domain`; `of(raw)`; `parseOrNull(raw)`
- [x] `AuthUser.email: EmailAddress` (the interface every realm USER implements)
- [x] `CreateUserForSignupParams.email: EmailAddress` — `of()` already normalized, now typed
- [x] `AuthUserAdapter.loadByEmail(email: EmailAddress)` + all 4 demo realms + repos' `findByEmail`
- [x] demo user entities (`B2bUser`, `B2b2cUser`, `AdminUser`, `OperatorUser`) + their `*UserModel`s
- [x] `AuthRecord.EmailChangeToken.pendingEmail: EmailAddress`
- [x] **The 5 canonicalization gaps** — password-reset init, Google SSO login + signup-existence,
      GitHub SSO login + signup-existence — now go through `EmailAddress.of(raw)`
- [x] `UserRecord.email` / `JwtUserData.email` / `Caller.email` → `EmailAddress?`, parsed with
      `parseOrNull` at the claim boundary (attacker-supplied)
- [x] Request bodies STAY raw `String` (`AuthSignInRequest`, `AuthSignUpRequest`,
      `AuthRecoverAccountRequest`, `AddMemberRequest`) — normalized at the handler
- [x] Wire + storage format unchanged (inline value class over a String)

## Out of scope (deliberate)

- `funktor/messaging` `EmailDestination` / `Email` — the SENDING domain, a different concept from a
  user's identity address. Would be a reasonable follow-up.
- `AuditLog.userEmail` (`ultra/model`) — an audit snapshot string, not a lookup key.
- `Attendee.email` (funktorconf demo) — unrelated sample domain, not an auth user.

## ⚠️ ROLLOUT STEP — run BEFORE deploying to any environment with existing user data

`init` makes canonicality a DECODE-time invariant. A stored non-canonical address therefore fails to
deserialize: that account is locked out with an opaque 500, and any list query returning the row 500s
for everyone (e.g. `B2bMembersApi.loadMembers` takes out an org's entire member list). It is
fail-CLOSED — it denies, never grants — but it is a hard outage, so audit first:

```aql
// per user collection (b2b_users, b2b2c_users, admin_users, operator_users, …)
FOR u IN <users> FILTER u.email != LOWER(TRIM(u.email)) RETURN { _id: u._id, email: u.email }
```

Any hit must be canonicalized (or the row removed) before the deploy. The demo's seeds are all
lowercase, so the demo needs nothing — this is for real deployments. Format is NOT part of this: a
malformed-but-canonical address still decodes and still matches, deliberately.

## Test evidence

- [x] `EmailAddressSpec` — `init` rejects non-canonical (leading/trailing space, uppercase) and
      invalid; `of` normalizes; `parseOrNull` degrades; `localPart`/`domain`; slumber + kotlinx round
      trip; **`init` runs on decode** (a non-canonical stored value is rejected)
- [x] `EmailCanonicalizationSpec` — sign-in and password-reset-init canonicalize BEFORE the lookup.
      The reset-init test is **mutation-verified**: reintroducing a non-lowercasing lookup makes it red.
      The SSO gaps are covered by the existing `Google/GithubSsoAuthSpec` assertions, which now assert
      the typed CANONICAL value arriving at `loadByEmail` / `CreateUserForSignupParams`.
- [x] Full both-backend e2e green

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: to be assessed at review time (account-takeover-by-canonicalization surface:
unicode homoglyphs, dotted gmail aliases, `+tag` addressing).
