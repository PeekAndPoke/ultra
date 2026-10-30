# ULTRA-00277: Kraft FadingModal: the dimmer stops click propagation, so the router never sees a link inside a modal

> **Status (2026-10-30): IN PROGRESS.** Fixed 2026-10-30 (the maintainer: bugs into this release): a click on a link (`a[href]`) inside a FadingModal bubbles on, so the router's window listener takes route links; other clicks still stop at the modal (why they stop is not recorded; kept). `FadingModalLinkSpec` under hash routing (path routing would navigate the test page away). Review gate with the follow-up bugs next.

## Why this exists

`kraft/semanticui/src/jsMain/kotlin/modals/FadingModal.kt:111-117`: the dimmer's `onClick` calls `stopPropagation()`
for every click inside the content. The router listens for link clicks on `window`
(`kraft/core/src/jsMain/kotlin/routing/Router.kt:110` hash, `:210` path), so a route link inside a modal is never
routed:
- **path strategy:** the browser follows the link, and the page reloads;
- **hash strategy:** the browser writes the hash itself, so a leave guard can only hold it on the hashchange, after
  the address has changed.

Checked against the code on 2026-10-30.

## Decide before implementing

- Why is the propagation stopped? Probably so outer listeners (a modal underneath, popups that close on a document
  click) do not react. Then a narrower fix fits: stop it only for those, or let the router listen in the capture
  phase.

## The work

- A spec: a route link inside an open FadingModal, under both strategies, goes through the router (no reload, guards
  asked).

## Acceptance

- [ ] The spec is red before the fix and green after it.

## History

- 2026-10-30: created from the round-4 review ledger of the UI batch (ULTRA-00266 gate).
- 2026-10-30: IN PROGRESS.
- 2026-10-30: fixed; spec added and mutation-checked.
