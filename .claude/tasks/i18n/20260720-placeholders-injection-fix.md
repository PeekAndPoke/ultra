# Fix: second-order placeholder injection in `ultra.common.Placeholders`

**Status:** DONE — fixed, adversarial tests green on jvm+js+metadata (2026-07-20)
**Plan:** prompted by the S1 review (`.claude/tasks/i18n/20260720-i18n-l10n-foundation.md` Interactions)
**Security-critical:** yes (injection / information disclosure in a shared utility)

## Problem

`Placeholders.Filled.replace` (`ultra/common/src/commonMain/kotlin/Placeholders.kt:20-22`) was a
`fold` of `String.replace` over the **accumulating** text — multi-pass. A value substituted for one
placeholder was re-scanned by later iterations, so a value containing `{{other}}` (e.g. user input)
got expanded into another placeholder's value.

- **Second-order injection / disclosure:** with `resolve("Hi {{name}}, {{token}}", name="{{token}}",
  token=SECRET)`, pass 1 turns `{{name}}` into `{{token}}`, pass 2 expands it to `SECRET` — the secret
  leaks into the user-controlled name field. Order-dependent on map insertion order.
- **Amplification:** chained values (`a="{{b}}{{b}}"`, …) expand exponentially (billion-laughs shape).
  Not an infinite loop (each pattern visited once), but a DoS vector.

Blast radius today: **no production callers** of `ultra.common.Placeholders` (verified — the
`funktor/insights` `PlaceholderList` and the kraft hits are unrelated mechanisms). So this is
hardening + regression-prevention before it gets used, not an active live exploit. `ultra:i18n` (S1)
already does its own single-pass substitution and does not use this.

## Fix

Single-pass: build ONE combined regex over all placeholder strings (`Regex.escape` each, alternate,
longest-first) and replace in a single scan — substituted text is never re-examined.
`ultra/common/src/commonMain/kotlin/Placeholders.kt` `Filled` now holds a `combined: Regex?` and
`replace` does `combined.replace(text) { mapping[it.value]?.let(replace) ?: it.value }`.

- Pure literal alternation → linear, no ReDoS/backtracking (verified).
- Longest-first ordering so overlapping patterns (`{{a}}` vs `{{ab}}`) resolve correctly.
- Empty mapping → returns text unchanged. Unknown match → left literal (defensive).

## Test evidence

- [x] Existing `PlaceholdersSpec` (ultra/common) unchanged and green (single-pass gives identical
      results for all non-adversarial inputs).
- [x] Added adversarial regressions: value-containing-a-placeholder not re-interpreted; chained values
      not amplified; self-referential value emitted literally; overlapping names resolve to longest.
- [x] `:ultra:common:jvmTest :ultra:common:jsTest` green; `:ultra:common:compileCommonMainKotlinMetadata`
      green (valid for native targets too).

## Review record (full 3-agent gate, at user request — security-critical)

3 opus reviewers (impl & style, correctness/API, security). Gate **PASS**.

| Reviewer | Verdict | Notes |
|---|---|---|
| 1. Implementation & code style | PASS | correct, linear (no ReDoS), KMP-safe (`Regex.escape` confirmed in common stdlib); LOW: add a metachar-in-name test |
| 2. Correctness / API | PASS | single-pass is a determinism *improvement* (old nesting was iteration-order-dependent); LOW F1 `findErrorsIn`/`replace` alphabet divergence is **pre-existing**, out of scope |
| 3. Security | PASS | actively tried to break it — injection closed, amplification eliminated, no ReDoS; tests genuinely fail vs old code; 2 nice-to-haves |

Fixes applied post-gate:
- **Defense-in-depth guard** `.filter { it.isNotEmpty() }` on the combined-regex build — a directly
  constructed empty key would otherwise match zero-width everywhere (not reachable via `fill()`).
- **Full test suite** rounded out: empty pattern-set, unknown/adjacent placeholders, empty-value,
  cross-style **TripleHash** injection + overlap, regex-metachar-in-name, and a value/following-text
  boundary-concat regression.
- Not fixed (agreed out of scope): F1 alphabet divergence and pre-existing doc noise in `Placeholders.kt`.

## Notes

- Adversarial tests double as the red-team scenarios for this fix (bypass attempts on single-pass), so
  no separate red-team task is warranted.
