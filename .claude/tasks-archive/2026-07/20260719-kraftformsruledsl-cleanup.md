# Remove the useless `@KraftFormsRuleDsl` marker

**Status:** DONE (2026-07-22) — completed by `f0abc8a9 refactor(forms): remove the KraftForms* DSL
markers` (i18n-foundation branch, merged via PR #51). Scope exceeded this task in the right way: all
three markers (`@KraftFormsDsl` / `@KraftFormsRuleDsl` / `@KraftFormsSettingDsl`) removed —
definitions, all applications, dangling imports. Forms tests green per the commit.
**Plan:** — (housekeeping)
**Security-critical:** no



## Commits & files changed

<!-- Generated 2026-07-28 from `git log --follow --name-status` over this task file.
     Commits that merely renamed the doc into the archive (R100) are excluded, since
     their code belongs to whatever task shipped alongside the move. -->

| Commit | Date | Files | Subject |
|---|---|---:|---|
| `53776a47` | 2026-07-19 | 8 | feat(saas): subdomain-safe slug validation for orgs and branches |

Archived by `62c25eb1` (rename only — that commit's code belongs to another task).

### Files changed (8)

**funktor-demo/ops-app**
- `funktor-demo/ops-app/src/jsMain/kotlin/pages/OrgEditPage.kt`

**funktor/saas**
- `funktor/saas/src/commonMain/kotlin/domain/Slugs.kt`
- `funktor/saas/src/commonTest/kotlin/SlugsSpec.kt`
- `funktor/saas/src/jvmMain/kotlin/api/OrgsApi.kt`
- `funktor/saas/src/jvmMain/kotlin/domain/slugs.kt`

**kraft/core**
- `kraft/core/src/jsMain/kotlin/forms/validation/strings/string_rules_extra.kt`

**ultra/common**
- `ultra/common/src/commonMain/kotlin/regexes.kt`
- `ultra/common/src/commonMain/kotlin/strings.kt`

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
