# Functional (VDom) component review & hardening

**Status:** FUTURE — not scheduled. **Blocks** the functional-component parts of the forms DOM-tests
work. Deferred while finishing i18n.
**Area:** kraft functional/VDom components + their delegates/remembered-value mechanics.

## Why

kraft has two component styles: **class** components (`Component<P>`, `VDom.render`, `value(...)` /
`subscribingTo(...)` delegates) and **functional** components authored against `VDom` with
remembered values (e.g. `VDom.formController() = value { this.component.formController() }` in
`forms/forms.kt`). Before we write DOM tests for `formController()`/`formObserver()` inside functional
components — and before building more on them — the functional-component mechanics need a proper review
and hardening pass. There is already a tracked concern here (the S4 "functional-component delegates"
follow-up).

## Scope of the review

- **Remembered values (`value { ... }` in a VDom scope):** identity/stability across re-renders, when
  the block re-runs, teardown. Do remembered values leak or get recreated unexpectedly?
- **Subscriptions in functional components:** how `subscribingTo(stream)` / stream subscriptions are
  set up and, crucially, **torn down** on unmount — no dangling subscriptions after a functional
  component leaves the tree (ties into the async-validation unmount caveat).
- **Lifecycle:** mount/unmount/next-props hooks for functional components vs class components — parity
  and correctness.
- **`VDom.formController()` / `VDom.formObserver()`:** confirm they register/unregister fields and
  handle messages identically to the class-based `Component.formController()`; confirm the remembered
  `FormController` survives re-render and is cleaned up on unmount.
- **`this.component` access from `VDom`:** correctness of the bridge from the functional scope back to
  the owning component.

## Deliverables

- A short findings write-up (what's solid, what's fragile).
- Hardening fixes for anything unsound (lifecycle/teardown/identity bugs).
- Then unblock: functional-component `formController`/`formObserver` DOM tests
  (`20260722-forms-field-testbed-dom-tests.md`).

## Notes

- Do this as a focused review (possibly a small agent fan-out over the component/VDom modules) before
  coding fixes.
- Coordinate with the forms DOM-tests plan — its functional-component coverage depends on this.
