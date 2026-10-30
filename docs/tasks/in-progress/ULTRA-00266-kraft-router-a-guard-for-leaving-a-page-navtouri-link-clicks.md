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

## Round 3 (level 3, reviewer-top), base c8251d14..1fa45f21, phase 1 only (valve)

Note: two reviewers (A impl, B domain) read the earlier record through the tickets named in the brief; both disclosed it.
Verdict: NOT clean in either group. Phase 2 not run: both MAJORs verified by the coordinator (B by a one-off experiment,
red on JVM and JS) and neither overlaps a settled row. Safety valve re-armed: maintainer.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 3 A sec | MAJOR | Router.kt isLeaving vs RouterComponent.kt key | The guard calls "the same page" equal mounted route + decoded params; RouterComponent remounts on any raw-path change. `#/edit/%31`, a urlParam re-encoding a param, or another catchAll/RoutePattern path remounts the guarded page and loses its draft, unasked | maintainer-decision (valve) | Verified by reading RouterComponent.kt:37-56 and RouteBase.kt:67-69. Touches the recorded rule "leaving = another matched route" |
| 3 B impl | MAJOR | sampled.kt:69 | The keep-fresh handler captures nothing: one shared instance; two samplers on one source share one subscription in the source's handler set; the first to leave unsubscribes both | fix (pending) | Experiment: fold source, two samplers, leave the first: the second sees no new value (JVM and JS red) |
| 3 A | MINOR ×14 | (see the round-3 reports) | stale-Ask check too strict for same-page moves; awaitUri before render; link-click row cannot fail on preventDefault; guard removal untested; BackNavigationTrap KDoc; style (line length, import order, ReturnCount); withoutLeaveGuards vs async beforeunload; replay without checking the entry; normalize default for prefixed custom strategies; guards outside routed pages inert; "typed address" wording under path routing; scroll restoration on a held move; letThrough outliving a replay; public addLeaveGuard | pending | |
| 3 B | MINOR ×8 | (see the round-3 reports) | Light.shadow for base-typed callers; addons.astro not updated (pixi/three); sampled thread context; sampled.kt:20 line length; a throwing first render kills the scene; steady(false) default per call; CREDITS line not written; smooth frame without finally | pending | |

## Maintainer, 2026-10-29, at the valve after round 3

- Same page: "Leaving = path changes": the guard asks whenever RouterComponent would remount (another path before `?`
  and `#`); query-only and fragment changes still never ask. Replaces the rule "another matched route".
- Fix both MAJORs and all round-3 MINORs in one batch, then round 4 at level 3.

## After round 3: what was done (6273cbc2 group A, 6ae6b9f6 group B)

All round-3 rows FIXED, as the maintainer decided:
- A MAJOR: leaving = another path (pagePath: before ? and #), RouterComponent's key; targets normalized by URL parsing
  (browser encoding) in both strategies; guards bind to the page path. Spec: `%31` under both strategies.
- B MAJOR: sampled subscribes its source with a handler of its own (keepFresh). Spec: two samplers, fold source.
  The same trap in Stream.subscribeToStream's default and permanent(): ULTRA-00274 (out of scope, pre-existing).
- A MINORs: page-scoped stale check for guarded Asks (identity for the Back replay); restored entry marked, replay
  only from it (else navigate to the target); 3 s unload window after withoutLeaveGuards; scroll restoration manual
  while guards are registered; warning for guards registered on the placeholder route; addLeaveGuard/LeaveGuard
  internal; awaitUri waits a frame; link-click row checks defaultPrevented; unmount removal and beforeunload removal
  rows; KDoc: page loads vs router navigations, trap deactivate, normalize for prefixed strategies; style.
- B MINORs: sampled context parameter; smooth frame cleanup on a throw; Light.shadow on the base; ThreeJs loop starts
  after a throwing first render; one NotPaused default; addons.astro pixi extras and three.js section; CREDITS plan
  change recorded (ultra has not adopted CREDITS.MD); line wraps.

## Round 4 (level 3, reviewer-top, phase 1 blind), base c8251d14..6ae6b9f6

Group B: CLEAN (all three charters). Group A: NOT clean, 1 MAJOR, verified by the reviewer's one-line experiment
(red under Hash). Phase 2 not run (valve; the MAJOR overlaps no settled row).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 4 A impl | MAJOR | Router.kt:653 | The "restored, but something stands on top" branch navigates without setting letThrough: under hash routing its hashchange asks the guards again (a second "Leave?") | fix (pending maintainer) | Experiment: `control.asked shouldBe listOf("/a")` in the row: red under Hash |
| 4 A | MINOR | Router.kt:641-644 (2 charters) | A Back-started Ask drops a yes after any emission (urlParam), contradicting the docs | pending | |
| 4 A | MINOR | Router.kt:544-556 (2 charters) | scrollRestoration is per entry: the toggle does not keep a held Back from scrolling, and restores the wrong entry | pending | |
| 4 A | MINOR | Router.kt:571-572 (sec + domain) | Internal approvals open the 3 s unload window: the next page's beforeunload is muted | pending | |
| 4 A | MINOR | Router.kt:559-562 (impl + domain) | withoutLeaveGuards KDoc: history.back() inside it is still asked | pending | |
| 4 A | MINOR | Router.kt:535-542 (all three) | An app-shell guard binds to page "", the hash home page: asked there | pending | |
| 4 A sec | MINOR | Router.kt:581-594 | letThrough left set when go() throws (cross-origin pushState): a later Back passes unasked | pending | |
| 4 A dom | MINOR | Router.kt:709-722 | A stale Ask keeps askingToLeave after a navigation past the guards | pending | |
| 4 A dom | MINOR | helpers.kt:99-111 | urlParam re-renders the path: a non-canonical address makes a tab switch "leaving" | pending (follow-up?) | |
| 4 A dom | MINOR | FadingModal.kt:111-117 | Pre-existing: the dimmer stops click propagation, no router intercepts links in a modal | out-of-scope → follow-up task | |
| 4 A impl | MINOR | LeaveGuardSpec.kt:509-513, :326-340 | Proceed check inside the unload window is vacuous; the "goes with its component" row cannot fail for its reason | pending | |
| 4 B | MINOR ×10 | (reports) | zIndex KDoc remedy (equal value is a no-op, 2 charters); smooth law change does not resume a motion stalled by a throw; long→short interval waits out the long tick (decided); inline clock restarts the loop (KDoc); first draw before onFrame (decided); mirror/page notes; contract row lacks the new names; onSub half-subscribed after a throw (sampled, smooth); first-render throw inside the Preact commit; no needsUpdate row on Texture | pending | |

## Maintainer, 2026-10-29, at the valve after round 4

- Group A: "Fix all, round 5": the MAJOR and the MINORs; the ineffective scrollRestoration toggle dropped; round 5 at
  level 3.
- Group B: its MINORs applied in the same batch. Kept as decided, not changed: a long-to-short interval waits out the
  tick due (ULTRA-00267 KDoc); the first draw before any onFrame (ULTRA-00270 KDoc).

## After round 4: what was done (3ae0b38f group A, 6da0b366 group B; 5d18de10 follow-ups)

All round-4 rows FIXED, as the maintainer decided ("Fix all, round 5"):
- A MAJOR: the "restored, but something stands on top" branch navigates via navToUri inside the approved scope, so
  its hashchange is let through and not asked again. Spec: `control.asked shouldBe listOf("/a")` under both.
- A MINORs: Back-started Ask keeps its question over a query-only move (page check); internal approvals no longer open
  the beforeunload window (public withoutLeaveGuards only); app-shell guards bind to no page (null); letThrough
  cleared when go() throws; a navigation past the guards to another path cancels a pending Ask; the scrollRestoration
  toggle removed (per entry, ineffective); withoutLeaveGuards KDoc names history.back(); spec rows strengthened.
- A out-of-scope: ULTRA-00277 (FadingModal stops click propagation), ULTRA-00278 (urlParam re-renders the path).
- B MINORs: zIndex KDoc (an equal value is a no-op, verified sortMixin.mjs set zIndex); smooth's lawChanged resumes a
  stalled motion; inline-clock KDoc (keep a clock in a field); mirror notes (first draw, setSize while paused, clock,
  sampled context); contract row names the new members; onSub unwinds on a throw (sampled, smooth) and
  StreamSourceImpl drops the handler when onSub throws; first ThreeJs draw reported on its own task
  (drawOnceThenStart); Texture needsUpdate row. Kept as decided: long-to-short interval, first draw before onFrame.

Added to the change set for round 5 (new features, their round 1): ULTRA-00275 (WakeLock: d540e896, ae7bb1cb),
ULTRA-00276 (ResponsiveController portrait/landscape: b855d9e6).

## Round 5 (level 3, reviewer-top, two-phase), base c8251d14..55424e5d

Scope: group A, group B's round-4 MINOR batch, and the first round of ULTRA-00275 (WakeLock) and ULTRA-00276
(ResponsiveController). Verdict: NOT clean: 1 MAJOR (impl, kept in phase 2; domain and security found the same and
graded it MINOR: the reviewer's MAJOR stands). Coordinator experiment: the row is red under both strategies.
Safety valve re-armed: maintainer.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 5 A impl (+dom, sec) | MAJOR | Router.kt:736-751, :717-723 | An Ask cancelled before its coroutine starts never runs its finally: askingToLeave stays true, every leaving navigation from a guarded page is dropped, every Back undone, until reload | fix | Experiment: LeaveGuardSpec "an Ask dropped before it ran" red under Hash and Path |
| 5 A sec | MINOR | Router.kt:387, :570; LeaveGuard.kt:52 | passingLeaveGuards reads `approved`, set by every internal approval: a page load during one gets no beforeunload prompt; KDoc "no page load is approved by them" wrong | fix | Verified: approving() sets approved |
| 5 A sec | MINOR | Router.kt:633-639 | The let-through arrival leaves the page past the guards without cancelAskIfLeaving | fix | |
| 5 A dom | MINOR | TestWindowController.kt:57-59 | visibility(false) leaves hasFocus true; the browser never has that state | fix | |
| 5 A impl | MINOR | Router.kt:156-157 | urlParam under hash routing (`replaceUri("#/...")` changed) has no row; a `##` mutant survives | fix | |
| 5 A impl | MINOR | Router.kt:113-116 | The hash click listener's removal on dispose is untested | fix | |
| 5 A impl | MINOR | LeaveGuardSpec | No Forward row (round-2 MINOR listed typed hash and Forward) | fix | |
| 5 A impl/sec | MINOR | llms/kraft.md:1253-1262 | The mirror lacks the history shape of held Forward / held Back | fix | |
| 5 B dom/impl/sec | MINOR | StreamSourceImpl.kt:47; CHANGELOG | A handler that throws on its first value stays subscribed; the CHANGELOG headline overclaims | fix | |
| 5 B dom | MINOR | smooth.kt:239-244 | A null law does not land a motion a throwing law stalled | fix | |
| 5 B impl | MINOR | SampledSpec.kt:174 | The half-subscribe row cannot catch a missing unwind (source throws first) | fix | |
| 5 B impl | MINOR | ThreeJsComponent.kt:120-130, 193-202 | The paused wiring has no component-level row | fix | |
| 5 00275 impl | MINOR | WakeLock.kt:81-89 | Every controller adds a visibilitychange listener nothing removes (a test fake per test) | fix | |
| 5 00276 dom | MINOR | ResponsiveController.kt:146-159 | A height-only change (a resizing on-screen keyboard, a docked DevTools) can flip 768-959 px between Tablet and Mobile; not said | fix (document) | |
| 5 | SURPLUS | WakeLock.kt:79, index-jsMain.kt | holderCount public; the builder method deviates from ULTRA-00275 decision 1 | reject | The builder registration is the maintainer's request of 2026-10-30 ("Add the wakelock to the kraftApp builder like windowCtrl()"), recorded in ULTRA-00275; holderCount is the counter's state for apps (cheap) |
| 5 | withdrawn | various | ShadowMap KDoc; an Ask-yes passing a later Stop; the 3 s window; public followBrowserNavigation; intersectObject; the ThreeJs clock param; the RouterReplaceUriSpec comment | withdrawn | Withdrawn in phase 2 against settled rows |

## Maintainer, 2026-10-30, at the valve after round 5

"Fix all, round 6": the MAJOR and every MINOR in one batch, then round 6 at level 3; the dnd gate's round 2 runs in
parallel.

## After round 5: what was done (commit below)

All round-5 rows FIXED, as the maintainer decided ("Fix all, round 6"):
- MAJOR: the Ask's state is reset on the job's completion (identity-checked, also for a job cancelled before it ran)
  and at once in cancelAskIfLeaving. Row "an Ask dropped before it ran" under both strategies.
- beforeunload: only withoutLeaveGuards approves a page load (its own flag); the router's own approvals no longer do.
  Row: a page load started inside an approved navigation is prompted.
- The let-through arrival cancels a pending Ask (Hash row). TestWindowController: hidden blurs (row). Rows: urlParam
  under hash, the hash click listener's dispose, Stop holds a Forward. Mirror: held Forward / Back history shape.
- Streams: a handler that throws on its first value is dropped and what onSub started released (StreamSourceImpl,
  StreamWrapperBase); a null law lands a stalled smooth; the sampled half-subscribe row throws after the source
  subscribed. ThreeJs: ThreeJsComponentSpec mounts the component on a fake three module (paused, a new paused).
- WakeLock: the visibility listener only while held (row). ResponsiveController: the height dependence documented.
Mutants, each red on its row: both Ask resets, approved in passingLeaveGuards, no let-through cancel, `##` hash URL,
click listener kept, hidden keeps focus, WakeLock listener never added, first-call handler kept (source, wrapper),
null law not landing, sampled no unwind, no followPaused, new paused ignored.

## Round 6 (level 3, reviewer-top, two-phase), base c8251d14..aa3cbd86

Verdict: **CLEAN** (all three charters, 0 CRITICAL, 0 MAJOR, after phase 2). Gate PASS for group A, ULTRA-00275 and
ULTRA-00276. Open MINORs go to the maintainer as a list (below). Process incident during this round: a reviewer read a
mutant (PopupsStage) applied for a mutation check; withdrawn in phase 2; `_incidents.md`.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 6 impl, dom | MINOR | Router.kt:561, CHANGELOG.md:69 | The sample `router.withoutLeaveGuards { navToUri("/login") }` does not compile (block has no Router receiver) | list | |
| 6 dom | MINOR | ResponsiveController.kt:126 | Safari <= 13: MediaQueryList has no addEventListener; every kraftApp throws at start | list | |
| 6 dom | MINOR | ResponsiveController.kt:27-31, CHANGELOG | Positional `ResponsiveController(Breakpoints(...))` compiles and silently means portrait only | list | |
| 6 sec, dom, impl | MINOR | StreamCombinator.kt:53-55, ops/cutoff.kt:60-61, ops/switchMap.kt:97-98 | The round-5 first-value-throw rollback did not reach these three; CHANGELOG headline overclaims | list | |
| 6 impl | MINOR | Router.kt:613, :650, :554 | A pending Ask whose guard unmounted in-page is not cancelled when the page is left through a plain branch: askingToLeave stays true | list | |
| 6 impl | MINOR | LeaveGuardSpec.kt:353 | The page-binding row cannot fail for its reason (awaitUri waits a frame); a same-turn path-strategy row would | list | |
| 6 impl | MINOR | ThreeJsComponent.kt:123-126 | The clock-change branch has no row | list | |
| 6 sec | MINOR | LeaveGuard.kt:43 | KDoc: "unless the page load starts in withoutLeaveGuards": also within 3 s after | list | |
| 6 sec, dom | note | ULTRA-00275 decision 1 | The ticket still says "no change to the app builder" | list (record) | |
| 6 dom | NIT | three-definitions.kt:183 | PCFSoftShadowMap: removed in r186.1's renderer (constants.js says deprecated) | list | |
| 6 sec, impl | NIT | routing.astro, llms/kraft.md; CHANGELOG.md:67 | Server logout via withoutLeaveGuards not in the docs; "every urlParam never asks" vs ULTRA-00278 | list | |
| 6 | SURPLUS | ResponsiveController(measure), TestWindowController.focus/resize | Test seam in the public constructor; uncalled helpers | list | |
| 6 | withdrawn | various | DropdownMenu propagation (same class as ULTRA-00277: extend that ticket), holderCount, public followBrowserNavigation, 3 s window, PopupsStage `get()` (a mutant) | withdrawn | Withdrawn in phase 2 against settled rows or the file at HEAD |

## Round 7 (level 2, reviewer-xhigh, two-phase), base c8251d14..2a31958a

Scope: the round-6 MINOR batch (7bdd659d) and the first review of ULTRA-00272, 00273, 00274, 00277, 00278. Verdict:
NOT clean: 1 MAJOR (impl and domain; security graded the same MINOR: the reviewer's MAJOR stands). Coordinator
experiment: `DropdownMenuLinkSpec` with `.menu.visible` gone after the click: red. Not at the valve (round 6 was
clean): round 8 at level 3. Full suite on 2a31958a: 16489 tests, 0 failures.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 7 impl, dom (sec MINOR) | MAJOR | DropdownMenu.kt:182-186, :194-197 | A link click now bubbles from the menu to the trigger around it, which opens the menu again: it stays open over the next page | fix | Experiment red (above) |
| 7 impl, dom | MINOR | Router.kt:189-192, :314-315 | A link whose own onClick navigates is asked twice when the guard says Stop (the address did not move) | fix | |
| 7 sec | MINOR | Router.kt:188-189 | A hash link to the current address is left to the browser; under a foreign `<base href>` that loads another document past the guards; the comment says the browser does nothing | fix | |
| 7 impl | MINOR | FadingModal.kt:112-119 | Under path routing a link in a modal now navigates in-app; the modal stays open and its trap swallows the first Back | maintainer-decision -> fix | Maintainer 2026-10-30: "Close it": a navigation to another page dismisses an open FadingModal |
| 7 dom | MINOR | StreamSourceImpl.kt:18, streams.kt:16 | `subscriptions` now holds wrapper entries: contains(h) is false, a manual remove(h) unsubscribes nothing | fix (CHANGELOG) | |
| 7 dom | MINOR | sampled.kt:15, combining.astro, CHANGELOG | "two minutes at four values a second" holds only in a visible tab (hidden-tab timer throttling) | fix | |
| 7 impl | MINOR | switchMap.kt:95, StreamCombinator.kt:40, StreamSourceImpl.kt:39 | ownEntry unpinned at three of five sites | fix | |
| 7 impl | NIT | StreamCombinator.kt:74-77, PopupsStage.kt:86, double blank lines | Dangling KDoc, a stale @Suppress | fix | |
| 7 impl | SURPLUS | WakeLock.kt:51 | `api == null \|\| api == undefined`: `== null` covers undefined | fix | |
| 7 impl | SURPLUS | pixijs_addon.kt:33, threejs_addon.kt:241 | Duplicate `create` bodies | reject | Two addons, two modules with no shared dependency below kraft core; a core helper for two callers is not worth the API |
| 7 | withdrawn | various | the 3 s window (sec), keepFresh (sec, own correction), holderCount, TestWindowController focus/resize, restoreUri branch | withdrawn | Phase 2 |

## History

- 2026-10-28: created from Klang's request; checked against the code the same day.
- 2026-10-29: DESIGNED.
- 2026-10-29: the maintainer asked how the router knows a guard is active: by registration from the component's lifecycle; answered in the ticket. "Leaving" narrowed to a change of matched route (a query change from `urlParam` must not ask).
- 2026-10-29: maintainer: leaving = another matched route (pattern or route params); a query-only or fragment change never asks.
- 2026-10-29: implemented and through two review rounds (record in ULTRA-00266); round 2 not clean (one MAJOR on replaceUri keeping history.state): stopped at the safety valve for the maintainer.
- 2026-10-29: IN PROGRESS.
- 2026-10-29: maintainer at the valve: the trap checks the URL (keep the state, the trap clears its marker instead of going back when the address changed); round 3 at level 3; all round-2 MINORs applied in the same batch.
- 2026-10-29: maintainer after round 3: leaving = another path (before ? and #), what RouterComponent remounts for; replaces "another matched route". Both round-3 MAJORs and all MINORs fixed in one batch, then round 4 at level 3.
