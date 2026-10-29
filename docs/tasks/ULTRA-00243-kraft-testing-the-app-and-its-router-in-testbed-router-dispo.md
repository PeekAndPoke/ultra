# ULTRA-00243: Kraft testing: the app and its router in TestBed, Router.dispose, awaiting a route

> **Status (2026-10-29): DESIGNED.** Plan written 2026-10-29, waiting for the maintainer's choices (Decide section). Collected from Klang's first app-module browser tests (KLANG-00343, klang `38ef6dd6`, `src/jsTest/kotlin/comp/RouterWithTransitionsSpec.kt`); the maintainer asked Klang to send TestBed/KQuery findings to ultra "for you to collect". Not scheduled. Releases are rare now (Maven Central limits): when it is picked up, consider growing it in a workshop module first.
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

## Depends on / unblocks

- Pairs with ULTRA-00268 (shared WindowController): both are "a TestBed mount leaves window listeners behind". Every
  `kraftApp` builds a router even without `routing { }` (`index-jsMain.kt:107`, hash strategy by default,
  `RootRouterBuilder.kt:80`), so today *every* TestBed mount leaves a `hashchange` listener on `window`.
- ULTRA-00263 (replaceUri under hash routing) and ULTRA-00266 (leave guard) get their specs written with this.

## Decide before implementing

1. **How the test reaches the app.** Recommendation: give the `test` lambda a receiver,
   `test: suspend TestBedScope.(root: KQuery<Element>) -> Unit`, with `TestBedScope.app: KraftApp` and
   `TestBedScope.router: Router`. Every existing caller passes a lambda literal (`{ root -> ... }`), which fits a
   receiver type unchanged, so no call site changes; only a caller passing a function reference would. The
   alternative, a second parameter `(root, app)`, breaks every `{ root -> }` lambda.
2. **`dispose()` public.** Recommendation: public `Router.dispose()` and `KraftApp.dispose()` (an app mounted into a
   host page, e.g. a widget, needs teardown too); `RouterStrategy.dispose()` with an empty default body so custom
   strategies keep compiling.

## The work

1. `kraft/core/src/jsMain/kotlin/routing/Router.kt`: both strategies store their listeners as `private val`
   function references (each `::x` makes a new object, so today removal is impossible); `RouterStrategy.dispose()`
   (default `{}`) removes them; `Router.dispose()` disables and disposes the strategy.
2. `kraft/core/src/jsMain/kotlin/index-jsMain.kt`: `KraftApp.dispose()` disposes the router and every app attribute
   implementing a new `KraftApp.Disposable` (ULTRA-00268's controller uses it if it stays per-app).
3. `kraft/testing/src/jsMain/kotlin/TestBed.kt`: `TestBedScope(app, router)`; `render` and the `preact` overloads take
   the receiver lambda; `finally` calls `app.dispose()` after the unmount.
4. `kraft/testing/src/jsMain/kotlin/routing.kt` (new): `suspend fun Router.awaitUri(uri: String, timeout: Duration = 2.seconds)`
   on `eventually`, failing with "router at <current>, expected <uri>".
5. Specs, `kraft/core-tests/src/jsTest/kotlin/routing/RouterDisposeSpec.kt` and `TestBedAppSpec.kt`:
   - after a TestBed render, a `hashchange` (and `popstate`/`click` for the path strategy) reaches no router: count
     calls of `navigateToWindowUri` through a spy strategy; RED today;
   - a spec navigates the app's own router via `router.navToUri("/x")` and `awaitUri("/x")`;
   - `awaitUri` times out with the message.
6. Docs: the Kraft testing page (TestBedScope, awaitUri) and LLM mirror; CHANGELOG.

## Acceptance

- [ ] A spec navigates the app's own router; no listener is left on `window` after a TestBed render.
- [ ] All `kraft/**/jsTest` suites green with the receiver lambda (no call site edited).

## History

- 2026-10-26: created from Klang's KLANG-00343 feedback.
- 2026-10-29: plan written (maintainer: "write a detailed plan into each ticket").
- 2026-10-29: DESIGNED.
