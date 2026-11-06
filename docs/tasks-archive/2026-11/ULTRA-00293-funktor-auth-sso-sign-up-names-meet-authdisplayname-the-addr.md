# ULTRA-00293: funktor auth: SSO sign-up names meet AuthDisplayName, the address's local part otherwise

> **Status (2026-11-06): DONE.** gate PASS after round 1.** Approved by the maintainer 2026-11-06.
**Security-critical:** yes (a name the user did not choose and the rule would refuse)

## Why this exists

Found in the ULTRA-00289 gate (finding 2.5), older than that change. An SSO sign-up takes the provider's profile
name, only trimmed: it skips `AuthDisplayName`, so control, invisible and bidi characters pass. A GitHub profile
with `"name": null` becomes the literal display name `"null"` (`JsonNull.content`).

## Decisions (maintainer, 2026-11-06)

"Can we in this case use the email address before the @ as the default profile name?" Yes: a missing or refused
provider name falls back to the address's local part.

## The work

- `AuthUserAdapter.CreateUserForSignupParams.of`: the given name if `AuthDisplayName` accepts it, else the local
  part (if the rule refuses even that, its first `MAX_LENGTH` chars). One place, so every provider gets it.
- `GithubSsoAuth`: read `name` with `contentOrNull`, so a JSON null is no name.

## Implementation notes

- The rule sits in `CreateUserForSignupParams.of` (`AuthUserAdapter.kt`), the one constructor every provider uses,
  so a new provider cannot skip it. Email-and-password sign-up passes no name and gets the local part as before.
- `GithubSsoAuth.signUp` reads `name` as `(… as? JsonPrimitive)?.contentOrNull`.
- The funktor-all fake GitHub now answers `"name": null`, as GitHub does for a profile without a name.

## Test evidence

- `GithubSsoAuthSpec`, `GoogleSsoAuthSpec`: null (GitHub), missing, blank and refused names give the local part; a
  good name is kept, trimmed. `ActivationHardeningE2eSpec` (Karango and Monko): a new GitHub account without a
  profile name is named after the local part.
- Green: auth JVM 284/0, funktor-all 251/0.
- Mutation-checked (2026-11-06): the name unchecked (both provider specs); GitHub's null read as content (GitHub
  spec, the e2e on both backends); the long local part not cut (Google spec).

## Acceptance

- [x] GitHub `"name": null`, a missing name, a blank one and a refused one (a bidi override) each give the local part.
- [x] Google: the same for its `name` claim.
- [x] A good provider name is kept, trimmed.
- [x] Mutation-checked.

## Review record (the ledger)

### Round 1: level 1 (`reviewer-high`, all three charters): CLEAN (0 CRITICAL, 0 MAJOR)

| # | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1.1 | MINOR | `AuthUserAdapter.kt:54` | The third fallback (the local part cut to `MAX_LENGTH`) is reachable, as the address format allows a 150-char local part, but untested | fixed | Verified (`EmailRegex` has no 64-char cap). Google spec case, mutant W1 |
| 1.2 | NIT | `AuthUserAdapter.kt:54` | The cut local part may end in an (escaped, quoted) space | fixed | `.trim()` after the cut |
| 1.3 | NIT | `CHANGELOG.md` | Email-and-password sign-up also cuts a local part over 100 chars, unlisted | fixed | CHANGELOG line |

Noted, not a defect: GitHub's `login` would be a better fallback than the local part; the maintainer chose the local
part. Out of scope, older: GitHub sign-up keys on the profile's public `email`, so a user with a private address is
refused although `/user/emails` holds the verified primary.

**Probed and CLEAN:** `of` is the only constructor (`@ConsistentCopyVisibility`); the three callers; GitHub's `name`
as null, missing, object, array or number; Google's claim missing or not a string; no other path (sign-in, profile
sync, `resolveSsoAccount`, invitations) writes a provider name; funktor-demo realms; retirement; the tests' literal
expectations; KDoc; code style; every stored name now passes the rule.

**Verdict:** gate PASS.

## History

- 2026-11-06: created on the maintainer's approval.
- 2026-11-06: built, round 1 CLEAN, its findings fixed; DONE. No docs follow-up: ULTRA-00290 (docs) already notes it.
- 2026-11-06: DONE.
