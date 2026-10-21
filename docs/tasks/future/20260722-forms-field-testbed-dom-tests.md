# Forms: TestBed DOM tests for all input field components

**Status:** FUTURE — not scheduled (large). Deferred while finishing i18n.
**Area:** kraft forms field components (semanticui) + kraft:core-tests.

## Goal

Real-browser behavioral coverage for **every** form-field component via `TestBed.preact { }` — typing,
parsing, validation display, `onChange` propagation, formatting, focus, and (once built) keyboard nav.
Currently there are essentially no DOM tests for the field components themselves; validation is covered
at the rule level and the controller has async/status/race specs, but the fields' actual DOM wiring is
untested.

## Scope — components to cover

`kraft/semanticui/src/jsMain/kotlin/forms/`:
- `field_input.kt` — `UiInputField` and its typed variants (text, number, date, date-time, time,
  password) built on `AbstractFormField`. Cover `fromStr`/`toStr`, parse-error → `invalidValue`,
  formatting, `onNextProps` value/user-input reconciliation, autofocus.
- `field_checkbox.kt` — checkbox field.
- `field_textarea.kt` — textarea (incl. vertical auto-resize).
- `old/select/*` — SelectField (being redesigned; see the UiSelectField future-plan — write the new
  tests against the redesigned component, not the deprecated one).
- `old/misc/NoInputFieldComponent.kt`.

## Also cover: FormController / FormObserver wiring in BOTH component styles

The current async/status/race specs construct `FormController(this)` directly. We also need coverage of
the public factories and both consumption styles:
- **Class components:** `Component.formController()` / `Component.formObserver()`
  (`kraft/core/.../forms/forms.kt`).
- **Functional / VDom components:** `VDom.formController()` / `VDom.formObserver()` — the
  remembered-value variants (`value { this.component.formController() }`).

Assert the actual mechanics through the DOM: field mount/unmount registration (the
`FormFieldMounted/Unmounted/InputChanged` message flow), `stopEvents` vs observer (`stopEvents=false`)
propagation to parent components, `isValid`/`numErrors`/`status` reacting to child field state,
`resetAllFields`, and `validate { }`/`validate()` end to end — in **both** styles, since the functional
`VDom.formController()` goes through the remembered-value delegate and could behave differently.

> Dependency: the functional-component side of this depends on the functional-component review &
> hardening task landing first (see `20260722-functional-component-review-hardening.md`) — don't write
> these against shaky functional-component mechanics.

## Approach

- Reuse the harness pattern proven in `FormControllerAsyncRaceSpec.kt`: a minimal component that
  mounts real fields, driven via `TestBed` + `typeText`/`setValue`/`click`/`selectCss`/`textContent`
  (`kraft/testing/.../kquery_dom.kt`, `kquery_events.kt`). Exercise both a class-component form and a
  functional (`VDom`) form.
- Assert against real DOM (`.field.error`, rendered error text, input `value`, focus) — don't mock what
  we can run (per CLAUDE.md testing rules).
- Exercise the async-validation interplay: touched-field re-validation, latest-wins ordering, i18n
  re-render on language switch (`i18nCtrl.translateStream`).
- Include a11y assertions once the keyboard-accessibility work lands (TAB order, arrow-key select).

## Notes

- This is a big sweep; consider fanning out per-field with a workflow when scheduled.
- Some fields live in `semanticui`, so these tests belong in a semanticui test module (or an expanded
  `core-tests` if the field harness can be made engine-agnostic).

## Related

- Keyboard accessibility + UiSelectField redesign future-plans — their behaviors should be asserted here.
