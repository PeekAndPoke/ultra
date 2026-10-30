# ULTRA-00281: Kraft routing and modals: the round-9 MINORs of the UI batch (a guard in a modal shown with a hash navigation, rows)

> **Status (2026-10-30): IDEA.** The open MINORs of round 9 of the UI batch (ULTRA-00266 record), after a clean round. Not scheduled.

## Why this exists

Round 9 (level 3) was clean; these MINORs stay open (code, or rows), the prose ones were fixed before the release:

1. **A `guardLeaving` inside a modal shown with a hash navigation binds to the page being left** (`Router.kt:571`,
   `LeaveGuard.kt:61-62`). Under hash routing the modal's content mounts before the router follows the written
   address, so `addLeaveGuard` reads the old page; on the new page the guard is never asked, and a link in the modal
   loses its input unasked. Fix direction: bind a guard registered while a written address is on its way to that
   address's page (the rule FadingModal's watch uses). Data loss: the most important of the list.
2. **"A modal shown together with a navigation" holds within one handler only**: a route link whose own `onClick`
   opens a modal mounts it at the microtask checkpoint between listeners, on the old page; the navigation dismisses
   it. A spec with `dispatchEvent` runs no checkpoint and is falsely green.
3. **Rows**: `page -> stillOn = page` (FadingModal.kt:110) is pinned by none (row: after arriving on /b, a
   `replaceUri("/a")` dismisses); the Safari `addListener` row counts calls, not identity.
4. **Custom strategies**: `RouterStrategy.linkClicked` has a no-op default; documented now, but nothing makes a
   strategy with its own click listener override it (abstract would break existing ones).

## The work

Fix 1 with a row under hash routing; rows for 3; decide 4.

## Decide before implementing

## Acceptance

## History

- 2026-10-30: created.
