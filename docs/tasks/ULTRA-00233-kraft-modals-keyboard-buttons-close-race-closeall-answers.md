# ULTRA-00233: Kraft modals: keyboard buttons, close race, closeAll answers

> **Status (2026-10-21): DESIGNED.** Klang's KLANG-00323 findings, verified against the code 2026-10-21; fix after the pause.

**Security-critical:** no

## Why this exists

Klang's look review (KLANG-00323, against Kraft 0.111.0) found three defects in the Semantic UI modals. Klang works
around them in its own YesNoModal (klang `a5ca09c5`) and drops the workaround once Kraft ships the fix. All three, and
a fourth, hold in master:

1. **Buttons are divs.** `OkCancelModal` renders `ui.negative.button { }` / `ui.positive.button { }`
   (`kraft/semanticui/src/jsMain/kotlin/modals/OkCancelModal.kt:177,185`), and `SemanticTag.invoke` is a DIV
   (`ultra/semanticui/src/commonMain/kotlin/SemanticTag.kt:47`). No focus, no Enter, no focus ring.
   `SemanticTag.Button` (`:65`) renders a real `<button>`.
2. **Two results from one modal.** `FadingModal.close` ignores repeat calls once fading
   (`kraft/semanticui/src/jsMain/kotlin/modals/FadingModal.kt:114`), but Cancel calls `onResult(Cancel)` itself
   after `close()` (`OkCancelModal.kt:180-181`). Ok then Cancel within the fade answers Ok (after 150 ms) AND
   Cancel. The handle's `onClose` runs only after the whole fade (`FadingModal.kt:135-138`), so a caller that
   waits for it answers late.
3. **`closeAll()` runs no `onClose` handlers** (`kraft/core/src/jsMain/kotlin/modals/ModalsManager.kt:100-102`),
   unlike `close()` (`:87-97`). A modal waiting for its close never gets an answer.
4. **Dismissing gives no answer.** A backdrop click (`FadingModal.kt:93-96`) or back navigation (`:63-67`) calls
   `close()` without a result, so `OkCancelModal.onResult` never fires. Expected: Cancel.

## Decide before implementing

- (4) Dismiss answers `Cancel` (recommended), or a third `Result.Dismissed`, which would be a breaking change to a
  public enum.
- Focus on open: none, or Cancel (recommended: an OkCancelModal often confirms a destructive action, so Enter
  must not confirm by accident), or Ok as an option in `View`.

## The work

1. Render both buttons with `Button` (type `button`).
2. `OkCancelModal` answers exactly once: decide the result when the click arrives, ignore later clicks, and
   deliver it through one path for Ok, Cancel and dismiss.
3. `closeAll()` runs every open handle's `onClose` handlers, like `close()`.
4. Tests in `kraft/semanticui/src/jsTest/kotlin/` and `kraft/core-tests/` (`TestBed.preact`): Enter on a focused
   button; Ok then Cancel answers once; backdrop click answers Cancel; `closeAll` runs the handlers.

**A tested model of steps 2 and 3:** Klang's `klangui/src/jsMain/kotlin/comp/YesNoModal.kt:110-128` (klang repo).
- The first click decides. `close()` is overridden to decide "no" before `super.close()`, so a later click,
  Esc or a dismissal can't change the answer.
- The answer is delivered once, when the modal is gone: from `handle.onClose`, with `onUnmount` as the
  fallback for `closeAll()`.
- Delivering after the fade is deliberate: a second Enter during the fade stays in the modal and doesn't
  reach the page.

## Acceptance

- The four tests above are green and were red before the fix (mutation-check the "answers once" guard).
- Tell Klang (through Mission-Control) the version that carries it, so its YesNoModal workaround can go.

## History

- 2026-10-21: created.
