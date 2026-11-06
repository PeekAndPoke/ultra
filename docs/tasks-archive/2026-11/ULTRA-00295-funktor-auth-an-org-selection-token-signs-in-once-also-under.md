# ULTRA-00295: funktor-auth: an org-selection token signs in once, also under concurrent use

> **Status (2026-11-06): DONE.** gate PASS after round 1. The maintainer, 2026-11-06: "New ticket and tackle it".
>
> **Part of:** ULTRA-00248 (Epic: Auth and sessions)
**Security-critical:** yes (a single-use credential). Red team: none for a fix (maintainer, 2026-11-06); the scenario
goes into ULTRA-00095 (org isolation).

## Why this exists

Found while building ULTRA-00291. `AuthRealm.selectOrg` looks the token up, deletes it, and goes on without reading
what the delete removed. Two requests carrying the same token (a double submit, or a stolen token raced against its
owner) both find it, both delete, and both get a session. Every other one-shot token already checks the count
(`EmailAndPasswordAuth`: activation, reset, email change).

## Decide before implementing

Nothing: the fix is the pattern the other tokens use.

## The work

- [x] `selectOrg` goes on only when ITS delete removed the token (`RemoveResult.count == 1`); otherwise
      `AuthError.noOrganisationAccess()`, as for an unknown token.

## Acceptance

- [x] Unit: a store where the other request's delete wins (ours removes nothing) refuses before the user is loaded.
- [x] E2e, both backends: the picker's token completes the sign-in once; the same token a second time is refused.
- [x] Mutation-checked: ignoring the count goes red.

## Test evidence

- `AuthRealmActivationBaseSpec` (Karango and Monko): the other request's delete wins; ours removes nothing and is
  refused with `AuthError` before `loadById` (which this realm fails on).
- `ActivationHardeningE2eSpec` (both backends): the picker's token completes the sign-in; the same token again is a 403.
- funktor-auth 293/0, funktor-all 257/0, 2026-11-06. Mutants: Q1 the count ignored (unit red: `IllegalStateException`
  from `loadById`, the request went on), Q2 the token never consumed (e2e red: the second use).

## Review record — the LEDGER

Round 1 (level 1, `reviewer-high` ×3, 2026-11-06), one gate for ULTRA-00294, ULTRA-00295 and the cap-12 commit
a9c292b1 (ULTRA-00291 finding 1.8). All three charters CLEAN.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR (domain, security) | `CHANGELOG.md`, ULTRA-00294 "Decide" | Says a non-null date field fails; one WITH A DEFAULT takes its default (`DataClassAwaker.kt:87-108`) | fixed | Verified in the code; both texts now name null, default and failure |
| 1 | MINOR (domain) | `RealmTokenConfig.kt:51`, `CHANGELOG.md` | "12 keeps every mailed link" holds only while concurrent sign-ups cannot win one re-send slot twice | fixed (wording) | The race is ULTRA-00130, next in line; KDoc and CHANGELOG qualify the claim and name it |
| 1 | NIT (impl) | `DateAwakersMalformedInputSpec.kt:58` | `zoned` taken by position from `mapped` | fixed | An explicit list |
| 1 | NIT (impl, domain) | `DateAwakersMalformedInputSpec.kt:109` | The valid-input case cannot tell a zone from UTC; no offset, "Z" or "SYSTEM" case for the Mp awakers | fixed | 22:00Z: April 4 in UTC and "Z", April 5 in Bucharest, "+03:00", "UTC+03:00"; "SYSTEM" reads; mutant P7 (zone ignored) red |
| 1 | NIT (impl) | `ActivationHardeningE2eSpec.kt:594` | The second-use e2e is refused by the lookup, not the count | no change | True and intended: the count is pinned by the unit race test on both backends; the e2e pins sequential reuse |
| 1 | NIT (domain) | `AuthRealmActivationBaseSpec.kt:631` | "never reach the cap": they reach it, never exceed it | fixed | |

Probed and CLEAN (from the three reports): the exceptions each helper catches are the only ones its factory throws
for a String; `mpTimezoneOrNull` checks with the same kotlinx zones the calculation uses, offsets, "Z", "SYSTEM",
"GMT0", "UT" included, no valid id newly refused; extreme `ts` values; messages name the path, never the value;
`SlumberRestCodec`'s catch not widened; no new cost per request; no auth/saas/all record holds a nullable or defaulted
date of these types. `RemoveResult.count` is 1 only for the deleting request on Karango (`REMOVE … RETURN OLD`, a
missing document → count 0) and Mongo (`deletedCount`); the refusal is the same 403 as an unknown token. The cap
arithmetic (11 re-sends by 161h20, the next at 185h20) and the trim keeping exactly `keep`. SURPLUS: nothing.

**Verdict:** gate PASS after round 1. After the fixes: Slumber 1249/0, funktor-auth 293/0; mutant P7 red.

## History

- 2026-11-06: created on the maintainer's word.
- 2026-11-06: built, gate PASS after round 1, DONE. No docs ticket: no public API changed.
- 2026-11-06: DONE.
