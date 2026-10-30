# ULTRA-00282: Kraft dnd: the round-5 MINORs (the chord menu hold's bounds, listener and dispose rows)

> **Status (2026-10-30): IDEA.** The open MINORs of round 5 of the ULTRA-00265 gate, after its fourth clean round. Not scheduled.

## Why this exists

1. **The chord menu hold has no bound of its own** (`DefaultDndManager.kt:481`, `:494`): it ends only on that
   pointer's pointerup, pointercancel or pointerdown, or dispose. A pen whose barrel release is lost (a new id per
   contact) leaves every context menu on the page blocked until dispose; a lost mouse release swallows the next
   right-click's menu once. Fix: also end on a move of that pointer with no buttons, on a primary press of its type,
   and/or make it one-shot.
2. **A drop handler that disposes the app** gets the hold installed after dispose (it is armed after the handlers).
3. **Rows**: the listener-balance row lists no `contextmenu` / `pointerdown`; the dispose row has no chord block.

## The work

1 and 2 with rows; extend the two rows.

## Decide before implementing

## Acceptance

## History

- 2026-10-30: created.
