# S2 — i18n emitter core (`tooling/`)

**Status:** IN PROGRESS (started 2026-07-20) — effort `xhigh` (codegen edge cases)
**Plan:** `.claude/tasks/20260720-i18n-l10n-foundation.md` → Build step S2
**Security-critical:** no

## Spec

A pure JVM library in `tooling/` that turns i18n YAML catalogs into Kotlin source (no Gradle wiring —
that is S3). Two independently testable halves:

**Input pipeline (this commit):**
- [ ] `YamlCatalogParser` — SnakeYAML → flat dotted-key → template map; locale tag normalized to match
      `ultra:i18n` `Locale.tag` (lang lower, region UPPER).
- [ ] Model (`I18nNamespace`/`I18nMessage`) + `I18nModelBuilder` — from the **fallback** catalog only
      (D8), builds the namespace tree: collapses `_one`/`_other` plural siblings into one plural
      message, detects `{{placeholder}}` names (minus the implicit `count`), supports nesting.

**Output pipeline (next):**
- [ ] `KotlinEmitter` — emits (D5/D6 split): a baked `<Prefix>Catalog : I18nCatalog` object (all
      locales), and accessors — nested receiver classes, `I18nTranslate` root extension properties, and
      non-inline top-level extension functions with **forced named params** (leading `vararg _: Nothing`)
      and `count: Int` for plurals.
- [ ] Golden-file tests.

**Checker (next):**
- [ ] `I18nChecker` — per non-fallback locale: missing/superfluous keys, placeholder rules
      (introduced=error, omitted=info), base-vs-regional handling, identical-to-base redundancy (D8).

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
