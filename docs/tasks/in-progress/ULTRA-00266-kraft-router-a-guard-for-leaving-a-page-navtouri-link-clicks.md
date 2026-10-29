# ULTRA-00266: Kraft router: a guard for leaving a page (navToUri, link clicks, Back)

> **Status (2026-10-29): IN PROGRESS.** Implemented 2026-10-29; the review stopped at the safety valve (round 2, one MAJOR; ULTRA-00266 Review record), waiting for the maintainer. Request from Klang (Klang-Research, 2026-10-28), from its look editor (KLANG-00382). Not scheduled; Klang keeps unapplied drafts in memory and arms beforeunload meanwhile.
>
> **Part of:** ULTRA-00257 (Epic: Kraft forms and components)

**Security-critical:** no

## Why this exists

Klang-Research, 2026-10-28: Klang's look editor (klang `src/jsMain/kotlin/pages/LooksPage.kt`, `eb719ce71`, KLANG-00382)
holds drafts not yet applied; its design says "leaving with a draft not applied asks first". Kraft has no way to hold
a navigation away from a page. Klang's workaround: drafts kept in memory and shown again on return, plus a
`beforeunload` listener for reload and tab close.

Checked against the code, 2026-10-28 (`kraft/core/src/jsMain/kotlin/routing/`):

- **Holds:** middlewares are those of the *target* route, run in `Router.navigateToWindowUri` after the address has
  changed (`Router.kt:475-506`), and can only `Proceed` or `Redirect` (`middleware.kt:29-40`). A redirect back adds a
  history entry and the address flashes.
- **Holds:** `BackNavigationTrap` (`BackNavigationTrap.kt`, `trapBackNavigation`) answers only `popstate`, so not
  `navToUri` and not intercepted link clicks.
- **Found on the way, the hard part:** most navigations reach the router *after* the address moved. `navToUri`
  (`Router.kt:329`) goes through the strategy: the hash strategy sets `location.hash` and learns of it from
  `hashchange` (`:84-90`, `:108-114`); the path strategy pushes first, then navigates (`:131-138`); `popstate` and
  `hashchange` from Back or a typed fragment are after the fact too. A guard has to run before the strategy writes
  for `navToUri`, `replaceUri` and intercepted clicks, and for popstate/hashchange put the old address back without a
  new entry.

## Decide before implementing

1. **The API.** Klang's idea: a component-scoped guard, living and dying with its component as `trapBackNavigation`
   does: `guardLeaving { target -> LeaveResult.Proceed | Stop | Ask(suspend () -> Boolean) }`, asked before any
   navigation away from the current route. `Ask` lets the page show its own modal and go on to the target on yes.
   Recommendation: that shape; several guards are asked in mount order, the first non-Proceed wins.
2. **One mechanism with `BackNavigationTrap`, or two.** Recommendation: the guard subsumes the trap's "ask on Back"
   use; the trap keeps its "Back closes a modal" use. Decide once ULTRA-00203 (trap fires on fragment navigation) and
   ULTRA-00263 (replaceUri under hash routing) are settled, since all three touch the same listeners.
3. **`beforeunload`.** Optional flag: armed while the guard would say Stop or Ask. Browsers show only their own text.
4. **What counts as leaving:** a change of the matched route (another route pattern or other route params), never a
   change of query or fragment alone. A query change must not count: every `urlParam` filter change goes through
   `replaceUri` and would otherwise ask "leave this page?". Recommendation: this rule.

## How the router knows a guard is active (maintainer's question, 2026-10-29)

By registration, as `trapBackNavigation` does today (`BackNavigationTrap.kt:25-55`, registered in the component's
`lifecycle { onMount / onUnmount }`):

- `Component.guardLeaving { target -> ... }` finds the router through the component tree
  (`getAttributeRecursive(Router.key)`), and in `onMount` calls `router.addLeaveGuard(guard)`, which returns an
  `Unsubscribe` called in `onUnmount`. The router holds a plain list; "a guard is active" means "registered".
- The block is asked **at the moment of each navigation**, with the target, so a guard needs no "dirty" flag of its
  own in the router: `guardLeaving { if (draft.isDirty) Ask { confirmLeave() } else Proceed }`. A page without unsaved
  work says `Proceed` and nothing happens.
- `beforeunload` (the optional flag) asks the same blocks synchronously with no target; any `Stop` or `Ask` arms the
  browser's prompt (browsers show only their own text, and `Ask`'s modal cannot run there).
- No guards registered: the router does exactly what it does today (one empty-list check per navigation).

## The work

1. Guard registry on the `Router`; `navToUri` / `replaceUri` / link interception ask it before the strategy writes.
2. popstate and hashchange: ask, and on Stop or a "no" put the previous address back with `replaceState`.
3. `Component.guardLeaving { }`, removed on unmount.
4. TestBed specs for both strategies: Stop holds each entry point and leaves the address unchanged; Ask continues on
   yes, stays on no; unmount removes the guard.
5. Docs: routing page and LLM mirror.

## Acceptance

- Each entry point (`navToUri`, a link click, Back, a typed hash) is held by a Stop guard, with the address and
  `history.length` unchanged; RED without the guard.
- `Ask` with "yes" lands on the target with one history entry.

## Review record

The batch of the UI tickets went through one gate together: group A (ULTRA-00243, ULTRA-00247, ULTRA-00263,
ULTRA-00266, ULTRA-00268) and group B (ULTRA-00264, ULTRA-00267, ULTRA-00269, ULTRA-00270, ULTRA-00245, ULTRA-00262,
ULTRA-00246). Follow-ups: ULTRA-00272 (popups stage listeners), ULTRA-00273 (malformed percent-escape).


Group A = ULTRA-00243, 00247, 00263, 00266, 00268 (Kraft routing, testing, window).
Group B = ULTRA-00264, 00267, 00269, 00270, 00245, 00262, 00246 (streams, three.js, pixi).
Round 1: level 1 (reviewer-high), blind. Charters: A impl, A domain, B impl, B domain, security (A+B).
Base c8251d14..a9f412fd. Verdict: NOT clean (A: MAJORs, B: 2 MAJORs). Round 2 runs at level 2.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 A | MAJOR | Router.kt ask()/guarded + middleware redirect | A redirect inside a navigation the guard let through is guarded again: dropped while `askingToLeave` (path), or asked twice (hash, and on Proceed) | fix | Verified by reading: `ask` runs `go()` inside try with `askingToLeave` true; redirect → `navToUri` → `guarded` → `isLeaving` (current still old). Raised by A impl, A domain, security |
| 1 A | MAJOR | Router.kt browserNavigationAllowed Ask branch | Ask→yes on a held Back/Forward/typed hash pushes the target as a new entry instead of replaying the move | fix | Verified: restoreUri pushes on top of the target entry, then `navigateToUri(target)` pushes again. Replay with `history.back()` instead |
| 1 A | MAJOR | Router.kt HashRoutingStrategy (no click interception) | A `#/…` link under hash routing is written by the browser first; a held one leaves the rejected entry, Back re-asks | fix | Verified: hash strategy installs only `hashchange`. Intercept plain clicks on `#/` links → `navToUri`; a typed hash stays a documented residual |
| 1 A | MAJOR | Router.kt navBack:623, clearHistoryAndNavTo:615 | History entries removed before the guard decides; after Stop the next navBack throws | fix | Verified: `removeLast()` twice before `navToUri`. Domain charter MAJOR, impl and security MINOR: the reviewer's MAJOR stands |
| 1 A | MAJOR | RouterDisposeSpec.kt:58-127 | Dispose specs pass on `disable()` alone; listener removal unproven; no try/finally | fix | Verified: each listener returns early on `!enabled`. Re-enable after dispose before dispatching |
| 1 A | MAJOR | LeaveGuardSpec.kt | Path strategy, link click, typed hash, history length untested | fix | Ticket Work 4 / Acceptance name both strategies and every entry point |
| 1 A | MINOR | Router.kt letThrough compare | Percent-encoded hash defeats letThrough (asked twice) | fix | Compare decoded |
| 1 A | MINOR | Router.kt replaceUri writes state null | Wipes a BackNavigationTrap entry's marker | fix | Keep `history.state` |
| 1 A | MINOR | Router.kt browserNavigationAllowed internal | Custom strategies cannot ask the guards | fix | Public `followBrowserNavigation()` |
| 1 A | MINOR | TestBed.kt TestBedScope | Receiver silently rebinds a file-level `router`/`app` | fix | CHANGELOG note |
| 1 A | MINOR | WindowController.kt default | TestWindowController cannot steer code outside components | maintainer-decision | Listed for the maintainer |
| 1 A | MINOR | LeaveGuardSpec fixed delays | Back test waits on fixed delays | fix | `eventually` |
| 1 A | MINOR | specs using history.length | Chromium caps history.length at 50 | fix | Assert the entry by `history.state` instead of the length |
| 1 A | MINOR | Router.kt | No way past a guard for logout | fix | `withoutLeaveGuards { }` |
| 1 A | note | Router.kt askGuards | After Ask→yes later guards are not asked | maintainer-decision | Matches the recorded decision ("first that does not proceed decides"); listed |
| 1 A | SURPLUS | index-jsMain.kt KraftApp.Disposable | No implementer | fix | Give it its user: ResponsiveController adds a never-removed resize listener per app |
| 1 B | MAJOR | sampled.kt interval() | A repeated equal interval resets the wait: a chatty config stream stops the ticks | fix | Verified: `stopTicks()` on every emission; StreamSourceImpl/map do not dedupe. Raised by B impl and B domain |
| 1 B | MAJOR | pixi-definitions.kt zIndex/sortChildren KDoc | The trap's remedy (set sortableChildren afterwards) does not work; sortChildren is a no-op unless sortDirty | fix | Verified in pixi 8.22 sortMixin.mjs:23-24, childrenHelperMixin.mjs:88 |
| 1 B | MINOR | pixi-definitions.kt resize KDoc | Says the methods exist only with resizeTo | fix | ResizePlugin always adds them |
| 1 B | MINOR | follow_element.kt | Before init it silently does nothing | fix | `require` after init |
| 1 B | MINOR | follow_element.kt | Observer survives destroy (plan asked otherwise) | fix | Disconnect when queueResize is gone |
| 1 B | MINOR | three-definitions.kt needsUpdate | Write-only in three.js | fix | KDoc |
| 1 B | MINOR | sampled.kt | Thread on JVM/native undocumented | fix | KDoc pointer to `sampled(clock = FrameClock.ticker(…, context))` |
| 1 B | MINOR | sampled.kt, ticker.kt | Intervals > 24.8 days wrap negative → busy loop | fix | Clamp to Int.MAX_VALUE ms |
| 1 B | MINOR | sampled.kt onSub | First value read before subscribing the source | fix | Subscribe first |
| 1 B | MINOR | combining.astro | sampled section splits history/historyOfNonNull | fix | Move it |
| 1 B | MINOR | ThreeJsComponent.kt | A resize while paused leaves the canvas blank | fix | KDoc note |
| 1 B | MINOR | RenderLoop.kt | A throwing onFrame ends the loop | fix | Reschedule in finally |
| 1 B | MINOR | FrameClock.js.kt | A non-Error throw escapes `catch (e: Throwable)` | fix | `catch (e: dynamic)`; cheap either way |
| 1 B | MINOR | ThreeJsMembersSpec.kt | lookAt, renderOrder, wrapS rows cannot fail for the right reason | fix | Stronger rows |
| 1 B | SURPLUS | ThreeJsComponent clock param; createContainer | Not needed | reject | Both are planned and decided: ULTRA-00270 decision 3, ULTRA-00245 decision 3 (maintainer took the recommendations 2026-10-29) |
| 1 B | SURPLUS | sortChildren, intersectObject | Could be deferred | reject | sortChildren keeps a correct KDoc (B MAJOR fix); intersectObject is the one-object form of the planned intersectObjects |
| 1 sec | MINOR ×3 | (redirect, logout, navBack) | as above | fix | Merged into the A rows above |

## Round 2 (level 2, reviewer-xhigh, two-phase), base c8251d14..f6a0c9c2

Verdict: group B CLEAN (both charters and security: 0 CRITICAL, 0 MAJOR; every round-1 B fix holds).
Group A NOT clean: 1 MAJOR, kept by both A charters in phase 2. Safety valve (2 rounds without a clean one): stop, maintainer.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 A | MAJOR | Router.kt:124, :206 with BackNavigationTrap.kt:116-122 | The round-1 fix (keep history.state in replaceUri) makes a FadingModal's close go back onto the entry from before a replaceUri done while it was open: a urlParam change inside a modal is rolled back | maintainer-decision (valve) | Verified by reading: doGoBack goes back when state == marker; the entry below has the old URL; the router follows it. Options: (a) revert to null (planned, one leftover entry), (b) trap skips the back and clears its marker when the URL changed since its push, plus a spec |
| 2 A | MINOR | Router.kt ask() | A confirmed Ask runs go() even when the router moved on meanwhile (logout, unmount, dispose); a stale navBack can throw | list | |
| 2 A | MINOR | Router.kt hash clickListener | A `#/` link whose own onClick navigates now navigates twice | list | |
| 2 A | MINOR | link-clicks.astro:149-155, llms/kraft.md:1157 | Still say the hash strategy has no click listener | list | |
| 2 A | MINOR | Router.kt restoreUri default + replay | A custom strategy with the default restoreUri gets a broken Ask replay | list | |
| 2 A | MINOR | LeaveGuard.kt beforeunload | withoutLeaveGuards does not reach the beforeunload prompt | list | |
| 2 A | MINOR | CHANGELOG.md held moves | A held Forward also leaves an entry; a held Back drops forward history | list | |
| 2 A | MINOR | LeaveGuardSpec | Typed hash and Forward rows missing; some rows hash-only; leaving replaceUri / clearHistoryAndNavTo guard unspecced | list | |
| 2 A | MINOR | RouterReplaceUriSpec.kt:24/36, :79/88 | Still asserts history.length (capped at 50) | list | |
| 2 A | MINOR | RouterDisposeSpec hash rows | Cleanup not in finally | list | |
| 2 A | MINOR | TestBed.kt:132 | A throwing app.dispose() hides the test's failure | list | |
| 2 A | MINOR | index-jsMain.kt:79 | A replaced default ResponsiveController keeps its listener | list | |
| 2 A | MINOR | PopupsStage.kt:49-55 | Pre-existing: removes a fresh `::` reference, leaks two document listeners per app | out-of-scope → follow-up task | |
| 2 sec | MINOR | Router.kt:124, :134 | Hash replaceState/pushState resolve "#/x" against <base href> | list | |
| 2 sec | MINOR | RouteBase.kt:68, :78 | Pre-existing: decodeURIComponent throws on a crafted `%` | out-of-scope → follow-up task | |
| 2 B | MINOR | sampled.kt:88-102 | A stream of distinct intervals re-times on every change: no tick while it moves | list | |
| 2 B | MINOR | smooth.kt:64 | Stream<Smoothing?> is invariant: a Stream<Smoothing> val does not compile (`out` would) | list (maintainer call: the plan named the signature) | |
| 2 B | MINOR | sampled.kt tick() | No try/finally: one throw stops the ticks for good | list | |
| 2 B | MINOR | ThreeJsComponent.kt:179-181 | A scene mounted paused never draws (black plate until focus) | list | |
| 2 B | MINOR | ThreeJsComponent.kt | A clock prop change after mount is ignored | list | |
| 2 B | MINOR | ThreeJsComponent.Props | Two new required params, not listed as breaking | list | |
| 2 B | MINOR | three-definitions.kt renderOrder, lookAt KDoc | renderOrder only within opaque/transparent lists; lights aim at target | list | |
| 2 B | MINOR | follow_element.kt | A host sized by its content grows every frame | list | |
| 2 B | MINOR | RenderLoopSpec stop row | Cannot fail for its title (pause cancels first) | list | |
| 2 B | MINOR | ThreeJsMembersSpec.kt:108, ThreeJsComponent.kt:49, history.kt historyOfNonNull, follow_element lateinit, combining.astro order, CHANGELOG ticker clamp, SampledSpec order row | Comment wording, line length, a missing note, a simplification, section after the page nav, a missing Fixes line, an unguarded order | list | |
| 2 | withdrawn | various | The phase-1 SURPLUS remarks on intersectObject, the ThreeJs clock param, RenderLoop after a throw, the ticker clamp, public withoutLeaveGuards / followBrowserNavigation; the history.length part and the replay-wording MINOR | withdrawn | Withdrawn in phase 2 against settled rows |

## History

- 2026-10-28: created from Klang's request; checked against the code the same day.
- 2026-10-29: DESIGNED.
- 2026-10-29: the maintainer asked how the router knows a guard is active: by registration from the component's lifecycle; answered in the ticket. "Leaving" narrowed to a change of matched route (a query change from `urlParam` must not ask).
- 2026-10-29: maintainer: leaving = another matched route (pattern or route params); a query-only or fragment change never asks.
- 2026-10-29: implemented and through two review rounds (record in ULTRA-00266); round 2 not clean (one MAJOR on replaceUri keeping history.state): stopped at the safety valve for the maintainer.
- 2026-10-29: IN PROGRESS.
- 2026-10-29: maintainer at the valve: the trap checks the URL (keep the state, the trap clears its marker instead of going back when the address changed); round 3 at level 3; all round-2 MINORs applied in the same batch.
