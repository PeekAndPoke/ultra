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

- [ ] On an idle tree (no other Gradle in this worktree): `:kraft:addons:avatars:jsBrowserTest` alone
- [ ] Then the full suite (`clean build`), nothing else building; fix whatever fails for real
- [ ] If the IrLinkageError reproduces without a concurrent build: find the race (Kotlin/JS IC caches, compile-sync
      outputs, parallel tasks) and fix it at the build level

## Implementation notes

## Test evidence
