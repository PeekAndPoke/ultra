# Forms: full keyboard accessibility (TAB order + keyboard-only operation)

**Status:** FUTURE — not scheduled. Deferred while finishing i18n.
**Area:** kraft forms field components (semanticui) + shared a11y.

## Goal

Every form input must be fully operable **keyboard-only**, with **TAB moving from one field to the
next** in a sensible order (and Shift+TAB backwards). No field may be reachable/usable by mouse only.

## Requirements

- **TAB order:** logical, matches visual order; no keyboard traps; composite widgets (select,
  date pickers, auto-suggest) expose a single tab stop and use arrow keys internally.
- **Focus visibility:** a clear focus indicator on every focusable control.
- **Per-widget keyboard semantics:**
  - Text/number/date/time/password inputs — native, verify nothing steals focus or swallows keys.
  - Checkbox — Space toggles.
  - Select / auto-suggest — Up/Down/Enter/Esc/Home/End/type-ahead (see the UiSelectField future-plan;
    this plan owns the cross-cutting a11y model, that plan owns the select specifics).
  - Any custom control (reveal-password icon, clear buttons, etc.) — reachable + Enter/Space activatable.
- **ARIA:** correct roles/labels/`aria-invalid`/error association (`aria-describedby` → the rendered
  error) so validation errors are announced.
- **Labels:** every field's label is programmatically associated (`for`/`id` or wrapping).

## Scope

All components under `kraft/semanticui/src/jsMain/kotlin/forms/` (`field_input`, `field_checkbox`,
`field_textarea`, the redesigned select, `NoInputFieldComponent`) plus the shared label/error rendering
(`renderLabel`, `renderErrors`).

## Notes

- Coordinate with the UiSelectField redesign (its keyboard nav is the hardest part) and assert all of
  this in the TestBed DOM-tests future-plan (drive Tab/Arrow/Enter/Esc, assert focus + ARIA).
- Consider an a11y audit pass (axe-core or manual) once implemented.

## Related

- UiSelectField redesign future-plan (select keyboard nav + groups).
- TestBed DOM-tests future-plan (keyboard/focus/ARIA assertions).
