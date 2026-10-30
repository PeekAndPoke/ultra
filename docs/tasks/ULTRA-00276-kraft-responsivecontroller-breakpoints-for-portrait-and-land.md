# ULTRA-00276: Kraft ResponsiveController: breakpoints for portrait and landscape, and the orientation in its state

> **Status (2026-10-30): DESIGNED.** The maintainer asked for it on 2026-10-30, for the next release (Klang's mobile
> browser version). Breaking changes are fine. The plan below waits for the maintainer to confirm it.

## Why this exists

The maintainer (2026-10-30): "configure breakpoint for landscape and portrait screens, will be important for the mobile
browser version of Klang. This means we need two sets of breakpoint in the ctor. Breaking changes are fine. Also the
ResponsiveController must listen to the 'rotation' of the phone and update the stream accordingly."

What the code does today (`kraft/core/src/jsMain/kotlin/utils/ResponsiveController.kt`):
- It has one `Breakpoints(tablet, desktop)` set and compares the width with it, so a phone held sideways (about 844 to
  932 px wide) counts as Tablet.
- It listens to `resize` only.
- It measures `document.body.clientWidth/clientHeight`. `body.clientHeight` is the height of the content, not the
  height of the viewport: on a long page `windowSize.y` is the page's height.
- It emits on every resize event, even when the state has not changed.

## Decide before implementing

1. **One stream, with the orientation in `State`.** Yes: rotation changes the size and the display type at the same
   moment, so a second stream would only emit together with the first one. `State.orientation: Orientation`
   (`Portrait`, `Landscape`), with `isPortrait` and `isLandscape`.
2. **Two breakpoint sets in the constructor:** `ResponsiveController(portrait: Breakpoints, landscape: Breakpoints)`.
   Both compare the width. Proposed defaults:
   - portrait `Breakpoints(tablet = 768, desktop = 1200)`, the same as today;
   - landscape `Breakpoints(tablet = 960, desktop = 1200)`: a phone held sideways (932 px or less) stays Mobile, an
     iPad in landscape (1024 px and more) is Tablet.
   - The cost: a desktop window between 768 and 960 px wide becomes Mobile instead of Tablet.
3. **What orientation means:** the viewport's own orientation, `matchMedia("(orientation: portrait)")`, which is
   height ≥ width. It is what CSS media queries use, and it also works for desktop windows. The alternative is
   `screen.orientation`, which follows the device and not the window.
4. **Fix the measuring as well:** use `document.documentElement.clientWidth/clientHeight` (the viewport without
   scrollbars), and emit only when the state changed.

## The work

- `Orientation` enum and `State.orientation`; the `Breakpoints` are chosen by orientation.
- Listeners:
  - `resize`;
  - the media query's `change` event;
  - `screen.orientation`'s `change` event where the browser has it, else `orientationchange`.
- After a rotation, the state is measured again on the next animation frame, because iOS reports the old size inside
  the rotation event.
- `dispose()` removes every listener.
- Docs (`docs-site/src/pages/ultra/kraft/utilities.astro`, `docs-site/src/data/llms/kraft.md`) and a breaking-change
  line in `CHANGELOG.md`.
- Update the fomanticui example and `FieldOptionsBase.autofocusOnDesktop` if their use changes.

## Acceptance

- [ ] The spec (`TestBed`, a measuring seam for size and orientation) covers:
  - portrait and landscape choose their own breakpoints;
  - a rotation emits once, with the new size, orientation and display type;
  - an unchanged state does not emit;
  - `dispose()` removes every listener.
- [ ] Mutation check: swap the two breakpoint sets, and drop the change listener. The spec must fail for both.
- [ ] `/feature-review`.

## History

- 2026-10-30: created from the maintainer's request; plan written, waiting for confirmation.
