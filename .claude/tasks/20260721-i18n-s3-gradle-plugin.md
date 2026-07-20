# S3 — i18n Gradle plugin (buildSrc)

**Status:** DONE — gate APPROVE after fixes, fixture green jvm+js (2026-07-21)
**Plan:** `.claude/tasks/20260720-i18n-l10n-foundation.md` → Build step S3
**Security-critical:** no

## Spec

The thin Gradle plugin wrapping the S2 emitter core, buildSrc-hosted for the monorepo (published
standalone later). Consumers: `apply<I18nPlugin>()` + `configure<I18nExtension> { ... }` (repo
precedent `ExtractExampleCodePlugin`; a `plugins { id(...) }` form comes with the published plugin).

- [ ] **buildSrc wiring**: buildSrc cannot depend on `:tooling` (compiles first), so the self-contained
      `tooling/src/main/kotlin/i18n` package is **source-included** into buildSrc (single source of
      truth, unit-tested in `tooling`, compiled into buildSrc for the plugin) + SnakeYAML dep
      (version kept in sync with `Deps.kt` — cross-referencing comments).
- [ ] **`I18nExtension`**: `packageName` (required), `moduleName` (default: project name PascalCased),
      `fallbackLang` (default `en`), `requiredLangs(...)`, `sourceSet` (default `commonMain`);
      yaml convention `src/<sourceSet>/i18n/messages.<locale>.yaml`.
- [ ] **`GenerateI18nTask`**: `@InputFiles` (NAME_ONLY, SkipWhenEmpty) + `@Input` config +
      `@OutputDirectory build/generated/i18n/<sourceSet>/kotlin`; clean-writes emitter output.
- [ ] **`CheckI18nTask`**: runs `I18nChecker`; ERROR always fails; WARNING fails under
      `-Pi18n.strict`; INFO logged; report file output; wired into `check`.
- [ ] **srcDir wiring**: `kotlin.srcDir(generateTaskProvider)` on the configured source set →
      Gradle auto-wires generate → every compile (metadata + platforms). No hardcoded task names.
- [ ] **Fixture module `:tooling:i18n-fixture`** (KMP jvm+js, permanent integration test): applies the
      plugin, ships `messages.{en,de,de-CH}.yaml`, tests call the generated accessors through a real
      `I18n` with the generated catalog — proves generate → srcDir → compile → runtime end-to-end.

## Test evidence

- [x] Fixture tests green on jvm + js from a clean build (new module, first run = clean)
- [x] Up-to-date: second `generateI18n` UP-TO-DATE; yaml edit re-triggers generate
- [x] `checkI18n` wired into `check`; **requiredLangs verified e2e**: removing a `de` key →
      `i18n [ERROR] de :: fixture.plain — missing translation (required language)` → BUILD FAILED;
      restored → green. (`-Pi18n.strict` wiring is 3 lines; severities unit-tested in tooling.)

## Notes / accepted caveats

- **M3 (documented, not fixed — by design):** `compileKotlin` depends on `generateI18n` but NOT
  `checkI18n` (generation reads only the fallback). So a non-fallback catalog with an ERROR (e.g. an
  introduced placeholder) compiles and can be `assemble`d. **CI/release must run `check` or `build`,
  not bare `assemble`/webpack.** Kept the split (Gradle convention); caveat recorded here.
- **L2 (accepted simplification):** the extension exposes `sourceSet` (name) but no `sourceDir`
  override; the design mentioned an overridable dir. All near-term consumers only vary the source set
  (commonMain/jsMain/main), so deferred.
- **buildSrc double-compile:** `tooling/src/main/kotlin/i18n` is source-included into buildSrc and
  compiled with the kotlin-dsl **embedded** compiler (older than `:tooling`'s). Fine today; a future
  2.x-only construct in `:tooling` would break the buildSrc compile — noted in `buildSrc/build.gradle.kts`.

## Review record (filled by /feature-review)

3 opus reviewers (impl & style, gradle/i18n domain, security). Gate: **APPROVE after fixes**.

| Reviewer | Verdict | Confirmed findings (fixed unless noted) |
|---|---|---|
| 1. Implementation & code style | APPROVE | avoids both `ExtractExampleCodePlugin` defects; MED test-gap (→ testable `checkOutcome`); LOW config-cache `fileTree` provider, LOW FQCN `RegularFileProperty`, buildSrc kotlin-version note (documented) |
| 2. Domain (gradle + i18n) | APPROVE | M1 `@SkipWhenEmpty` hid required-lang absence; M2 regional tags in `requiredLangs`; M3 compile≠check (documented); L1 yaml error filename; L3 pure-JVM source-set error message |
| 3. Security | APPROVE | LOW locale-tag collision (dedup guard); LOW `deleteRecursively` (sourceSet name validation); INFO log injection (control-char strip) |

Fixes applied:
- **M1** — `checkI18n` is no longer `@SkipWhenEmpty`; empty catalogs with `requiredLangs` now ERROR.
- **M2** — regional tags in `requiredLangs` are rejected with a clear message (targets base languages).
- **Test-gap** — the fail/pass decision is now the unit-tested `checkOutcome(findings, strict)` in
  `:tooling`; the plugin is trivial glue. Fixture also compiles+calls backticked (`is`/`greet-user`)
  accessors on jvm+js.
- **Security** — duplicate normalized locale tags fail the build; `sourceSet` validated as a simple
  name (guards `deleteRecursively`); control chars stripped from logged/reported findings.
- **LOWs** — config-cache-safe `Directory.asFileTree` provider; `RegularFileProperty` imported; yaml
  parse errors name the file; a missing source set gives a friendly "set sourceSet(\"main\")" hint.

Also (addressing user friction): **removed the kctfork compile-test** (`GeneratedCodeSpec` no longer
uses `ExperimentalCompilerApi`/kctfork; the `:tooling:i18n-fixture` module now provides stronger,
real-multiplatform compile coverage). `Deps` reverted the `compiletesting_core` const; the 0.13.0
version bump stays (used by the ksp modules).
