# Async / suspend form validation rules

**Status:** DONE — `/feature-review` gate PASS (2026-07-21)
**Plan:** i18n/l10n foundation umbrella (`.claude/tasks/20260720-i18n-l10n-foundation.md`) — sibling
feature; validation seam originally opened in S5.

## Goal

Let form rules validate against async sources (e.g. "is this slug still available?" → server call).
This makes `Rule.check` suspend, which ripples into field validation and the `FormController` submit
flow. Add a controller-level status (spinner) and a built-in double-submit guard.

## Decisions (confirmed with user)

- **D1 — Full async Rule contract.** Both `Rule.check` AND `Rule.getMessage` become `suspend`.
  Composites (`anyRuleOf`/`allRulesOf`) re-run `check` inside `getMessage` to report only the failing
  sub-rules; keeping that precise requires a suspend `getMessage`. `GenericRule` keeps its `messageFn`
  /`i18nFn` **sync** (only the interface method is suspend) so the ~28 builders' custom `message:`
  params stay sync and unchanged. Only `given(check)` gains a `suspend (T) -> Boolean` param (it is the
  escape hatch for custom/async rules).
- **D2 — FormController API.** Add `suspend fun validate(): Boolean` (awaitable, for coroutine callers)
  AND `fun validate(onValid: suspend () -> Unit): Job?` (the onClick shape). Remove the old sync
  `validate(): Boolean` and `ifValidate`. `isValid`/`isNotValid`/`numErrors` stay **sync** but read
  cached per-field `errors` (`_fields.none { it.hasErrors }`) instead of re-running validation.
- **D3 — Status + double-submit guard.** `FormController.Status { IDLE, VALIDATING, PROCESSING, ERROR }`,
  exposed as `status` + a subscribable `statusStream` (ultra `StreamSource`). VALIDATING while rules
  run, PROCESSING while the `onValid` callback runs, ERROR if the pipeline throws. `isBusy = VALIDATING
  || PROCESSING`; `validate(onValid)` is a no-op (returns `null`) while `isBusy` → prevents double-clicks.

## async.kt scope review (user asked)

`CoroutineScope(Dispatchers.Main + SupervisorJob())` — fine for validate-on-submit. Main dispatcher is
right for DOM; SupervisorJob isolates failures. Caveats (non-blocking): no `CoroutineExceptionHandler`
(a throwing async rule only hits `console.error` via the default handler — added a try/catch → ERROR in
the controller instead); process-global scope is never cancelled, so a server validation still in flight
when a form unmounts is not cancelled. Acceptable now; a per-form lifecycle scope is the long-term fix.

## Blast radius

- Core seam: `Rule.kt`, `GenericRule.kt`, `OrRule.kt`, `generic_rules.kt` (composites → anonymous
  `Rule` objects with suspend `getMessage`; `given` check → suspend).
- Fields: `FormField.kt` (interface), `AbstractFormField.kt`, `FormFieldComponent.kt` — `validate()`
  → suspend; sync callers (`setValue`, i18n-stream re-validate) wrap in `launch { }`.
- Controller: `FormController.kt`.
- Call sites (sync `if (formCtrl.validate())` → `formCtrl.validate { }`): ~10 `FormWith*` demos in
  `kraft/examples`, `funktor/auth` LoginController + ChangePasswordWidget + ResetPasswordPage,
  `funktor/inspect` LogsBulkActionPopup, `kraft/semanticui` NoInputFieldComponent (`launch { validate() }`).
- Tests: existing specs already call `check`/`getMessage` in kotest suspend blocks (compile as-is);
  `BuiltInRulesI18nSpec.resolves` helper → `suspend`. New tests: async rule via `given`, controller
  status transitions + double-submit guard.

## Compiler bug hit during implementation

Making `Rule.check` suspend failed to compile with a misleading `Suspend function 'check' cannot
override non-suspend function ...` on every implementer, despite the source clearly being suspend.
Root cause (minimal-repro-isolated): a **K2/Kotlin-JS bug where a `companion object` inside an
interface that has a `suspend` member breaks override resolution** for that interface's suspend
functions. `Rule` had an unused `companion object`; **removing it fixed the build.** Recorded in the
`build-verification-quirks` memory.

## Status of work

- DONE — core seam (`Rule`/`GenericRule`/`OrRule`/`generic_rules` composites + suspend `given`),
  fields (`FormField`/`AbstractFormField`/`FormFieldComponent`), `FormController` (status stream +
  cached `isValid` + suspend/callback `validate` + `isBusy` double-submit guard).
- DONE — migrated all 20 sync `if (formCtrl.validate())` call sites (12 kraft demos, 4 funktor-demo
  edit pages, funktor/auth ×3, funktor/inspect ×1) + `NoInputFieldComponent` field `validate()`.
- DONE — tests green (`:kraft:core-tests:jsTest`, 391): `AsyncRulesSpec` (4 — async `given`, async
  i18n, async composite, `nonNullAnd` async), `FormControllerAsyncSpec` (3 — status IDLE→PROCESSING→
  IDLE, double-submit guard, throwing-onValid→ERROR), plus the existing rule/i18n suites. Also fixed
  a latent composite bug (anyRuleOf/allRulesOf dropped the first rule from the message).
- DONE — full downstream compile: kraft (core/semanticui/examples), funktor (auth/inspect/saas),
  funktor-demo (ops-app/adminapp).

## Review record (`/feature-review` gate, 2026-07-21)

3 opus reviewers (impl+style, domain, security) over the async diff. Verdict: **gate PASS** — no open
CRITICAL/HIGH after fixes; e2e/browser tests present & green (392). Security: no CRITICAL/HIGH
exploitable (client guard is UX-only, servers authoritative, `equalTo` non-echo intact).

| # | Sev | Finding | Resolution |
|---|-----|---------|------------|
| H1 | HIGH | Double-submit guard race: `isBusy` checked sync but VALIDATING flipped inside the dispatched coroutine → two same-tick clicks both pass | FIXED — `setStatus(VALIDATING)` now flips **synchronously before `launch`**; added same-tick double-click regression test |
| H2 | HIGH | `FormFieldComponent.setValue` fired `onChange` async + gated → controlled-input regression, diverged from `AbstractFormField` | FIXED — `onChange` fires synchronously & unconditionally; `launch { validate() }` separate |
| M1 | MED | Out-of-order async validation could overwrite newer errors; `currentValue` re-read across suspension | FIXED — snapshot value/translation once + monotonic `validationSeq` latest-wins in both field classes |
| M2 | MED | `suspend validate()` clobbered a concurrent callback run's status | FIXED — suspend form is now status-neutral (status pipeline owned by the callback form) |
| H6 | HIGH | `runValidation()` used `_fields.map { it.validate() }.all { it }` — a field validated early (captured `true`) that turns invalid while a SLOW async field is still validating left a stale `true` in the aggregate → `onValid` fired on an invalid form (user-reported) | FIXED — re-derive the verdict from `_fields.none { it.hasErrors }` AFTER awaiting all validations (works with the M1 latest-wins token); `FormControllerAsyncRaceSpec` reproduces it (edit field A invalid while field B's async check is parked) + positive control |
| M5/R2 | MED | Call sites `validate { launch { … } }` detached work → PROCESSING/isBusy inert; copy-paste footgun | FIXED — dropped inner `launch` at ChangePasswordWidget/ResetPasswordPage/LogsBulkActionPopup(×2); `onValid` now runs under PROCESSING; KDoc warns against detaching |
| M3 | MED | Composite `getMessage` re-runs `check` → doubles async server round-trips | NOTED — design cost (D1); optimize later by passing the failed-set into message assembly |
| — | LOW | No debounce/cancel on per-keystroke async rules; language switch re-runs `check`; global never-cancelled scope; status stream has no consumer yet | NOTED — deferred follow-ups (debounce, per-form scope, split check/translate on locale change, wire a "checking…" spinner to `statusStream`) |

Deliberate test-coverage decision: the controller-level "invalid field blocks onValid" path is not
unit-tested because no concrete `FormField` exists in `core-tests` (all fields are UI-module
`FormFieldComponent`s) — the branch is `if (runValidation())` over the exhaustively-tested
`field.validate()`. Same-tick guard + status + ERROR paths ARE tested via real TestBed browser runs.

- [x] `/feature-review` gate (impl+style, domain, security) — PASS.
- [x] Red-team follow-up collected: [[20260721-redteam-async-suspend-rules]].
