# Forms: dynamic (callback) comparison values in rules

**Status:** FUTURE — not scheduled. Deferred while finishing i18n.
**Area:** kraft forms validation rules.

## Problem

Comparison rules take a **static** operand, so they cannot compare one field against another. E.g.
`greaterThan(6)` compares to a fixed literal — there's no way to express "this input must be greater
than the value of *another* input" (min/max pairs, price-range fields, "confirm ≥ start date", etc.).

Today's state:
- `numbers/number_rules.kt` — `greaterThan(value: Number)`, `lessThan`, `inRange(from, to)`,
  `greaterThanOrEqual`, `lessThanOrEqual`: all take **static** operands.
- `comparable/comparable_rules.kt` — same, static `T` operands.
- `generic_rules.kt` — `equalTo` / `notEqualTo` / `anyOf` / `noneOf` **already** take `() -> T`
  callbacks (the pattern to generalize from). `equalTo(compareWith: () -> T)` is the model.

## Proposed solution

Make the operand a callback everywhere and have the rules work with the callback internally:
- Add `() -> Number` / `() -> T` operand variants to every comparison rule (keep the static overloads
  as thin wrappers that wrap the literal in `{ literal }`, exactly like `equalTo(x) = equalTo({ x })`).
- Internally the rules resolve the operand via the callback at `check` time, so a cross-field rule
  reads the other field's current value each validation:
  ```kotlin
  greaterThan { otherField.value }   // "greater than another input"
  inRange({ min }, { max })
  ```

## The hard part: cross-field re-validation (NEEDS A DESIGN PASS)

A field currently re-validates **only when it itself changes** (`setValue` → `launch { validate() }`).
That breaks cross-field rules where the *dependency* changes, not the dependent:

> Field B has the rule "must be greater than A". State: B=5, A=10 → B is in **error** (5 ≯ 10).
> The user lowers A to 3. B *should* now be valid (5 > 3) and clear its error — but because **A**
> changed and not **B**, B never re-runs and is **stuck showing a stale error**. The mirror case is
> just as bad: A rises past B and B should go into error but doesn't until B is next touched.

Submit-time `runValidation()` re-runs everything, so the form can't be *submitted* wrongly — but the
inline error state is stale/misleading between edits. Fixing that needs a re-validation mechanism.

Candidate approaches to weigh in the design pass:
1. **Declared dependencies.** A cross-field rule/field declares the field(s) it depends on; the
   `FormController` re-validates dependents when a dependency changes. Precise, but needs an API to
   express the dependency (field ref / key) and wiring so a rule can name another field.
2. **Re-validate all touched fields on any change.** The controller already receives
   `FormFieldInputChanged` (today it only `triggerRedraw`s — see `FormController.init`). It could
   instead re-validate every *touched* field on any field change. Simple and needs no dependency
   graph, but with async rules it would re-fire server checks on unrelated fields → must pair with
   **debouncing** and latest-wins (already have per-field seq tokens) to be acceptable.
3. **Reactive operand.** Make the comparison operand a subscribable stream (the other field's value
   stream); the dependent field subscribes and re-validates on emission. Clean data-flow, but couples
   rules to the streams layer and needs fields to expose value streams.

Interactions to keep in mind: async rules (option 2 amplifies server calls; debounce required), the
"only touched fields show errors" semantics, and the per-field `validationSeq` latest-wins guard.

## Other notes

- Rules are now `suspend` (check/getMessage). The operand callback can stay **sync** (`() -> T`) — a
  cross-field value is already in memory. Only make it `suspend` if a real need appears; sync keeps the
  API simple and avoids suspending in the common case.
- Keep the message placeholders working: the resolved operand feeds `{{limit}}`/`{{from}}`/`{{to}}`
  (see `i18n/messages.*.yaml` and the generated accessors). Resolve the operand once per `getMessage`.
- Back-compat: additive (new overloads), static call sites unchanged.

## Related

- Keyboard accessibility, select redesign, and per-field DOM tests are separate future-plans in this dir.
