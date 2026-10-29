# ULTRA-00265: Kraft dnd: pointer events, keyboard and pick-and-place, hover state, drop source, auto-scroll, preview

> **Status (2026-10-29): DESIGNED.** Redesigned 2026-10-29 as a new `kraft/dnd` module (maintainer); the design waits for confirmation. Requests from Klang (Klang-Research, 2026-10-28) for its look editor's equip grid; the maintainer asked Klang to file what it would like. Not scheduled; Klang can build pick-and-place itself meanwhile. The dnd has no tests today.
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

## The maintainer's direction (2026-10-29)

> "I think we should fully throw away the current DnD system. It is not in use except for some examples. We should
> probably have a DnD-Stage that auto-mounts like the Modals and a DnD Manager. Components register drop-zones in
> their lifecycle and receive events like dnd.onDragStart {} dnd.onDragStop {}, dnd.onEnter {} or dnd.onDrop {} ...
> the ui state needs to be handled by the components themselves, like highlighting when a drop is started. We might
> provide default helpers but it must be fully customizable without DOM-magic as the current impl does. The counter
> part would also be dnd.startDrag(content) ... dnd will be available as Component<*>.dnd get() =
> getAttributeRecursive(key). So yes new kraft/dnd module and in kraft/semanticui we can have the ui helpers."

So Klang's points 1 (pointer events), 3 (state, not inline styles), 4 (the drop knows its source) and 6 (the preview)
are answered by the new design; 5 (auto-scroll) is a later step; 2 (keyboard, pick-and-place) is parked with
ULTRA-00239 (the key manager stays in Klang).

## The design (proposed, to confirm)

**Module** `:kraft:dnd` (`kraft/dnd/`, package `io.peekandpoke.kraft.dnd`), depending on `:kraft:core` only.
`kraftApp { dnd() }` registers it (opt-in: a separate module cannot be added by core's builder).

**Manager**, like `ModalsManager` (`kraft/core/src/jsMain/kotlin/modals/ModalsManager.kt:20`): `interface DndManager :
AutoMountedUi` with a default implementation, `DndManager.key`, `val Component<*>.dnd: DndManager get() =
getAttributeRecursive(key)`. It owns the one drag in progress, `val current: Stream<Drag<*>?>`.

**Stage**, auto-mounted like the modals' stage: renders the preview of the current drag at the pointer, above
everything, with `pointer-events: none`. Nothing else in the DOM is touched: no styles, no datasets, no clones.

**Starting a drag**, from the source component's own `onPointerDown`:
`dnd.startDrag(event, payload = look, source = slot, preview = { div { +look.name } }, threshold = 4.px)`.
- The drag starts only once the pointer moved past `threshold`, so a click stays a click.
- The manager takes `setPointerCapture` on the event's target and listens to `pointermove`, `pointerup` and
  `pointercancel` there only for the drag's duration: mouse, touch and pen alike.
- `preview` is a renderer (`FlowContent.() -> Unit`), drawn by the stage; default: nothing but the cursor.
- Returns a `DragHandle` with `onDropped { zone -> }` and `onCancelled { }` for the source.

**Drop zones**, registered in a component's lifecycle and removed on unmount:
```kotlin
lifecycle {
    dnd.dropZone<Look>(element = { dom }, accepts = { it.look.kind == slot.kind }) {
        onDragStart { drag -> canTake = true; triggerRedraw() }   // any accepted drag started anywhere
        onDragStop { canTake = false; hovered = false; triggerRedraw() }
        onEnter { hovered = true; triggerRedraw() }
        onLeave { hovered = false; triggerRedraw() }
        onDrop { drag -> swap(drag.payload, drag.source) }
    }
}
```
- Typed by payload: `dropZone<T>` (reified, `KClass` filter) plus the `accepts` predicate.
- Hit testing: on each move, `document.elementFromPoint(x, y)` and the innermost registered zone whose element
  contains it and accepts the drag. No `mouseover`/`mouseout`, so no flicker over children.
- The component keeps its own UI state (`canTake`, `hovered`) and renders from it. That is the "no DOM magic" rule.
- `Drag<T>` carries `payload`, `source` (any, given by the starter), and the pointer position.

**Cancel:** `pointercancel`, or Esc while dragging (a plain `keydown` listener the manager holds only during a drag,
until the key manager arrives).

**Helpers in `kraft/semanticui`:** optional, on top of the zone events, e.g. a `DropZoneState` (canTake, hovered)
with a ready `css` for a Fomantic look, and a default preview. Never required.

**Removal:** `kraft/semanticui/src/jsMain/kotlin/dnd/` (Dnd.kt, drag-handle.kt, drop-target.kt) is deleted; the
fomanticui example (`kraft/examples/fomanticui/.../howto/blocks/dnd/`) moves to the new API. A breaking change in
the CHANGELOG ("the old dnd is gone; it had no users outside the examples").

## Decide before implementing

1. **Confirm the design above**, above all: opt-in `dnd()` in the app builder; zones registered with an explicit
   `element` (default the component's `dom`); `threshold` before a drag starts; Esc cancels through a plain listener
   for now.
2. **Touch:** a touch drag needs `touch-action: none` on the handle *before* the pointer goes down, or the browser
   scrolls instead. Recommendation: document it and offer `dragHandle()` as a modifier in the helpers that sets it;
   the manager cannot set it in time.
3. **Auto-scroll and pick-and-place:** later steps, not this round. Recommendation: auto-scroll as step 5 of this
   ticket if time allows; pick-and-place with the keyboard path.

## The work

1. Module skeleton `kraft/dnd` (build file like `kraft/core`, settings entry), `DndManager`, `DefaultDndManager`,
   `Drag`, `DragHandle`, the stage, `dnd()`.
2. Drop zones and hit testing; the event set; typed payloads.
3. Specs, `kraft/dnd/src/jsTest/kotlin/` with TestBed and synthetic `PointerEvent`s (real layout, so
   `elementFromPoint` works): a drag below the threshold is a click; start, enter, leave, drop in order; only
   accepting zones hear `onDragStart`; the innermost zone wins; moving over a child of a zone gives no leave/enter;
   `pointercancel` and Esc cancel with `onCancelled`; unmounting a zone mid-drag removes it cleanly; no listener left
   after a drag; the preview follows the pointer on the stage.
4. `kraft/semanticui`: delete the old `dnd/`, add the helpers; port the fomanticui example.
5. Auto-scroll (if this round).
6. Docs: a Kraft dnd page and its LLM mirror; CHANGELOG (new module, removal); README module list.

## Acceptance

- The whole equip flow of Klang's look editor (drag to a slot swaps, drag to the bench moves) written against the new
  API in a spec, with mouse, touch and pen pointer types.
- No DOM writes by the manager outside its stage.

## History

- 2026-10-28: created from Klang's requests; checked against the code the same day.
- 2026-10-28: Klang noted it in KLANG-00382 (klang `53d228ab3`) and builds its own pick-and-place first; nothing waits on
  this. Growing the rework in Klang's `workshop/` is the maintainer's call (decision 1).
- 2026-10-29: plan reviewed; the keyboard path (step 3) builds on the key manager of ULTRA-00239.
- 2026-10-29: DESIGNED.
- 2026-10-29: the keyboard path (step 3) is parked with ULTRA-00239: the maintainer keeps the key manager in Klang for now ("not stable yet").
- 2026-10-29: maintainer: throw the old dnd away; a new `kraft/dnd` module with a manager, an auto-mounted stage and lifecycle-registered drop zones; UI state in the components, helpers in semanticui. Design written, to confirm.
