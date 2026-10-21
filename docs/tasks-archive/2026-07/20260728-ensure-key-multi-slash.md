# ensureKey: split at the first slash, not the second

**Status:** DONE (2026-07-28)
**Plan:** none — found while writing KDoc for `ultra/vault/src/jvmMain/kotlin/helpers.kt`
**Security-critical:** no

## Outcome

One line changed, plus its KDoc:

```kotlin
val String.ensureKey get() = substringAfter('/')   // was: if (contains('/')) split('/')[1] else this
```

Identical behaviour for every well-formed id. It differs only for malformed input — `"a/b/c"` now
yields `"b/c"` instead of `"b"` — so a bad id degrades visibly instead of silently resolving to a
middle segment.

## The invariant this rests on

**A document key must not contain a slash.** The `collection/key` id format only parses
unambiguously without them, and ArangoDB enforces it server-side. Every key in this codebase is a
slug (`elena-brandt`, `funktor-summit`, `ultra-conf-2026`); auto-generated keys are ObjectId hex or
Arango keys, neither of which can contain `/`.

**This is deliberately not validated.** Arango rejects a slash key with a server error; Mongo accepts
it and the key is then corrupted on read-back, because `_key` is re-derived from `_id` via
`ensureKey` (`slumber/StoredAwaker.kt:32`, `domain.kt:125`, `domain.kt:263`). Decided on 2026-07-28
to leave it undefined rather than add validation — no code path produces such a key, so the check
would guard a case that cannot occur.

If that ever changes, note that the fix is NOT a smarter `String.ensureKey`. Given `"tenant/a/user1"`
a context-free extension cannot tell `collection/key` from a key containing slashes; it is
undecidable without the repository name. It would have to be repository-aware
(`removePrefix("$name/")`, the mirror of `Repository.ensureId`) at every "id or key" call site —
`MonkoRepository.findById`, `MonkoDriver.deleteOne`, and the Karango equivalents.

## What was tried and reverted

A first pass supported slash-containing keys: a repo-aware `Repository.ensureKey`, rewired
`MonkoRepository.findById` and `MonkoDriver.deleteOne`, `ensureId` switched to
`startsWith("$name/")`, and a MongoDB round-trip e2e test. It worked and was green, but the premise
was wrong — the maintainer confirmed slash keys should never exist. Supporting them on Mongo would
also have split the two backends: app code valid on Mongo would fail on Arango, which defeats the
point of a portable abstraction. All of it was reverted; `Repository.kt`, `MonkoRepository.kt` and
`MonkoDriver.kt` show no diff.

**Process note:** the example that drove that design (`tenant/a/user1`) was invented by the agent and
never checked against real data. Ninety percent of the work was spent supporting a case that does not
exist. Verify that a case is reachable before designing for it.

## Test evidence

- [x] `ultra/vault/src/jvmTest/kotlin/HelpersSpec.kt` — 5 tests, including two added here:
      `splits at the first slash, not the second`, and `an id without a key is empty`
- [x] Mutation-checked the guarantee the change actually depends on: `substringAfter('/')` returns
      the receiver unchanged when there is no delimiter. Forcing `substringAfter('/', "")` fails
      exactly one test — without it, every bare key would silently become `""`. Restored from a `cp`
      backup; suite re-run green.
- [x] Suites green, counts read from `build/test-results/**/TEST-*.xml`: `:ultra:vault:jvmTest` 249,
      `:monko:core:test` 249, `:karango:core:test` 1649, `:funktor-demo:server:test` 47 —
      0 failures, 0 errors. Karango's 1649 staying green is the proof nothing changed for Arango ids,
      where exactly one slash makes both implementations identical.

## Trap found while verifying

**kotest's JUnit XML mis-attributes which test failed.** The mutation above can only break the
no-slash case, but the XML named the failure `ensureKey extracts key from id with slash`. The same
mismatch appeared in a `funktor-demo:server` run, where a test containing no null assertion was
reported as `Expected value to not be null`. Failure *counts* in these files are reliable; per-test
*names* are not, so they cannot be used to work out which case broke. This matters because the
project's verification rules lean on these XML files — see `CLAUDE.md` → Verification traps.

## Review record

Not run — the surviving change is a one-line expression simplification with no behaviour change for
valid input, covered by an existing test and a mutation check. The version that would have needed a
review gate was reverted.
