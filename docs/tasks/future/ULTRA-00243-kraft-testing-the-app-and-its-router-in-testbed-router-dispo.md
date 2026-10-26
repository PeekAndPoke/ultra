# ULTRA-00243: Kraft testing: the app and its router in TestBed, Router.dispose, awaiting a route

> **Status (2026-10-26): IDEA.** Collected from Klang's first app-module browser tests (KLANG-00343, klang `38ef6dd6`, `src/jsTest/kotlin/comp/RouterWithTransitionsSpec.kt`); the maintainer asked Klang to send TestBed/KQuery findings to ultra "for you to collect". Not scheduled. Releases are rare now (Maven Central limits): when it is picked up, consider growing it in a workshop module first.
>
> **Part of:** ULTRA-00244 (Kraft testing epic)

**Security-critical:** no

## Why this exists

Klang-Research, 2026-10-26. Each point checked against the code the same day.

1. **The test cannot reach the app.** `TestBed.preact(appSetup = { routing { ... } }, view, test)` builds the
   `KraftApp`, but `test` only gets the root element (`kraft/testing/src/jsMain/kotlin/TestBed.kt:57`, `:82`). A spec
   that wants to navigate the app's router builds its own `RootRouterBuilder().apply { useHashStrategy() }.build()` and
   passes it as a prop. Wish: the test gets the app, e.g. `test(root, app)` or a `TestBed.app`, so
   `app.router.navToUri(...)` works.
2. **Routers never take their window listeners down.** The strategies add `hashchange`
   (`kraft/core/src/jsMain/kotlin/routing/Router.kt:75`), `popstate` and `click` (`:125`, `:127`); nothing in
   `routing/` removes them, and `disable()` (`:317`) only silences the router. Every spec's router stays on `window`
   for the rest of the run. Wish: `Router.dispose()` that removes them, and TestBed disposing the app's router on
   unmount. Note for the fix: the listeners are added as `::windowListener` etc.; each `::` makes a new function
   object, so removal needs the same stored reference.
3. **Awaiting a route.** Specs write `eventually { router.current().uri shouldBe "/x" }`. Wish: e.g.
   `suspend fun Router.awaitUri(uri)`.

What worked well for Klang: `eventually()`, KQuery as a List, TestBed's cleanup between rows (ULTRA-00240).

## Decide before implementing

- The shape of 1: a second lambda parameter (`test(root, app)`) breaks no caller if it is an overload; a `TestBed`
  property needs TestBed to hold state per render.
- Whether `dispose()` is public API or internal to TestBed.

## Acceptance

- [ ] A spec navigates the app's own router; no listener is left on `window` after a TestBed render.

## History

- 2026-10-26: created from Klang's KLANG-00343 feedback.
