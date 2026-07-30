# Move the i18n catalog model out of `:tooling` into `ultra/i18n`

**Status:** IN REVIEW — needs `/feature-review`
**Plan:** `.claude/tasks/20260730-frontend-sdk-vue-contributors.md` → "i18n in the SDK", prerequisite 2
**Security-critical:** no (build-time refactor; no behaviour change, no new input surface)

## Spec

`ultra/codegen` will emit TypeScript i18n accessors from the same key tree the Kotlin emitter uses. That
tree lived in `:tooling`, which is **not published**, so a published `ultra:codegen` could not depend on
it. Move the target-agnostic parts into `ultra/i18n` and leave the code-generation parts behind.

- [x] `LocaleCatalog`, `I18nNode`/`I18nNamespace`/`I18nMessage`, `I18nModelBuilder` move to
      `ultra/i18n/src/commonMain/kotlin/model/`
- [x] The three duplicated facts collapse to one declaration each: the `{{name}}` pattern, the plural
      suffix set, the locale-tag grammar
- [x] `YamlCatalogParser`, `KotlinEmitter`, `CodeWriter`, `KotlinNames`, `I18nChecker`, `I18nGenConfig`
      stay in `:tooling`
- [x] No change to generated output — `KraftFormsCatalog.kt` is byte-identical in shape, and the
      end-to-end fixture still passes
- [x] Dead config removed: `I18nGenConfig.requiredLocales` was declared and never read (the real
      `requiredLangs` path is the checker's, via the Gradle extension)

## Implementation notes

**The constraint that determined the design, found by the compiler on the first attempt.** buildSrc
source-includes these files (it cannot depend on a main-build project), and buildSrc is compiled by the
Kotlin embedded in **Gradle** — 8.10 → 1.9.24, with `kotlin-dsl` pinning the language version to **1.8**.
So a source-included file may use nothing from 1.9 or 2.x.

- `Locale` uses `@ConsistentCopyVisibility` (Kotlin 2.0), so it **cannot** be compiled into buildSrc.
  That is why `LocaleCatalog` keeps a `String` tag and why `I18nChecker` compares tags textually rather
  than asking a `Locale` for its region — and why `normalizeLocaleTag` existed in the first place. My
  first attempt "improved" `LocaleCatalog` to hold a `Locale` and failed to compile; reverted.
- Only the `model/` **directory** is source-included (`buildSrc/build.gradle.kts`), not the module. An
  earlier note in the plan doc claimed the whole commonMain could go in because it has zero imports and
  no `expect`/`actual` — true and irrelevant. The blocker is the language level.
- The contract (1.8 only; reference nothing outside the directory) is written at the top of
  `ultra/i18n/src/commonMain/kotlin/model/I18nCatalogModel.kt`. Both halves fail the buildSrc compile
  before any project builds, so it is enforced rather than merely documented.

**The duplication is still removed, differently.** `model/` owns `splitLocaleTag`; `Locale.parse`
(`ultra/i18n/src/commonMain/kotlin/Locale.kt:32`) and `normalizeLocaleTag` both go through it. One
grammar, two callers, no drift possible.

**`pluralSuffixes` cannot be derived from `PluralCategory`** — the enum is outside `model/` — so it is a
literal, pinned by `PluralSuffixParitySpec`. That is the honest shape: two declarations plus a test, not
one declaration plus a comment.

**`:tooling` gained `api(project(":ultra:i18n"))`** — `api`, because `LocaleCatalog` appears in
`KotlinEmitter.emit`'s and `I18nChecker.check`'s signatures.

## Test evidence

- [x] Unit: `PluralSuffixParitySpec` (4) — new; pins the suffix set against the enum, every category
      round-tripping through the key grammar, and `normalizeLocaleTag` == `Locale.parse(…).tag` over
      eight tag shapes
- [x] **Mutation-tested**: dropping `"zero"` from `pluralSuffixes` fails 2 of the 4 assertions
      (`PluralSuffixParitySpec.kt:19` and `:25`). The spec is not vacuous.
- [x] Existing suites unchanged and green: `:tooling:test` 22 tests (ForcedNamedParam 1,
      GeneratedCode 2, I18nChecker 9, I18nModelBuilder 2, KotlinEmitter 8), `:ultra:i18n:jvmTest` 29,
      `:tooling:i18n-fixture:jvmTest` 6 — 57 total, 0 failures, confirmed via `TEST-*.xml`
- [x] End-to-end: `:tooling:i18n-fixture:jvmTest` runs the plugin for real (yaml → generate → srcDir →
      compile → resolve), and `:kraft:core:generateI18n` + `:kraft:core:checkI18n` +
      `:tooling:i18n-fixture:checkI18n` pass `--rerun-tasks` against the real catalogs
- [x] Compile sweep: `compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs
      compileKotlin compileTestKotlin --continue` — one failure, `monko:core:compileTestKotlin`, which is
      **pre-existing and unrelated**: concurrent uncommitted work on `ultra/log` changed the `Log`
      interface (13 files, +342) and `MonkoDriverConfigSpec` has not caught up. `monko` has no i18n
      reference at all.
- [x] Full commands: `./gradlew :tooling:test :ultra:i18n:jvmTest :tooling:i18n-fixture:jvmTest` and
      `./gradlew :kraft:core:checkI18n :tooling:i18n-fixture:checkI18n :kraft:core:generateI18n
      --rerun-tasks` — both BUILD SUCCESSFUL
- [ ] `:ultra:i18n:jsTest` / native targets not run — the moved code is pure Kotlin with no platform
      surface, but the sweep did not cover native

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

## The 1.8 island is gone — buildSrc off `kotlin-dsl`, Gradle 9.5.0 (2026-07-30)

Done at the maintainer's request, immediately after the move, in the order they specified: drop
`kotlin-dsl` first, then bump Gradle.

**`kotlin-dsl` was the cause, not the Gradle version.** It applies the Kotlin plugin at Gradle's
*embedded* version and pins the language version to 1.8. Replaced with plain `kotlin("jvm")` at the
project's own 2.4.10:

- `buildSrc/settings.gradle.kts` (new) resolves the version from the root `gradle.properties` — buildSrc
  does not inherit it — declares it in `pluginManagement { plugins { … } }` so `build.gradle.kts` needs no
  version, and passes it to the project via `extra` so the file is read exactly once. Two gotchas, both
  found by the compiler: `pluginManagement` is compiled in isolation so file-level imports do not reach
  inside it (hence the fully-qualified `java.util.Properties`), and `getProperty` returns a platform type
  which makes `version` ambiguous (hence the explicit `: String`).
- **`kotlin-dsl` also configures the `sam-with-receiver` compiler plugin**, which is what turns Gradle's
  `@HasImplicitReceiver`-annotated `Action<T>` parameters into implicit receivers. Without it every
  `tasks.register<T>("x") { dependsOn(y) }` in buildSrc loses its receiver — 12 unresolved references
  across all four files. Re-applied explicitly as `kotlin("plugin.sam.with.receiver")` +
  `samWithReceiver { annotation("org.gradle.api.HasImplicitReceiver") }`. This is the non-obvious part of
  the swap and the reason it is worth writing down.
- `gradleApi()` was already declared; `gradleKotlinDsl()` had to be added. `java-gradle-plugin` and
  precompiled-script support are NOT needed — every consumer applies by class (`apply<I18nPlugin>()`),
  never by plugin id, and there are zero `*.gradle.kts` files in `buildSrc/src`.
- `jvmToolchain(17)` set explicitly: buildSrc output is loaded by the JVM running Gradle, and `Deps` is
  this project's own output so it cannot supply the constant.

**Gradle 8.10 → 9.5.0.** KGP 2.4.10 requires ≥ 7.6.3 and fully supports up to **9.5.0**; latest Gradle is
9.6.1, so the newest release is deliberately NOT the target. Clean bump, no code changes needed.

**Proof the constraint is lifted:** pointing the srcDir at the whole `commonMain` — which contains
`Locale.kt` and its Kotlin 2.0 `@ConsistentCopyVisibility` — now compiles. That exact configuration failed
before. The srcDir was then reverted to `model/` on purpose: a minimal include is still the right choice,
it is just no longer a hard limit.

**Also worth recording:** `gradle --version` on 9.5.0 reports embedded Kotlin **2.3.20**. So bumping Gradle
alone would only have moved the island from 1.8 to whatever `kotlin-dsl` pins under 2.3.20 — it would not
have removed it. Dropping `kotlin-dsl` was the load-bearing change.

Verification on 9.5.0: the 57 i18n tests still green; `:ultra:codegen:check` green including the
pnpm/node `tsVerify` gate (10 fixtures); `:ultra:kontainer:test` green (186 tests, and it uses a
Gradle-deprecated `TaskContainer.create` overload, so it exercises the deprecation surface); compile sweep
identical to before the bump — same single pre-existing `monko:core` failure, nothing new. Remaining
warnings are all pre-existing deprecations (`TaskContainer.create` with an `Action`, `macosX64` native
targets, `readLine`) plus one in `ExtractExampleCodePlugin.kt:46` that only became visible because buildSrc
now compiles against a newer API.

## Follow-ups

- [ ] **`LocaleCatalog` can now hold a real `Locale`**, and `I18nChecker` can ask it for `region`/`base`
      instead of doing `.contains('-')` / `.substringBefore('-')` on a tag. Unblocked by the above but
      deliberately not done in the same pass — it touches the checker's semantics and wants its own
      verification. `normalizeLocaleTag` would then collapse into `Locale.parse(tag).tag`.
- [ ] Clear the pre-existing deprecation warnings (`TaskContainer.create` with an `Action` in
      `mutator/core`, `ultra/kontainer`, `ExtractExampleCodePlugin`) — Gradle will remove those overloads.
      Unrelated to i18n; its own small chore.
- [ ] **No DOCS task.** This adds public API to a published module (`ultra.i18n.model`), but it is
      generator-facing plumbing with no user-facing story yet. Document it with the TS i18n contributor
      that consumes it, not before. The Gradle/buildSrc change is internal and needs no docs.
