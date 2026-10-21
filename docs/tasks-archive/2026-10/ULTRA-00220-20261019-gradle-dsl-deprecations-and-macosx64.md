# ULTRA-00220: Build: Gradle 9.6 Kotlin DSL deprecations, drop the macosX64 target

**Status:** DONE (archived 2026-10-19)
**Plan:** none — maintainer request, 2026-10-19: after the `~/.gradle` cache wipe the recompiled build scripts printed
deprecation warnings, "we should fix them too"
**Security-critical:** no

## Spec

- [x] `val X: String by project` (68 build scripts) → `val X = providers.gradleProperty("X").get()`, the idiom the
      root `build.gradle.kts:13` already used. Every value comes from the root `gradle.properties`; no module
      `gradle.properties` overrides a `*_GROUP` or `VERSION_NAME`, so the lookup is equivalent.
- [x] `buildSrc/src/main/kotlin/Docs.kt:19`, the same delegate inside `useEmptyJavadoc()` — it produced the 11 runtime
      "`by project` property delegate" deprecations that pointed at the CALLING build scripts.
- [x] `buildSrc/build.gradle.kts:15` `by extra` → `extra["kotlinVersion"] as String`.
- [x] Root `build.gradle.kts:65-69` yarn-lock setters → the `*Property.set(...)` forms.
- [x] `by tasks.registering(...)` → `tasks.register<T>(name)` (`ultra/codegen/build.gradle.kts`, 3 tasks); every use
      is a `TaskProvider` (`dependsOn(...)`), so the type is unchanged.
- [x] `tasks.create(...)` → `register<JavaExec>(...)` (`ultra/kontainer`, `mutator/core`); the only references are
      by name (`dependsOn("generateDocs")`), so lazy registration is enough.
- [x] **macosX64 dropped** (maintainer decision, 2026-10-19 — Kotlin deprecated the target): removed from
      `ultra/common`, `cache`, `datetime`, `maths`, `streams`; platform lists on the docs site and in
      `docs-site/src/data/llms/maths.md` updated. From 0.113.0 on there are no `*-macosx64` artifacts.
- [x] `./gradlew help --warning-mode all` shows no deprecation left except the "Native task … is disabled"
      notices (host limitation: macOS/Windows tests cannot run on Linux — nothing to fix).
- [x] Full `clean build` green.
- [x] CHANGELOG 0.113.0: macosX64 removal under breaking changes.

## Implementation notes

- After the first round, `help --warning-mode all` still showed 11 runtime delegate deprecations, the
  `registering`/`create` ones and macosX64; the sweep above is what the second run left.

## Test evidence

- [x] `./gradlew help --warning-mode all` after the delegate/registering/create fixes: only macosX64 (5) and the
      3 host notices remained (2026-10-19 12:2x).
- [x] After the macosX64 removal: `help --warning-mode all` leaves only the two host notices; cold `clean build`
      green, 68 targets / 15630 tests / 0 failures (2026-10-19 12:31).

## Review

Scoped by risk (review-loop): mechanical build-script changes with no library code touched, verified by the
warning-free configuration and the cold full build; no reviewer round. The macosX64 removal is a maintainer decision.
No docs follow-up: the platform lists were updated in this change.

## History

- 2026-10-21: numbered ULTRA-00220 (was `docs/tasks-archive/2026-10/20261019-gradle-dsl-deprecations-and-macosx64.md`).
