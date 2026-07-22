# S2 — i18n emitter core (`tooling/`)

**Status:** DONE — gate PASS after fixes, all tests green (2026-07-21) — effort `xhigh`
**Plan:** `.claude/tasks/20260720-i18n-l10n-foundation.md` → Build step S2
**Security-critical:** no

## Spec

A pure JVM library in `tooling/` that turns i18n YAML catalogs into Kotlin source (no Gradle wiring —
that is S3). Two independently testable halves:

**Input pipeline:**
- [x] `YamlCatalogParser` — SnakeYAML (SafeConstructor) → flat dotted-key → template map; locale tag
      normalized to match `ultra:i18n` `Locale.tag` (lang lower, region UPPER).
- [x] Model (`I18nNamespace`/`I18nMessage`) + `I18nModelBuilder` — from the **fallback** catalog only
      (D8), builds the namespace tree: collapses `_one`/`_other` plural siblings into one plural
      message, detects `{{placeholder}}` names (minus the implicit `count` for plurals), supports nesting.

**Output pipeline:**
- [x] `KotlinEmitter` — emits (D5/D6 split): a baked `<Module>Catalog : I18nCatalog` object (all
      locales, raw plural keys), and accessors — nested receiver classes, `I18nTranslate` root extension
      properties, and non-inline top-level extension functions with **forced named params** (leading
      `vararg _: Unit` — `Nothing` is prohibited as a vararg) and `count: Int` for plurals.
- [x] Golden-file (`shouldContain`) tests + a real compile-test.

**Checker:**
- [x] `I18nChecker` — per non-fallback locale: missing/superfluous keys, placeholder rules
      (introduced=error, omitted=info), base-vs-regional handling, identical-to-base redundancy (D8).
      Plural placeholders compared against the **union** of the fallback's forms. `requiredLangs`
      (`I18nGenConfig.requiredLocales`): an absent required locale or a missing key → ERROR.
- [x] `KotlinNames` — identifier safety: whitelist `[A-Za-z0-9_-]`, backtick keywords/hyphen/leading-digit,
      reject anything else (injection guard). `escape` applied to keys, values, locale tags, accessor
      bodies. Namespace class-name and namespace/message collision guards.

## Review record (filled by /feature-review)

3 opus reviewers (impl & style, i18n domain, codegen security). Gate: **NOT PASS → fixed → PASS**.

| Reviewer | Verdict | Confirmed findings (all fixed) |
|---|---|---|
| 1. Implementation & code style | fixed→PASS | HIGH invalid/keyword identifiers break compile; MED no compile-test; LOW plural-suffix/name collisions, unescaped accessor-body literals |
| 2. Domain (i18n codegen) | fixed→PASS | HIGH plural placeholder check per-form not per-union (false CI errors); MED base-vs-regional key severity contradiction; LOW class-name collision, i18next regex gaps |
| 3. Security (codegen) | NOT PASS→fixed | **CRITICAL** identifier + raw-key injection into accessors file; MED unescaped locale tag; LOW hyphen placeholder |

Fixes applied:
- **Identifier safety** (`KotlinNames`) — whitelist + backtick + reject; closes the injection and the
  keyword/hyphen/leading-digit compile breaks. Proven by `GeneratedCodeSpec` (compiles emitted code
  incl. `is`, `my-key`, `{{first-name}}`; rejects a crafted `x(): Unit; fun y` key).
- **Escape everywhere** — `msg.key`, placeholder map-keys, and locale tags now go through `escape()`.
- **Plural placeholder union** in the checker (D8) — no more false-positive ERRORs on valid plurals.
- **Severity adjudication** — a dead/superfluous KEY = WARNING (base & regional); an introduced
  PLACEHOLDER = ERROR (both). Recorded in the umbrella doc D8.
- **Collision guards** — duplicate namespace class-name and namespace-vs-message name are hard errors.
- **`requiredLangs`** feature added (config + checker).
- **Real compile-test** (`GeneratedCodeSpec`) via kctfork — the regression guard that was missing.

Accepted / deferred (agreed LOW): i18next plural-suffix false positives (`step_one`) and whitespace/
dotted placeholder syntax are documented convention limits; explicit SnakeYAML DoS limits deferred
(SafeConstructor defaults already cap aliases/depth/size).

## Implementation notes

- Forced-named mechanism is Kotlin's `vararg _: Nothing` before the real params (a param after a vararg
  must be named; `Nothing` can't be passed positionally). The Dart generator's approach uses Dart's
  native named params — not reusable; this is fresh.
- Plurals use the i18next `count` convention: `{{count}}` is implicit from a `count: Int` param.
- Locale normalization is replicated from `Locale` (not a dependency on `ultra:i18n`, to keep the build
  tool independent) — a test pins that the tags match.

## Test evidence

- [ ] Parser/builder unit tests (nesting, plural collapse, placeholder detection, locale normalization).
- [ ] Emitter golden-file tests; checker unit tests.
- [ ] `./gradlew :tooling:test`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |
