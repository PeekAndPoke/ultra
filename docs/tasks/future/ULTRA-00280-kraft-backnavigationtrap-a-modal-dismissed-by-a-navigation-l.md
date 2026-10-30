# ULTRA-00280: Kraft BackNavigationTrap: a modal dismissed by a navigation leaves its trap entry behind the new page

> **Status (2026-10-30): IDEA.** From the round-8 review of the UI batch (ULTRA-00266 record), pre-existing under hash routing. Not scheduled.

## Why this exists

A FadingModal pushes a trap entry (BackNavigationTrap). When a navigation to another page dismisses the modal (the
maintainer's "Close it", 2026-10-30) the trap deactivates, but `doGoBack` (`BackNavigationTrap.kt:126`) bails because
the current entry's state is no longer the trap's: the trap entry stays in the history behind the new page, and a
later Back on that page is spent on it (it lands on the old page's address a second time). Under hash routing this was
so before the change too.

## The work

- Decide: leave the entry (a harmless duplicate) or replace it when the trap closes on a navigation.
- A row: modal on /a, navigate to /b, Back, Back: the second Back leaves /a.

## Decide before implementing

## Acceptance

## History

- 2026-10-30: created.
