# S1 — `ultra/i18n` runtime core

**Status:** DONE — gate PASS, fixes applied, tests green (2026-07-20)
**Plan:** `.claude/tasks/i18n/20260720-i18n-l10n-foundation.md` → Build step S1
**Security-critical:** no

## Spec

The platform-neutral runtime the whole i18n foundation resolves through. No codegen, no kraft, no
build plugin — hand-written catalogs drive the tests. New KMP module `ultra:i18n` (jvm+js, mirrors
kraft/core targets; NOT native).

- [x] `ultra:i18n` module created (jvm+js), registered in `settings.gradle`, depends on `ultra:common`
      (reuse `Placeholders.DoubleCurly` for `{{var}}`). No `ultra:streams` dep — the reactive stream is
      a kraft concern (S4); refines the umbrella doc which had guessed a streams dep.
- [x] `Locale` — language + optional region; `parse`, `base` (region-stripped), `tag` (BCP-47) (D11).
- [x] `I18nCatalog` interface + `MapI18nCatalog` in-memory impl (hand-written catalogs for tests; S2
      codegen emits an equivalent `object`). Plurals are flat `_one`/`_other` keys (D7).
- [x] `MessageResolver` — two-axis resolution: locale chain outer (most-specific first), catalog
      precedence inner (app-first) (D11/D3); `{{}}` substitution; plural category selection; missing
      key returns the key as a visible marker.
- [x] `I18n` — immutable state (locale + fallback + catalogs), `translate`/`format` sub-roots (D10),
      `Builder.install` (later wins — D3), `withLocale` (fresh immutable snapshot — D4/D10).
- [x] `I18nTranslate` (open receiver; `i18n` public so cross-module generated accessors reach it) and
      `I18nFormat` (closed; near-empty for now, D9 deferred).

## Implementation notes

- Plural category selection is the simple Germanic rule (`count == 1` → one) with a `Locale` seam for
  CLDR later (D9). `pluralCategoryFor(count, locale)`.
- Precedence: `Builder` collects in install order; `build()` reverses so last-installed (app) is
  highest precedence, then the resolver walks locale chain outer / catalogs inner.

## Test evidence

- [x] `LocaleSpec` — parse/normalize, base, tag.
- [x] `MessageResolverSpec` — substitution, selected→fallback, `de-CH→de→en`, specificity-beats-source
      + app-wins-same-specificity, missing-key marker, plural, `withLocale` snapshot.
- [x] Full: `./gradlew :ultra:i18n:jvmTest :ultra:i18n:jsTest` — **green on both targets (2026-07-20)**

## Review record (filled by /feature-review)

3 opus reviewers, coordinator adversarial verification. Gate **PASS**.

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | PASS (LOWs) | unused `plugin.serialization`; `I18nFormat.i18n` public > closed surface; (multi-pass substitution — dup of security); (proposed removing `ksp` — **rejected**, kotest 6 needs it) |
| 2. Domain expert | PASS (1 HIGH + MEDs) | multi-pass injection (HIGH); `fallback=locale.base` drops the `en` net (MED); `Locale` mis-parses script subtags (MED); normalize-only-in-`parse` (MED); plural category from requested vs resolved locale (LOW/latent) |
| 3. Security | PASS (1 MED/HIGH) | second-order cross-argument placeholder injection + billion-laughs amplification via the multi-pass fold |

Fixes applied (all confirmed findings):
- **Single-pass substitution** — `MessageResolver.substitute` now uses one linear regex pass; substituted
  values are never re-scanned. Regression test `substitution is single-pass: an arg value containing
  {{...}} is NOT re-interpreted`. Dropped the `Placeholders`/`ultra:common` dependency (now unused).
- **`fallback` is required** (no `locale.base` default) — keeps the `en` safety net at the chain tail (D4).
- **`Locale` normalizes at construction** (private ctor + companion `invoke`, `@ConsistentCopyVisibility`
  so `copy()` can't bypass it) — `Locale("DE") == Locale("de")`.
- **Documented the language+region-only limitation** (scripts mis-parsed) in `Locale` KDoc (D11 v1 scope).
- **Plural category seam** commented for the CLDR/D9 latent bug.
- Removed unused `plugin.serialization`; `I18nFormat.i18n` → `internal`; kept `ksp` (kotest 6 requirement).

**Follow-up (out of S1 scope):** `ultra/common/.../Placeholders.kt:20-22` `Filled.replace` has the SAME
multi-pass second-order-injection behaviour for its other callers (email templates). Tracked as a note
in the umbrella doc's Interactions; address before user data flows through those templates (S7).

Verification: `./gradlew :ultra:i18n:jvmTest :ultra:i18n:jsTest` green on both targets; no compiler warnings.
