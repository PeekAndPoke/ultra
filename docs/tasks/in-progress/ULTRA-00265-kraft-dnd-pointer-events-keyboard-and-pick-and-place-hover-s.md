# ULTRA-00265: Kraft dnd: pointer events, keyboard and pick-and-place, hover state, drop source, auto-scroll, preview

> **Status (2026-10-30): IN PROGRESS.** Built as the new `kraft/dnd` module (redesign by the maintainer, 2026-10-29), registered by `semanticUI()`, with `DndSpec` (16 rows); the maintainer checked the position and the text selection in the browser. The review gate (round 1) is next. Requests from Klang (Klang-Research, 2026-10-28) for its look editor's equip grid.
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
`kraftApp { dnd() }` registers it (a separate module cannot be added by core's builder), and the default
`KraftApp.Builder.semanticui()` builder function calls it, so every semanticui app has dnd (maintainer, 2026-10-29).
The dnd stage's z-index is above the modals' stage, so a drag works inside a modal and its preview is never hidden by
one.

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
- No DOM writes by the manager to the zones' DOM; on the page only `user-select` while a drag runs (the maintainer's
  request of 2026-10-29).

Auto-scroll (work item 5) is deferred: not in this round.

## Review record

Round 1 (level 1, reviewer-high, blind), base c8251d14..55424e5d, the dnd diff (module, semanticui registration,
example, docs). Verdict: NOT clean (4 MAJOR). All rows fixed in one batch; round 2 runs at level 2.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 sec (impl MINOR) | MAJOR | DefaultDndManager.kt:132, :264 | The drop goes to the zone of the last pointermove, not the one under the pointer at the release (wheel scroll, re-render, hidden zone) | fix | Verified by reading; row "the drop goes to the zone under the pointer at the release" |
| 1 impl, dom (sec MINOR) | MAJOR | DefaultDndManager.kt:52-65, :164-171 | No button / primary check, no `buttons == 0` check: a right press whose release a context menu took starts a drag with no button held, the next click drops | fix | Primary button only (else a handle that never calls back); a mouse move with no button held ends the drag; rows for both |
| 1 impl, dom | MAJOR | DefaultDndManager.kt:142-154 | An img or link inside the source starts the browser's native drag, whose pointercancel ends ours | fix | dragstart prevented (capture) while a session lives; row with a synthetic dragstart (a real native drag cannot be scripted) |
| 1 impl | MAJOR | ULTRA-00265 Acceptance vs DndSpec | The equip flow (slot swap, bench move, mouse/touch/pen) has no row | fix | Row "Klang's equip grid" (slots as source and zone, bench, a drop onto itself), all three pointer types |
| 1 impl, dom | MINOR | DefaultDndManager.kt:200-201 | Hit test over all zones: a zone that refused at the start can be entered and dropped on, and never hears the stop | fix | Hit test over the zones told of the start; row "a zone that refused at the start" |
| 1 impl, dom | MINOR | DefaultDndManager.kt:168-176, :264-276 | A throwing handler, or `cancel()` from one, leaves the drag half done (preview, highlights) | fix | Each handler guarded (logged); `ended` checked after every step; two rows |
| 1 impl, dom | MINOR | DefaultDndManager.kt:134-139, :280-293 | After Esc the button is still down: the click after its release reaches the source | fix | The click after that pointer's release is swallowed; row |
| 1 impl | MINOR | DndSpec.kt:231, :404, :325, :282, typed | Rows that cannot fail for their reason: innermost by mount order, a below-threshold click, the vacuous probe, leaked listeners, the typed filter | fix | Rows: inner zone registered after the outer, a below-threshold click reaches the element, the probe removed, a listener balance row, a zone of another type |
| 1 impl, dom, sec | MINOR | DndManager.kt:16-17, Drag.kt:19-21 | KDoc: "writes nothing else to the DOM" (it sets user-select); "Drag.type, which zones filter by" (nothing reads it) | fix | KDoc corrected; `Drag.type` and the `type` parameter removed (SURPLUS, below) |
| 1 impl | MINOR | DefaultDndManager.kt:184, :260-261 | -webkit-user-select restored from the user-select value | fix | Saved separately |
| 1 dom | MINOR | DefaultDndManager.kt:183-187, docs | A touch long-press selects text or shows the callout before the drag starts | fix (docs) | Docs and KDoc: `user-select: none; -webkit-touch-callout: none` with `touch-action: none` |
| 1 dom | MINOR | DndStage.kt:37-45 | The fixed stage shrinks near the right edge: the preview wraps | fix | `width: max-content` |
| 1 dom | MINOR | drag-and-drop.astro:66 | The sample needs a smart cast across modules: does not compile | fix | `when (val from = drag.source)`; the mirror with `as?` |
| 1 impl | note | DefaultDndManager.kt startDrag | A nested source loses its drag to the outer one (the event bubbles) | fix | The first startDrag for an event wins: the innermost source (the outer gets a handle that never calls back) |
| 1 impl, dom | SURPLUS | Drag.type, startDrag(type) | Nothing reads them | fix | Removed; the reified extension went with them |
| 1 impl, dom | SURPLUS | kraft/dnd/build.gradle.kts | ksp plugin and an empty testTask | reject | Every Kraft module's build file has both (kraft/core, semanticui, pixijs) |
| 1 impl, dom | SURPLUS | build.gradle.kts excludeDirs | Unrelated to the dnd | reject | A commit of its own, 3543d6c7 (MC-00031), not part of this change |
| 1 | SURPLUS | current, cancel(), onOver | Unused yet | reject | Part of the confirmed design; Klang's look editor |

After round 1: 29 rows green. Mutants, each red on its row: no hit test at the release, no button check, no
`buttons == 0` check, no dragstart listener / listener kept, no `ended` check, no swallow after Esc, `firstOrNull`
for the innermost, an untyped zone, the hit test over all zones; unguarded handlers crash the run (the throwing-handler
row).

Round 2 (level 2, reviewer-xhigh, two-phase), base c8251d14..aa3cbd86. Verdict: **CLEAN** (all three charters, 0
CRITICAL, 0 MAJOR). Gate PASS. Open MINORs go to the maintainer as a list.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 sec, impl, dom | MINOR | Drag.kt:95, DefaultDndManager.kt:220, :256 | `accepts` and `element()` run unguarded; a throw at the release skips end(): listeners, user-select, preview stay (touch: until the next drag) | list | |
| 2 all | MINOR | DefaultDndManager.kt:358-372, :75-78 | The after-Esc release waiter has no bound, outlives dispose, and a cancel() during the release hit test arms it for a later click | list | |
| 2 impl | MINOR | DefaultDndManager.kt:267, :328 | An onLeave that cancels hears onLeave twice | list | |
| 2 dom | MINOR | DefaultDndManager.kt:195-200 | A chorded release (primary up, another button held; a pen tip lifted with the barrel held) does not end the drag | list | |
| 2 dom | MINOR | DefaultDndManager.kt:172-178 | contextmenu not held off during a press (Android long press on an img or link cancels) | list | |
| 2 dom, sec | MINOR | DefaultDndManager.kt:343-355 | The 0 ms click swallow misses a touch tap's later click, and swallows a drop handler's own element.click() | list | |
| 2 dom | MINOR | DefaultDndManager.kt:51-69 | The old guard against dragging from input/textarea was dropped without a decision | list | |
| 2 dom, impl | MINOR | DraggableNumberComponent.kt:32 | The live example lacks the touch styles the docs require | list | |
| 2 sec | MINOR | Drag.kt:109 | Generic payload types are matched by erased class (Pair<A,B> vs Pair<C,D>) | list | |
| 2 impl, dom | MINOR | DndSpec | No rows: nested sources, onOver, current while running, a zone mounted mid-drag, isPrimary, DropZoneState; two rows cannot fail for their reason | list | |
| 2 impl | MINOR | Drag.kt:31 | KDoc `[zone]` does not resolve | list | |
| 2 dom | MINOR | kraft/testing kquery_events.kt:161-167 | kraft:testing's pointer helpers dispatch plain Events: they cannot drive dnd | out-of-scope -> follow-up task | |
| 2 | SURPLUS | onOver, cancel(), DropZone.type, dropZone(KClass), Drag.pointerType | onOver and cancel() are not in the confirmed design's event set (domain sticks, no severity); others could be internal | list | |
| 2 | withdrawn | various | PopupsStage `get()` (a mutant in the working tree at that moment), right-click mid-drag, `current` | withdrawn | Phase 2 |

## History

- 2026-10-28: created from Klang's requests; checked against the code the same day.
- 2026-10-28: Klang noted it in KLANG-00382 (klang `53d228ab3`) and builds its own pick-and-place first; nothing waits on
  this. Growing the rework in Klang's `workshop/` is the maintainer's call (decision 1).
- 2026-10-29: plan reviewed; the keyboard path (step 3) builds on the key manager of ULTRA-00239.
- 2026-10-29: DESIGNED.
- 2026-10-29: the keyboard path (step 3) is parked with ULTRA-00239: the maintainer keeps the key manager in Klang for now ("not stable yet").
- 2026-10-29: maintainer: throw the old dnd away; a new `kraft/dnd` module with a manager, an auto-mounted stage and lifecycle-registered drop zones; UI state in the components, helpers in semanticui. Design written, to confirm.
- 2026-10-29: maintainer: build it as designed.
- 2026-10-29: maintainer: `semanticui()` registers dnd by default; the dnd stage's z-index sits above the modals' stage, so dnd works in modals.
- 2026-10-29: from the maintainer trying the example: no text selection on the page while a drag runs; the preview keeps the grabbed share of its own size under the pointer (it may be smaller than the source). Found on the way: a press still pending at app teardown kept its window listeners; DefaultDndManager is KraftApp.Disposable and ends it.
- 2026-10-30: IN PROGRESS.
- 2026-10-30: review round 1 (level 1): 4 MAJOR, all fixed with the MINORs; round 2 next at level 2.
- 2026-10-30: review round 2 (level 2): clean. Gate PASS; the MINORs to the maintainer.
