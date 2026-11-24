# ULTRA-00353: Kontainer: no stack trace per override, overrides copied at build, JMH harness in its own module

> **Status (2026-11-24): DESIGNED.** Three decisions of the maintainer on the ULTRA-00351 follow-ups.

## Why this exists

Open points left by ULTRA-00351 (kontainer creation and provision performance), decided by the maintainer on
2026-11-24.

## Decided (maintainer, 2026-11-24)

1. **No stack trace per override, option (c): "yes c".** Each override at `create`/`clone` builds a
   `ServiceDefinition` whose default `codeLocation = CodeLocation.create()` is a `Throwable()` (`ServiceDefinition.kt:21`,
   built in `ServiceProviderFactory`). It walks the whole call stack on every request (about 1 us, most of what an
   override still costs) and stores only `ServiceProviderFactory.<init>`, which tells nobody anything. Instead the
   override's location names the class of the override lambda (for example `ApiKontainerKt$createApi$1$1`), which
   points at the file and function where `with { }` was written, with no stack walk. Visible in the dump and debug info
   only (`KontainerTools.kt:44-45`).
2. **Overrides copied at build: "yes fix it please".** `DynamicOverrides.Builder.build()` wraps the builder's live
   map (`DynamicOverrides.kt:15`), and the kontainer keeps it for `clone`. A builder kept after `create` and used again
   would change what later clones get. Fix: `DynamicOverrides(overrides.toMap())`.
3. **The JMH harness moves to its own unpublished module: "move to own unpublished module".** Today it is
   `ultra/kontainer/src/jmh` with the `me.champeau.jmh` plugin in the published module's build. It uses public API only.
   Target: e.g. `ultra/kontainer-benchmarks` (unpublished, like `ultra/slumber-test-classes`); `ultra/kontainer`'s
   build goes back to having no JMH plugin. Run command updated in ULTRA-00351's notes and wherever documented.

## The work

- Tests first for 1 and 2 (the location of an override in debug info and dump; a builder used after `create` does not
  change a later clone), RED before the change, mutation-check.
- Measure 1 with the harness (createOneOverride, createTwoOverrides, cloneOneOverride, the request mix) before and after.
- 3 is build-only: the benchmark compiles and runs from the new module; no published module applies the JMH plugin.
- CHANGELOG: 1 and 2 under Changed (small, debug-visible) with the measured gain; 3 under Build.

## Acceptance

- No stack walk on the override path; the override's location names the lambda's class; both changes pinned by tests.
- `:ultra:kontainer` builds without the JMH plugin; the harness runs from its own module; all suites green.

## History

- 2026-11-24: created on the maintainer's word.
