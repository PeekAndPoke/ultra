# ULTRA-00265: Kraft dnd: pointer events, keyboard and pick-and-place, hover state, drop source, auto-scroll, preview

> **Status (2026-10-29): DESIGNED.** Plan written 2026-10-29, waiting for the maintainer's choices (Decide section). Requests from Klang (Klang-Research, 2026-10-28) for its look editor's equip grid; the maintainer asked Klang to file what it would like. Not scheduled; Klang can build pick-and-place itself meanwhile. The dnd has no tests today.
>
> **Part of:** ULTRA-00257 (Epic: Kraft forms and components)

**Security-critical:** no

## Why this exists

Klang-Research, 2026-10-28, for Klang's look editor (design in progress, a KLANG ticket follows): an equip grid as in a
video game, 8 slots for the looks in the Motor's drawer and a bench for the rest. Dragging a look onto a slot swaps
the two, dragging it to the bench unequips it. The maintainer wrote the dnd
(`kraft/semanticui/src/jsMain/kotlin/dnd/`: `Dnd.kt`, `drag-handle.kt`, `drop-target.kt`) years ago and has hardly
used it; the requests come from reading the code for this use.

Checked against the code, 2026-10-28:

1. **Pointer events instead of mouse events.** Holds: the handle listens to `mousedown` on its host and `mousemove` /
   `mouseup` on the body (`drag-handle.kt:102-103`, `:203`), the targets to `mouseover` / `mouseout` on their parent's
   DOM (`Dnd.kt:50-52`). No touch, no pen. Crossing into a child element fires `mouseout` (clears the target,
   `Dnd.kt:25-29`) and then the child's bubbling `mouseover` (sets it again): the hover flickers rather than staying
   lost, and the highlight callbacks run each time. Pointer events with `setPointerCapture` and an `elementFromPoint`
   hit test would cover all three inputs and need no over/out pairs.
2. **A keyboard path and pick-and-place.** Holds: nothing listens to keys; no aria. Proposed: Space/Enter on a handle
   picks up, arrows or Tab move between the eligible targets, Enter drops, Esc cancels, an aria-live announcement.
   Click-to-pick, click-to-drop runs on the same state (and is how games equip).
3. **Hover feedback as state.** Holds: `greenHighlights` / `blueHighlights` write `style.backgroundColor` and keep the
   old value in `dataset["__dnd_initial__"]` (`drop-target.kt:67-114`). Proposed: `dragging` / `hovered` as state the
   target's content renders from, or classes on the target.
4. **A drop that knows its source.** Holds: `onDrop: (PAYLOAD) -> Unit` (`drop-target.kt:47`). Today's workaround is a
   payload that carries its source (`data class LookDrag(val look: Look, val from: Slot)`); a `source` on the handle
   passed to `onDrop` would make swap and move direct.
5. **Auto-scroll** near the edges of a scrolling container while dragging. Holds: none.
6. **A themed drag preview.** Holds: the preview is `cloneNode(true)` of the host (`drag-handle.kt:163-171`). Proposed:
   an optional `preview` renderer.

Not tested: no spec touches `DndDragHandle` or `DndDropTarget`; the only user is the fomanticui example
(`kraft/examples/fomanticui/.../howto/blocks/dnd/`).

## Decide before implementing

1. **Rework or new module.** The changes (pointer events, state, keyboard) touch every file. Recommendation: rework in
   place behind the same `DndDragHandle` / `DndDropTarget` names, after TestBed specs pin today's behaviour; or grow it
   in Klang's `workshop/` first (releases are rare) and port it with its tests.
2. **Keyboard semantics** (arrows by grid position, or Tab order) and the announcement texts: per the WAI-ARIA drag
   guidance; needs a short design pass. The keys run on Klang's key manager once it is in Kraft (KLANG-00325, see
   ULTRA-00239): a drag session opens a key layer, so Esc cancels the drag and nothing below answers (Klang-Research,
   2026-10-29). So step 3 waits for ULTRA-00239.
3. **Scope of one release.** Recommendation: 1, 3 and 4 first (they change the API), 2 next, 5 and 6 later.

## The work

1. TestBed specs for today's behaviour (drag, hover, drop, accepts filter).
2. Pointer events and the hit test; hover as state; `onDrop(payload, source)`.
3. Keyboard and pick-and-place, with aria-live.
4. Auto-scroll; the preview renderer.
5. Docs and the fomanticui example.

## Acceptance

- A drag by touch and by pen works in a browser spec (synthetic pointer events).
- Moving over a child of a target keeps it hovered, with no flicker of the callbacks.
- The whole equip flow (pick, move between slots, drop, cancel) runs by keyboard alone.

## History

- 2026-10-28: created from Klang's requests; checked against the code the same day.
- 2026-10-28: Klang noted it in KLANG-00382 (klang `53d228ab3`) and builds its own pick-and-place first; nothing waits on
  this. Growing the rework in Klang's `workshop/` is the maintainer's call (decision 1).
- 2026-10-29: plan reviewed; the keyboard path (step 3) builds on the key manager of ULTRA-00239.
- 2026-10-29: DESIGNED.
