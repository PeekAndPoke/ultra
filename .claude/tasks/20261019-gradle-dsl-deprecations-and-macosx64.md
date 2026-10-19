# Build: Gradle 9.6 Kotlin DSL deprecations, drop the macosX64 target

**Status:** IN PROGRESS (Ultra-Main-Dev, 2026-10-19)
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
- [ ] `./gradlew help --warning-mode all` shows no deprecation left except the three "Native task … is disabled"
      notices (host limitation: macOS/Windows tests cannot run on Linux — nothing to fix).
- [ ] Full `clean build` green.
- [ ] CHANGELOG 0.113.0: macosX64 removal under breaking changes.

## Implementation notes

- After the first round, `help --warning-mode all` still showed 11 runtime delegate deprecations, the
  `registering`/`create` ones and macosX64; the sweep above is what the second run left.

## Test evidence

- [x] `./gradlew help --warning-mode all` after the delegate/registering/create fixes: only macosX64 (5) and the
      3 host notices remained (2026-10-19 12:2x).
- [ ] After the macosX64 removal: config run + full `clean build`.
