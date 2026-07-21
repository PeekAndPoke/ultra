# S5 — kraft forms i18n (first vertical slice)

**Status:** DONE — gate PASS after fixes, all tests green + downstream compiles (2026-07-21) — effort `xhigh`
**Plan:** `.claude/tasks/20260720-i18n-l10n-foundation.md` → Build step S5
**Security-critical:** no

## Spec

The first end-to-end proof on a real surface: kraft form-validation messages resolve through `I18n`,
translate, and re-render on language switch. Full 3-reviewer gate.

### Rule seam design (DECIDED — additive, zero test churn)

`getMessage(value): String` is called by `FormFieldComponent`, `AbstractFormField`, `OrRule`, the
composite rules, and ~60 `ValidationRulesSpec` assertions. Changing its return type would break all of
them. So instead — **additive**:

- `Rule` keeps `getMessage(value): String` (the plain English default) and gains
  `getMessage(value, translate: I18nTranslate): String = getMessage(value)` (default delegates to plain).
- `GenericRule` gains optional `i18nFn: ((T, I18nTranslate) -> String)?` and overrides the 2-arg form
  (`i18nFn?.invoke(value, t) ?: messageFn(value)`). `invoke(customMessage)` clears `i18nFn` (a custom
  message is never translated).
- Each builder splits `message: String = "default"` into: `foo(args)` (i18n default — English kept as
  the `messageFn` fallback so the 1-arg path + existing tests are unchanged), `foo(args, message: String)`
  (plain), `foo(args, message: (T)->String)` (plain). The i18n default's `i18nFn` calls the generated
  kraft catalog accessor (e.g. `t.forms.minLength(count = length)`).
- `FormFieldComponent` subscribes to `i18nCtrl.translateStream` with an `onNext` that re-validates, and
  `validate()` uses the 2-arg `getMessage(value, t)` — so errors translate and re-render on switch.
  Keeps `errors: List<String>` (no ripple to rendering).
- Zero `ValidationRulesSpec` churn (they use the 1-arg `getMessage`, still English).
- [x] **`AbstractFormField`** (the real base of the semanticui fields) **and `FormFieldComponent`**
      subscribe to `i18nCtrl.translateStream` with an `onNext` that re-validates, and `validate()` uses
      the 2-arg `getMessage(value, translate)`. Both `// TODO: how to translate this?` markers killed
      (`FormFieldComponent`, `field_input`/`UiInputFieldComponent`) → `t.forms.invalidValue()`.
- [x] **kraft's catalog** — `messages.{en,de}.yaml` (all ~28 rules, full parity), generated via the
      plugin (`packageName io.peekandpoke.kraft.i18n.generated`, `sourceSet jsMain`, `requiredLangs("de")`);
      installed **by default** in `I18nController.default()` + `I18n.Builder.installKraftForms()` for apps.
- [x] All ~28 builders wired: no-arg → i18n default (English kept as `messageFn` fallback), `String`/lambda
      → plain. Cleaned pre-existing `_root_ide_package_.`/inline-FQCN artifacts while refactoring.
- [x] Back-compat: literal message + lambda overloads preserved.

## Notes

- **Composite rules now translate** (fixed in gate): `OrRule`/`anyRuleOf`/`allRulesOf`/`nullOrElse`/
  `nonNullAnd` override the 2-arg `getMessage` to compose children via their 2-arg form, joined with
  translated `forms.joinOr`/`forms.joinAnd`. Test: `notBlank() or validEmail()` → German children+joiner.
- A full **field-UI `TestBed` test** (mount a field, `setLang`, assert DOM flips) is **covered by
  composition**: S4's `I18nGlueSpec` proves a component re-renders on `setLang` via the same
  `subscribingTo`+`onNext` path (which `AbstractFormField`/`FormFieldComponent` reuse), and
  `FormRulesI18nSpec` proves rules resolve via i18n en/de. Accepted by the gate.

## Test evidence

- [x] `FormRulesI18nSpec` (5) — default rule resolves via the kraft catalog in en+de; plurals
      (minLength 1 vs 5); placeholders (inRange from/to); custom message never translated; 1-arg
      back-compat.
- [x] `ValidationRulesSpec` — **178 tests unchanged & green** (they use the 1-arg `getMessage`).
- [x] `checkI18n` green (German at full parity).
- [x] Downstream compiles: `:funktor:auth`, `:funktor-demo:adminapp` (call sites intact).
- [x] `./gradlew :kraft:core-tests:jsTest :kraft:semanticui:jsTest :kraft:core:checkI18n` — green.

## Review record (filled by /feature-review)

3 opus reviewers. Gate: **findings → fixed → PASS**.

| Reviewer | Verdict | Confirmed findings (all fixed) |
|---|---|---|
| 1. Implementation & code style | fixed→PASS | **HIGH** composites don't translate; **MED** `noneOf({..})` compile break; LOW English divergence; seam/re-render/back-compat verified sound |
| 2. Domain (i18n + forms) | fixed→PASS | **MED** "installs by default" was a convention → app that forgets loses the catalog; LOW stale Builder comments; catalog verified complete + German idiomatic |
| 3. Security | fixed→PASS | **MED** `equalTo`/`notEqualTo` echoed the operand → confirm-password leaks the password; single-pass/XSS/parse-path verified safe |

Fixes applied:
- **Security** — dropped the value echo from `equalTo`/`notEqualTo` (catalog + `i18nFn` no-arg + `messageFn`);
  regression test asserts no operand leaks (i18n and fallback).
- **Force-install** — `I18nController.create`/`inMemory` now build the `I18n` themselves and always
  `installKraftForms()` (lowest precedence; app catalogs still win — D3). Block-form factory API;
  `I18nGlueSpec` updated. Stale Builder/delegate comments fixed.
- **Composites translate** — `OrRule` + combinators override 2-arg `getMessage`; `forms.joinOr`/`joinAnd`
  catalog keys; composite `or` test.
- **`noneOf({..})`** lambda-source overload restored (+ symmetric `anyOf`); English yaml aligned to the
  `messageFn` fallbacks (`exactLength`/`exactCount`/`greaterThanOrEqual`/`lessThanOrEqual`).

Verification: `FormRulesI18nSpec` (7), `ValidationRulesSpec` (178, unchanged), `I18nGlueSpec` (5),
`checkI18n` (de parity), downstream (`funktor:auth`, demo apps) compile.
