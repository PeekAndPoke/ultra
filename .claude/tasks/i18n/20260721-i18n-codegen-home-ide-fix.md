# Fix: i18n codegen dual-source-root (IDE unresolved symbols)

**Status:** TODO (diagnosed 2026-07-21) — cosmetic, IDE-only; build is green
**Plan:** follow-up to S2/S3 (`.claude/tasks/i18n/20260720-i18n-l10n-foundation.md`)
**Security-critical:** no

## Problem

`buildSrc/build.gradle.kts:25` does `sourceSets.main.kotlin.srcDir("../tooling/src/main/kotlin/i18n")`
so the emitter sources (`I18nGenConfig`, `YamlCatalogParser`, `KotlinEmitter`, `I18nChecker`, …) are
source roots in BOTH `:tooling` and `buildSrc`. IntelliJ assigns each file to one module (buildSrc),
so `:tooling`'s own tests (`GeneratedCodeSpec`, same package) show unresolved symbols. **Gradle
compiles both copies fine — every build/test passes; the errors are IDE-only.**

## Fix options

- **(a) buildSrc home (recommended, least infra):** move `tooling/src/main/kotlin/i18n` + tests into
  `buildSrc/src/{main,test}/kotlin/i18n`; remove the srcDir inclusion; remove i18n + snakeyaml from
  `:tooling`. **Convert the tests from kotest to `kotlin.test`/JUnit5** — kotest 6 needs KSP for
  discovery, which is awkward in buildSrc; the assertions are mechanical (`shouldBe`→`assertEquals`,
  `shouldContain`→`assertTrue(contains)`, `shouldThrow`→`assertFailsWith`). Run via
  `./gradlew -p buildSrc test` (wire into CI).
- **(b) dedicated included build** (`tooling/i18n-codegen`, `java-gradle-plugin` + kotest as today):
  IDE-clean, kotest works natively, tests wire into root `check` via
  `gradle.includedBuild(...).task(":check")`; but can't use `Deps.kt` (version duplication) and
  consumers switch to `plugins { id("io.peekandpoke.i18n") }`. Closer to the eventual published plugin.

## Note

Single source of truth is the goal — pick one home so no file is dual-rooted. No runtime/generated
code changes; the plugin behavior and the fixture stay identical.
