# Forms: deprecate SelectField → new UiSelectField (needs design)

**Status:** FUTURE — not scheduled. **Requires a proper design/planning pass before implementation.**
Deferred while finishing i18n.
**Area:** kraft forms select components (semanticui).

## Goal

Replace the old select stack with a clean `UiSelectField` built on the **same base class as
`UiInputField`** (`AbstractFormField`), so select fields share the field lifecycle, options, validation,
i18n, and (upcoming) accessibility with every other field instead of being a bespoke component.

## Current state (to deprecate)

`kraft/semanticui/src/jsMain/kotlin/forms/old/select/`:
- `select.kt`, `SelectFieldComponent.kt`, `SelectFieldController.kt` (a controller already exists — reuse
  / refactor rather than start from zero).

The `old/` path and the "auto-suggest vs plain" behaviors are tangled; the variants aren't split cleanly.

## Direction (to be firmed up in the design pass)

- **Base:** `UiSelectField` on `AbstractFormField<T, Options, Props>`, mirroring `UiInputField`.
- **Extract the select logic into a controller** (list state, open/close, filtering, highlight index,
  selection) so the rendering components stay thin.
- **Split the variants cleanly:** plain select vs auto-suggest (type-to-filter) as distinct, well-named
  entry points sharing the controller — not one component with mode flags.
- **Keyboard controls (required):** Up/Down to move the highlight through options, Enter to select,
  Esc to close, Home/End, type-ahead. Must be fully operable keyboard-only.
- **Option groups (required):** support grouped/categorized options (group headers, non-selectable
  group labels, keyboard nav that skips headers).
- **Accessibility:** integrate with the keyboard-accessibility future-plan (TAB reaches the control;
  ARIA roles `listbox`/`option`/`group`, `aria-activedescendant`, `aria-expanded`).
- **Single vs multi-select:** decide scope in the design pass.

## Why it needs planning first

The controller API, the plain/auto-suggest split, groups, keyboard model, multi-select, and a11y roles
interact. Design these together (small doc + agreed API) before coding, then implement + DOM-test.

## Related

- Keyboard accessibility future-plan (shared a11y/keyboard model).
- TestBed DOM-tests future-plan (write the new select's tests against the redesigned component).
