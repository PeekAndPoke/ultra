# ULTRA-00277: Kraft FadingModal: the dimmer stops click propagation, so the router never sees a link inside a modal

> **Status (2026-10-30): IDEA.** Found by the round-4 review of the UI batch (domain charter, MINOR, out of scope:
> the code predates the batch). Not scheduled.

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
