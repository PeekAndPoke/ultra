# S4 — kraft reactive i18n glue

**Status:** DONE — gate PASS after fixes, `I18nGlueSpec` (5 tests) green in-browser (2026-07-21)
**Plan:** `.claude/tasks/i18n/20260720-i18n-l10n-foundation.md` → Build step S4
**Security-critical:** no

## Spec

The kraft-side machinery that makes `val t by Translations; t.forms.x()` re-render on language switch.
Lives in `kraft/core` jsMain (uses `subscribingTo`/`Component`/app-attributes — not in `ultra:i18n`).

- [x] `I18nController` (`kraft/core/.../i18n/I18nController.kt`) — chosen-locale as a persisted
      `StreamSource<String>`; derives an `I18n` snapshot via `base.withLocale` on change; `Stream<I18n>`
      + `translateStream`/`formatStream`; `TypedKey` app attribute (mirror `ResponsiveController`).
      `suspend setLang` (D4). `create` (persisted) / `inMemory` (tests) factories.
- [x] Boot language: `create` resolves localStorage (via `persistInLocalStorage`) → `navigator.language`
      (`browserLang`) → fallback, at controller construction (no separate initializer needed — the
      controller is built before `mount`).
- [x] `Translations` / `Formatting` (`delegates.kt`) — `provideDelegate` resolving `i18nCtrl`, mapping
      to `translate`/`format`, `subscribingTo` it (class components; functional variant deferred — see
      notes).
- [x] `KraftApp.Builder.i18n(...)` + `Component<*>.i18nCtrl` accessor. `ultra:i18n` added to kraft/core.

## Deferred (recorded, not done in S4)

- **Functional-component delegates** — `Translations`/`Formatting` are class-component only
  (`Component<*>.provideDelegate` → `Component.subscribingTo`). Functional components use
  `VDom.subscribingTo`; a `VDom`-based pair is a follow-up. **Does not block S5** (kraft
  `FormFieldComponent` is a class component). The plan's "class+functional" is honestly not fully met.
- **`browserLang` uses `navigator.language`** (single primary), not `navigator.languages` ∩ the offered
  locale set — a v1 refinement (the resolver chain keeps a wrong pick non-crashing). `I18n` would need
  to expose its installed locales first.
- **Pre-existing (not S4):** `Component.subscribingTo` discards the `Unsubscribe`; i18n multiplies
  subscriber count. Framework-level, out of scope; noted for awareness.

## Test evidence

- [x] `TestBed.preact { }`: `by Translations` renders `en` → re-renders `de` after `setLang`; same for
      `by Formatting` (locale tag). 5 specs green in-browser.
- [x] Default-registration: `by Translations` with NO app i18n degrades to the key (no crash).
- [x] Boot precedence: stored locale wins over initial; initial used when nothing stored.
- [x] `./gradlew :kraft:core-tests:jsTest` — green (5 tests).

## Review record (filled by /feature-review)

3 opus reviewers (impl & style, kraft/i18n domain, security). Gate: **PASS after fixes**.

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | PASS | no correctness bugs; MED persistence/boot untested; LOW-MED `by Formatting` untested; LOW fixed-sleep, functional gap |
| 2. Domain (kraft reactive + i18n) | PASS after fix | re-render crux (D10) verified sound; **MED i18n not default-registered → crash if app omits `i18n(...)`**; MED functional delegates not delivered; LOW `navigator.language` only |
| 3. Security | PASS (empty) | hostile localStorage tag total-function-safe → fallback; escaped text render; subscription torn down on unmount; no sensitive data persisted |

Fixes applied:
- **Default-registered** an empty English `I18nController` in the kraft `Builder.init` (mirrors
  `responsive(...)`), with `I18nController.default()` — `by Translations` now degrades to keys instead
  of crashing; the `i18n(...)` builder method moved from an extension to a Builder member.
- Added tests for persistence/boot precedence, `by Formatting`, and default-degradation; replaced the
  `delay(50)` with a poll-until `awaitText` helper.
- Functional-component delegates + `navigator.languages` recorded as deferred (above).
