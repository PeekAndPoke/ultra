# Red-team: `EmailAddress` — email as an authentication identifier

**Status:** COLLECTED 2026-07-26 — NOT executed. A dedicated penetration-test session runs these.
**Feature:** `.claude/tasks/20260726-value-class-emailaddress.md`
**Why:** email is how users are resolved at sign-in, password reset, and SSO login/signup.
Canonicalization is the fix, but canonicalization also COLLAPSES distinct inputs onto one key — which
is how account-takeover bugs are born.

> The security review found no auth bypass introduced by the change. §1 and §2 below are PRE-EXISTING
> and are the highest-value items here; they are tracked as work in
> `.claude/tasks/20260726-sso-email-verification.md`.

## 1. Unverified-provider-email takeover (HIGHEST VALUE)

`GoogleSsoAuth`/`GithubSsoAuth` signup resolve an existing local account by email and never read
`email_verified`.

- Stand up a Google Workspace on a controlled domain (or a Google account with an unverified alternate
  address) emitting `email_verified=false` for a seeded victim address. Attempt sign-up, then sign-in,
  against the `admin` realm (which enables `Capability.SignUp` on all three providers).
- Repeat for GitHub by manipulating the `/user` `email` field through a proxy, confirming the code has
  no independent verification.
- Expected today: full session minted as the victim, no password needed.

## 2. Pre-hijack chain (composes with §1)

Email+password signup returns `requiresActivation = true`, but `AuthRealm.signUp` still calls
`issueSignIn` unconditionally (the activation email is a `TODO`).

- Register `victim@corp.com` with an attacker password → confirm an immediate live session.
- Have the "victim" complete Google SSO signup → confirm they are handed the ATTACKER's account.
- Confirm the attacker's original password still authenticates it afterwards.

## 3. Unicode collapse — narrower than expected, verify it stays that way

Over all code points ≥ U+0080, exactly ONE lowercases entirely into the address alphabet the
ASCII-only `EmailRegex` accepts: **U+212A KELVIN SIGN → `k`**. `of()` now rejects non-ASCII raw input
BEFORE lowercasing, which should make the collapse unconstructable.

- Attempt signup / reset-init / SSO with `Karsten@…`; confirm rejection, not collision.
- Re-run the enumeration if `EmailRegex` is ever relaxed to accept unicode (see §7) — the guard and the
  regex are coupled.
- Grep for any raw-`String` email comparison that would give a collapse teeth.

## 4. Quoted-local-part parser abuse

The regex admits a quoted local part, which may contain `@`: `"a@corp.com"@evil.com` is a VALID
address. `localPart`/`domain` now split on the LAST `@` (so `.domain` is `evil.com`), and both are
KDoc'd display-only.

- Register such an address; confirm accepted.
- Assert no trust decision consults `.domain` / `.localPart` (a domain allowlist built on `.domain`
  would be the bug).
- Check the auto-derived display name that lands in outbound HTML mail.

## 5. Enumeration by timing

Response bodies are identical for unknown / known / unparseable on reset-init, and identical for
invalid-format / unknown / wrong-password on sign-in. The WORK is not:

- reset-init: unparseable does nothing; unknown does one read; known does a read + token + DB write +
  an awaited `mailing.send()` (a real SMTP round trip on the request path).
- sign-in: unknown short-circuits before any password hashing; known runs a deliberately slow hash.

Statistically distinguish known from unknown across ~1k samples each, and measure whether
`letTheBotsWait()`'s 250–500 ms jitter meaningfully raises the required sample count.

## 6. Decode-failure blast radius (fail-closed, but an outage)

`init` enforces canonicality on decode. Format is deliberately NOT enforced there — nor in `of` /
`parseOrNull`, so a legacy row stays both READABLE and MATCHABLE (that second half was a review
finding: enforcing format on the lookup path left such accounts listed but unable to sign in or reset).
A non-CANONICAL stored row still fails to decode, which is what this probe is about.

- Plant a non-canonical email directly in a user collection.
- Confirm: that account is locked out with an opaque 500 (not a clean error), and
  `B2bMembersApi.loadMembers` 500s the entire org's member list for every member.
- Outside production the 500 body carries a stack trace; confirm production config does not.
- Pre-flight audit query for any real deployment:
  `FOR u IN <users> FILTER u.email != LOWER(TRIM(u.email)) RETURN u._id`

## 7. Gmail dot-aliases and `+tag` — an explicit decision, not a default

`a.b@gmail.com` and `ab@gmail.com` are the SAME mailbox at Google but DISTINCT accounts here, and
`user+tag@` is likewise distinct. That is deliberate: normalizing them would create a far larger
collapse surface than the current design.

Confirm this is a conscious product decision and write it down. If it should change, note that it
makes §3 materially more dangerous.
