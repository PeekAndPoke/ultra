# ULTRA-00263: Kraft router: replaceUri under the hash strategy writes a path, so the route never changes

> **Status (2026-10-27): IDEA.** Bug report from Klang (Klang-Research, 2026-10-27); checked against the code the same day. Not scheduled; Klang is unblocked (navToUri in its Learn filters). A small fix with a TestBed spec under hash routing.
>
> **Part of:** ULTRA-00257 (Epic: Kraft forms and components)

**Security-critical:** no

## Why this exists

Klang-Research, 2026-10-27: under `HashRoutingStrategy`, `replaceUri` puts the route into the path instead of the
fragment, so `router.current` never gets the new route. `urlParam` (`kraft/core/src/jsMain/kotlin/routing/helpers.kt:109`)
writes through `router.replaceUri`, so every urlParam change under hash routing is lost to the other subscribers of
`router.current`, and the address bar is wrong. Klang uses the path strategy in the app; it shows in its TestBed
browser specs with hash navigation, and its Learn filters (klang `LearnTracksColumn.kt`, `choose`) now use `navToUri`
with a KDoc naming this ticket.

Checked against `kraft/core/src/jsMain/kotlin/routing/Router.kt`, 2026-10-27:

- **Holds:** `HashRoutingStrategy.replaceUri` (`:93-95`) is `window.history.pushState(null, "", uri.cleanUri())`, and
  `cleanUri` strips the `#` (`:101-103`). The URL becomes a path (`/manuals/tutorials?difficulty=X`); a relative URL
  that starts with `/` carries no fragment, so the hash is dropped rather than kept.
- **Holds:** it pushes, against the interface KDoc "without adding a new entry" (`:64`). The path strategy uses
  `replaceState` (`:143`).
- **Corrected:** the router *is* told. `Router.replaceUri` (`:417-427`) calls `navigateToWindowUri()` after the
  strategy. But that reads the route back from the hash (`getUriFromWindowLocation`, `:97-99`), which no longer holds
  it, so the new route is never resolved: `current` emits nothing new, or whatever the empty hash resolves to.
- **Found on the way:**
  - The path strategy's `replaceUri` calls `navigateToWindowUri()` itself (`:144`), and `Router.replaceUri` calls it
    again (`:425`), so a replace under path routing resolves the route and emits on `current` twice.
  - `Router.replaceUri` compares the cleaned window URI with the raw `uri` (`:418-420`). Routes rendered by the hash
    strategy start with `#` (`:79-81`), so under hash routing the "unchanged" check never matches.

## Decide before implementing

1. **Who navigates:** the strategy or `Router.replaceUri`, not both. Recommendation: the strategies only write the
   address (hash: `replaceState(null, "", "#" + cleanUri)`; path: `replaceState`), and `Router.replaceUri` navigates
   once. The path strategy's own call goes.
2. **The comparison:** clean `uri` the same way before comparing, or let the strategy compare. Recommendation: a
   strategy-side clean, so `#/x` and `/x` match under hash routing.

## The work

1. Fix `HashRoutingStrategy.replaceUri`, drop the double navigation, normalise the comparison
   (`kraft/core/src/jsMain/kotlin/routing/Router.kt`).
2. Browser specs (TestBed) for both strategies: `replaceUri` changes the address without a new history entry,
   `current` emits the new route exactly once, a `urlParam` change is seen by a second subscriber.

## Acceptance

- Under hash routing, `replaceUri("/a?x=1")` leaves `location.hash == "#/a?x=1"`, `history.length` unchanged, and
  `router.current` on the new route; each spec RED against today's code.
- Under path routing, one emission per `replaceUri`.

## History

- 2026-10-27: created from Klang's report; checked against the code the same day.
