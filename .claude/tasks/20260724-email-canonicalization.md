# Email canonicalization on lookup (framework auth gap)

**Status:** COLLECTED 2026-07-24 — needs a decision on approach before implementing (framework-touching).
**Found via:** the add-member-by-email question (2026-07-24). The leaf bug (`B2bMembersApi` add lookup)
is already FIXED (`c5398382`); this task is the BROADER framework gap.
**Type:** framework correctness (auth).

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
