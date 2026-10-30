# ULTRA-00279: Kraft testing: pointer helpers with coordinates, pointer type and buttons, to drive kraft:dnd

> **Status (2026-10-30): IDEA.** From the ULTRA-00265 review gate. Not scheduled.
>
> **Part of:** ULTRA-00244 (Epic: Kraft testing (TestBed, KQuery and friends))

## Why this exists

Found by the review gate of ULTRA-00265 (round 2, domain charter; out of scope there). `kraft/testing/src/jsMain/kotlin/
kquery_events.kt:161-167`: `pointerDown()`, `pointerMove()` and `pointerUp()` dispatch a plain `Event`, with no
`pointerId`, `pointerType`, `isPrimary`, `button`, `buttons` or `clientX/Y`. `DndManager.startDrag` takes only the
primary button of the primary pointer, so a project spec (Klang's equip grid) that drives a drag through these helpers
gets no drag at all. `DndSpec` and `DropZoneStateSpec` each carry their own `pointer()` helper meanwhile.

## The work

1. `PointerEvent` helpers in kraft:testing: coordinates (default: the element's centre), pointer type and id, `button`
   and `buttons` held from down to up; a `drag(from, to)` that presses, moves past the threshold and releases.
2. Port `DndSpec`'s and `DropZoneStateSpec`'s helpers onto them; docs page `testing.astro` and its mirror.
3. Keyboard modifiers (Klang-Main-Dev, meeting 2026-10-30): `keyDown`/`keyUp`/`dispatchKeyboard` take `ctrlKey`,
   `shiftKey`, `altKey`, `metaKey` (today only `key` and `code`, `kquery_events.kt:30-39`), so a chord spec needs no
   hand-built `KeyboardEvent` (klang `workshop/src/jsTest/kotlin/keys/_key_events.kt`). `getModifierState("AltGraph")`
   through `KeyboardEventInit.modifierAltGraph` if the browsers honour it (UNVERIFIED: try Chrome and Firefox first).

## Decide before implementing

## Acceptance

## History

- 2026-10-30: created from the ULTRA-00265 review, round 2.
- 2026-10-30: keyboard modifiers added (Work 3), Klang-Main-Dev's request.
