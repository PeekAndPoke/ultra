---
name: testing
description: Use before saying "tests pass" in this repo, and when running Gradle tests, mutation checks or a compile sweep. Holds the verification traps that make a suite look green while the feature is broken.
---

# Testing in ultra

The rules (e2e for backend code, mutation checks for security and persistence, serialize what production
serializes) are in `CLAUDE.md`. This skill holds the traps. Moved here from `CLAUDE.md` at the Rules Retro
(2026-10-23), unchanged.

## Verification traps

These make a suite look green while the feature is broken. Treat them as preconditions for saying
"tests pass", not as extras.

- **kotest ignores `--tests` — sometimes**, e.g. for test names written in quotes (maintainer, 2026-10-23:
  watch this rule and pin the condition when it next shows). Confirm a spec actually ran via
  `build/test-results/**/TEST-*.xml`, never by trusting a filtered gradle invocation. Avoid `--rerun-tasks`
  (kapt flakiness). MPP modules use `:jvmTest`; `funktor-demo:server` uses `:test`.
- **…but that XML mis-attributes WHICH test failed.** Counts (`tests`/`failures`/`errors`) are
  reliable; the `<testcase name>` a `<failure>` is nested under is not — it can name a test that
  cannot produce that failure, and it disagrees with the gradle console for the same run. Use the XML
  to confirm a spec ran and how many failed; use the console output to see which case broke.
  Confirmed twice on 2026-07-28 (`HelpersSpec`, `MonkoSlashKeyTest`).
- **Mutation-test every change that is security-relevant OR touches persistence** — storage,
  serialization, codecs, migrations, indexes, query building (maintainer, 2026-08-28). Deliberately
  narrower than the klang standard the review-loop skill was adopted from, which mutation-checks every
  test. Protocol in `.claude/skills/review-loop/`. A green test proves nothing until it has been RED for
  the right reason. **Persistence earns the same bar as security because it fails the same way: silently,
  and the damage is already on disk by the time anyone looks.** This repeatedly catches vacuous or right-for-the-wrong-reason
  tests, including ones written in the same session — a "single-use token" e2e also satisfied by a
  cooldown, and a `validate()` test that constructed the healthy object so it passed whether or not
  validation ran.
- **If a type is serialized in production, a test must serialize it.** Not a mutation rule, a coverage
  one, and it earns its own line: `VaultCollector.Data` held a type Slumber had no codec for, so the
  slice threw in a post-response coroutine and silently dropped the WHOLE record — `vault.entries` was
  `[]` across all 1193 depot records. The collector had tests and the API had tests; the one operation
  the data actually undergoes in production had none.
- **A surviving mutant is not always a missing test** — it can mean the FIX was wrong, the fixture
  cannot express the difference, or a comment claimed something false. Work out which before adding an
  assertion to make it die.
- **Back up with `cp` before mutating a file; never restore with `git checkout`** — the file usually
  carries other uncommitted work. **Keep the source-set path in the backup name.** Backing several
  files into one directory by `basename` silently collides in a multiplatform layout — `commonMain/
  strings.kt` and `jvmMain/strings.kt` flatten onto the same name, and the restore puts one source
  set's content into the other. Mirror the path (`cp x/commonMain/y.kt bak/commonMain__y.kt`) or back
  up one file at a time.
- **Module test tasks do not compile everything.** The root project has its own `src/jvmMain` that is
  not a module in `settings.gradle`, so `:some:module:allTests` never touches it — a change to
  `ultra/common` broke it on 2026-07-29 while every module suite stayed green. Before claiming a
  cross-module change is contained, run a compile sweep:
  `./gradlew compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs compileKotlin
  compileTestKotlin --continue` and check for `^e:`. Native targets need their own
  `compileKotlinLinuxX64` and are not covered either.
- **A green compile sweep can be measured against STALE test classes.** After an ABI change to a
  published interface in `ultra/log`, Gradle held `:ultra:vault:compileTestKotlinJvm` UP-TO-DATE, so
  the sweep reported zero errors while the compiled test double still implemented the *old*
  signature. It surfaced only at runtime, as `AbstractMethodError` wrapped inside an
  `AssertionFailedError` — i.e. it looked like a logic bug, not a build problem. `touch` does not
  help (Gradle hashes content, not mtime). **Most likely cause: a concurrent build.** Another agent
  was building the same worktree at the time, and Kotlin's incremental state under `build/kotlin/`
  is not safe against two interleaved builds — so a task's snapshot can claim up-to-date while its
  outputs are not. This was not proven, only inferred; the observation itself is solid.
  Recovery: `rm -rf <module>/build/classes/kotlin/**/test` for every dependent module and re-run.
  Only then is "compiles clean" evidence. Observed 2026-07-30.
- **A grep for call sites misses receiver-less calls.** Searching `.observe(` will not find
  `observe(x) { }`, where the receiver is implicit — that is how the same break was missed twice.
  When removing an extension, prefer renaming it and compiling: the compiler finds every caller,
  a grep finds the ones you thought of.
- **…and a grep for implementors misses anonymous objects.** Sizing the `Log` interface change by
  grepping `: Log` found the named classes but not three `object : Log { }` literals inside a monko
  spec. They surfaced only after the stale test classes above were deleted and the module actually
  recompiled. Same rule applies: let the compiler enumerate implementors, and treat a grep-derived
  blast radius as a lower bound. Confirmed 2026-07-30.
- **`shouldBe` is untyped**, so `valueClass shouldBe "literal"` rots silently.
- **Local DBs run in docker:** `docker start mongodb arangodb`. They match `application.test.conf`;
  Arango must already have `funktor-demo-test` (Karango creates collections and indexes, never a
  database — a missing one surfaces as an opaque Arango 1228).
