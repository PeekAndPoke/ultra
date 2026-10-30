# ULTRA-00278: Kraft urlParam: writing a parameter re-renders the path from the route pattern

> **Status (2026-10-30): IN PROGRESS.** Fixed 2026-10-30 (the maintainer: bugs into this release): `urlParams` keeps the path as the address has it and replaces only the query; `RouterReplaceUriSpec` row on `/p/a@b` with a Stop guard on the page. Review gate next.

## Why this exists

`kraft/core/src/jsMain/kotlin/routing/helpers.kt:99-111`: `urlParams` writes a value with
`router.replaceUri(route = currentRoute.withQueryParams(updatedParams))`, which renders the whole address again from
the route pattern, not only the query. When the address in the bar is not the canonical form of its route (a trailing
slash, a different escape in a path segment), the path changes with it. Since ULTRA-00266, "leaving" means "another
path", so a tab switch through a `urlParam` on such an address counts as leaving: the page's leave guards are asked.

Checked against the code on 2026-10-30.

## Decide before implementing

- Keep the current path as it stands and replace only the query, or canonicalise once on arrival.

## The work

- A spec: on a non-canonical address with a leave guard, a `urlParam` write keeps the path and asks no guard.

## Acceptance

- [ ] The spec is red before the fix and green after it.

## History

- 2026-10-30: created from the round-4 review ledger of the UI batch (ULTRA-00266 gate).
- 2026-10-30: IN PROGRESS.
- 2026-10-30: fixed; spec added and mutation-checked.
