# ULTRA-00287: Kraft dnd: a long-press start for touch and pen (startDrag hold), so a swipe on a draggable tile still scrolls

> **Status (2026-10-31): IDEA.** A request from Klang-Research (2026-10-31), for Klang's Looks page
> (`src/jsMain/kotlin/pages/LooksPage.kt`, KLANG-00396 step 9). The maintainer asked for this direction there
> instead of a grip: "We could initiate the drag with a long press?"
>
> **Part of:** ULTRA-00257 (Epic: Kraft forms and components)

## Why this exists

Klang's look tiles call `dnd.startDrag(e, payload, source, preview)` on `pointerdown`, in a grid that scrolls. On
touch, a tile with `touch-action: none` drags on every swipe, so the grid can't be scrolled from a tile. A grip would
fix that, but the maintainer would rather not add one.

The wish is an opt-in such as `startDrag(..., hold = 400.milliseconds)`, for touch and pen only. Mouse keeps the
move threshold.
- The tile keeps `touch-action: pan-y` (or `manipulation`), so a swipe scrolls.
- A move past the threshold before the hold ends means no drag. The browser scrolls (and sends `pointercancel`), and
  neither callback runs.
- Once the hold time passes with the pointer still, the drag is armed: a vibration if available, and a hook such as
  `onArmed` so the tile can show it is lifted.
- From then on the session calls `preventDefault()` on every `touchmove`, through a non-passive listener added at
  `pointerdown`. The browser then starts no scroll, and the pointer stream continues without a `pointercancel`.
- Prior art: dnd-kit's TouchSensor `delay` activation constraint, and SortableJS's `delay` with `delayOnTouchOnly`.

Checked against the code (2026-10-31):
- `DndManager.startDrag` (`kraft/core/src/jsMain/kotlin/dnd/DndManager.kt:42`) takes `threshold: Double = 4.0`, and
  nothing time-based. Its KDoc (lines 38–40) asks for `touch-action: none`, or a grip in a list that scrolls: that is
  the limit Klang hit.
- The session (`DefaultDndManager.kt:216-222`) listens on the window, in the capture phase, to `pointermove`,
  `pointerup`, `pointercancel`, `keydown`, `dragstart`, `contextmenu` and `pointerdown`. It has no `touchmove`
  listener, so nothing can hold off the browser's pan today.
- `pointercancel` ends the session as `Ending.PointerGone` (line 180), which is the right end for a swipe that became a
  scroll during the hold.
- The context menu is already held off while a touch or pen press runs (line 194), as Klang says.
- `moved()` starts the drag at the threshold (line 260). With `hold`, a move past the threshold before the hold
  ends has to end the session quietly instead.

UNVERIFIED (web tools are blocked here; check on real devices before settling the design):
- That a cancelled `touchmove` keeps Chrome Android and iOS Safari from panning on a `pan-y` element after a still
  hold, with no `pointercancel` sent.
- Whether iOS needs the non-passive `touchmove` listener in place before the touch starts, rather than added at
  `pointerdown`. dnd-kit's docs mention an iOS caveat here.

## Decide before implementing

- The name and type: `hold: Duration?` (null meaning no hold) or `touchHold`. Whether pen holds as touch does.
- The armed hook: `DragHandle.onArmed { }` next to `onDropped` and `onCancelled`, or a stream.
- Whether the vibration is Kraft's (`navigator.vibrate`, when there is one) or left to the hook.
- What a small move during the hold does. One option is a tolerance (dnd-kit's `tolerance`) separate from the move
  threshold.

## The work

## Acceptance

- Specs drive a touch press that holds still and then drags, one that moves before the hold ends (no drag, no
  callback), a `pointercancel` during the hold, and mouse unchanged. Each assertion is mutation-checked.
  ULTRA-00279 (pointer helpers with pointer type and buttons) makes these specs easier to write.
- The `startDrag` KDoc and the docs page name the hold as the answer for scrolling lists, next to the grip.

## History

- 2026-10-31: created from Klang-Research's request; checked against `DndManager.kt` and `DefaultDndManager.kt`.
