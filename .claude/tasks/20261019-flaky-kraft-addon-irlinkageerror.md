# Kraft addon JS tests fail with IrLinkageError (TestBed.Companion)

**Status:** IN PROGRESS (Ultra-Main-Dev, 2026-10-19)
**Plan:** none — maintainer: fix the flaky tests first, then the full suite green, then prepare 0.113.0
**Security-critical:** no

## Symptom

In the release `clean build` of 2026-10-19 (and in the maintainer's own full-suite run at the same time), several Kraft
addon `jsBrowserTest` suites failed — avatars 3/4, browserdetect 2/3, jwtdecode 2/3, marked 2/3, prismjs 2/3,
signaturepad 2/3, threejs 2/4 — all with:

```
IrLinkageError: Can not get instance of singleton 'Companion': No class found for symbol
'io.peekandpoke.kraft.testing/TestBed.Companion|null[0]'
```

`kraft/testing/src/jsMain/kotlin/TestBed.kt:30-33` DOES declare `companion object`, and every addon depends on
`project(":kraft:testing")`. So the test executable linked a `kraft:testing` klib that lacked (or only partly had)
`TestBed` — not what the source says.

## Leads

- **Concurrent builds** (most likely for this run): the maintainer's full suite and the release `clean build` ran in
  the same worktree at the same time. The release log shows the classic interleaving damage: files that must exist were
  missing (`ultra/i18n/build/kotlinProjectStructureMetadata/kotlin-project-structure-metadata.json`,
  `tooling/build/libs/tooling-0.112.0.jar` mid-transform), "unresolved references" to code that exists
  (`getNumberOrNull` in `monko/core` tests), test workers dying with `EOFException`. A klib rewritten by one build
  while the other links against it explains "No class found".
- The same 7 suites were green on the final tree in an isolated run earlier the same day (`fed9393d`), and avatars and
  chartjs have practically identical build configs while only avatars failed — nondeterministic.
- Still to rule out: a real intra-build race (Gradle parallel execution) — reproduce on an idle tree.

## Plan

- [x] On an idle tree: `:kraft:addons:avatars:jsBrowserTest` alone — still RED, but only because its cached bundle
      was linked during the overlap (see notes)
- [x] Then the full suite (`clean build`), nothing else building — STILL RED (8 addons, now chartjs too): the damage
      was in the shared **build cache**, so a clean build restored it (see notes)
- [x] The 8 addons after a module `clean` + `--no-build-cache`: 59 tests, 0 failures; both bundles now contain
      `ultra-kraft-testing`
- [x] Damaged cache entries removed — the maintainer wiped `~/.gradle/caches` (2026-10-19 ~12:00)
- [ ] Full `clean build` green on the empty cache (runs as the 0.113.0 verification build)

## Implementation notes

**Root cause found (2026-10-19 11:45):** reproduced on an IDLE tree in 7 s with every task UP-TO-DATE. The avatars test
executable (`build/js/packages/ultra-kraft-addons-avatars-test/kotlin/`, linked 11:33:27 during the overlapping runs)
contains 34 modules but NO `ultra-kraft-testing.mjs`. The `kraft:testing` klib itself is complete (6 `.knm`, `TestBed`
in its strings). So the link ran while the other build had wiped or was rewriting `kraft:testing`'s outputs, and
Kotlin/JS **partial linkage** turned the unresolved library into runtime `IrLinkageError`s instead of failing the link.
Gradle then recorded the broken bundle as a successful, up-to-date output — the CLAUDE.md "stale outputs after a
concurrent build" trap, in its JS form. Not flakiness in the tests themselves.

**Consequence to check:** the maintainer's 0.112.0 publish was built in the same overlap. The staged publications
(466 jar/klib, `*/build/publishing/mavenCentral`, staged 11:34:28–39) were copied to the session scratchpad before
any clean, to compare against a fresh idle build.

**Correction (2026-10-19 11:52): it was the shared BUILD CACHE, not only the build dir.** The idle `clean build` failed
the same way, plus chartjs (19/20), in 7 m 40 s with 345 tasks FROM-CACHE — including
`:kraft:addons:avatars:compileTestDevelopmentExecutableKotlinJs`. The local cache (`~/.gradle/caches/build-cache-1`)
held 253 entries written 11:00–11:40, i.e. during the overlap. The avatars and chartjs test links were among them,
stored 11:24:11. The race: build A fingerprints a task's inputs (correct key), build B's `clean` deletes files A is
reading or about to pack, A stores the damaged output under the correct key — and from then on every build with those
inputs restores it, `clean` or not. The window also held 2 `compileKotlinJs`, 15 `compileCommonMainKotlinMetadata`
and ~9 native compiles (kinds that end up in published artifacts), 7 production webpack bundles and 78 artifact
transforms. Removing entries from the shared cache was (rightly) not mine to do; the maintainer wiped it.

**Rule this adds:** after any concurrent build in one worktree, `clean` is NOT enough — the cache entries from the
overlap window must go too (or build with `--no-build-cache`).

Possible hardening (proposal, not done): `-Xpartial-linkage-loglevel=ERROR` for the JS test/executable links, so a
missing library fails the link (and is never cached as a success) instead of surfacing as a runtime `IrLinkageError`.

## Test evidence
