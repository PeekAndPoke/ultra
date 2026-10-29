# ULTRA-00272: Kraft PopupsStage: its document listeners are removed with a fresh reference and leak per app

> **Status (2026-10-29): IDEA.** Found by the review gate of the UI batch (ULTRA-00266 Review record, round 2), pre-existing. Not scheduled.
>
> **Part of:** ULTRA-00257 (Epic: Kraft forms and components)

**Security-critical:** no

## Why this exists

`kraft/core/src/jsMain/kotlin/popups/PopupsStage.kt:49-55` adds and removes `::closeAllListener` on `document`;
each `::x` is a new function object, so the removal removes nothing and every app (every TestBed render) leaves two
document listeners behind. The same bug ULTRA-00243 fixed in the router and ResponsiveController.

## The work

1. Store the listener as a `private val` and remove that.
2. A spec as `RouterDisposeSpec`'s resize row: after a TestBed render, the event reaches no stale stage.

## History

- 2026-10-29: created from the review gate of the UI batch.
