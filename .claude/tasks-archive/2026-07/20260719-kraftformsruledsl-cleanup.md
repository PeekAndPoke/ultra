# Remove the useless `@KraftFormsRuleDsl` marker

**Status:** DONE (2026-07-22) — completed by `f0abc8a9 refactor(forms): remove the KraftForms* DSL
markers` (i18n-foundation branch, merged via PR #51). Scope exceeded this task in the right way: all
three markers (`@KraftFormsDsl` / `@KraftFormsRuleDsl` / `@KraftFormsSettingDsl`) removed —
definitions, all applications, dangling imports. Forms tests green per the commit.
**Plan:** — (housekeeping)
**Security-critical:** no

## What

`@KraftFormsRuleDsl` (declared in `kraft/core/src/jsMain/kotlin/forms/forms.kt`) is applied to ~63
sites, mostly the form-validation rule builders (e.g. `string_rules_extra.kt`'s `validEmail`,
`validUrlWithProtocol`, `validSlug`). Per the user it is a useless DSL marker — it does not scope any
receivers meaningfully — and should be removed the same way the redundant DSL markers were stripped
from `ultra/semanticui`.

## Scope

- Remove the `@KraftFormsRuleDsl` annotation from all ~63 usages.
- Remove the `annotation class KraftFormsRuleDsl` declaration and its import sites.
- Confirm no behavioural change (a `@DslMarker` only affects implicit-receiver resolution inside
  nested DSL lambdas; removing it must not break any existing form/rule DSL call site — verify the
  kraft examples + inspect UI still compile).

## Notes

- Mechanical sweep; do in one pass. New code added meanwhile (e.g. `validSlug`) intentionally keeps
  the marker for file consistency until this runs, then it goes with the rest.
- Cross-ref: mirrors the earlier `ultra/semanticui` DSL-marker cleanup for precedent/approach.
