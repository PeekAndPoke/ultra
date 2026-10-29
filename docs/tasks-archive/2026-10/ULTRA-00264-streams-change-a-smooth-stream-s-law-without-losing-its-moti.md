# ULTRA-00264: Streams: change a smooth() stream's law without losing its motion

> **Status (2026-10-29): DONE.** Done 2026-10-29: gate PASS in round 2 (ULTRA-00264 Review record). Request from Klang (Klang-Research, 2026-10-28), from its start-page lamp (klang 04af853dd). Not scheduled; Klang works with a local SwappableSmoothing. No epic fits: ULTRA-00255 (Ultra libraries) holds scans and defects, not wishes.

**Security-critical:** no

## Why this exists

Klang-Research, 2026-10-28: klang's start-page lamp (klang `04af853dd`, `src/jsMain/kotlin/comp/MotorBackground.kt`,
`BulbStutter.kt`) sends a dying bulb's brightness through `.smooth(smoothing)`, and each stutter picks its own law at
random (Exponential, AttackRelease, Spring, Linear). A new `.smooth()` per law starts the motion over (its first value
is taken as it is, position and velocity are lost), as the maintainer pointed out. Klang built, locally:

```kotlin
class SwappableSmoothing(var law: Smoothing) : Smoothing {
    override fun step(motion: Smoothing.Motion, goal: Double, elapsedMs: Double) = law.step(motion, goal, elapsedMs)
    override fun isSettled(motion: Smoothing.Motion, goal: Double, near: Double) = law.isSettled(motion, goal, near)
}
```

Checked against the code, 2026-10-28:

- **Klang's class is correct against today's operator:** `StreamSmoothOperator.frame` reads the law on every frame
  (`ultra/streams/src/commonMain/kotlin/ops/smooth.kt:160-163`) and keeps `Motion` (position and velocity) outside the
  law, so a swap takes effect on the next step from the current motion.
- **What a swap does not do:** wake a settled stream. With the goal reached and no frame pending, a new law does
  nothing until the next value, which is right: there is nothing to move.
- Both laws must agree on what `Motion.velocity` means: all four keep it in units per second (`Smoothing.kt`), so
  a Spring taking over from an Exponential swings on with the speed it had.

## Decide before implementing

1. **Which API.**
   - (a) `Smoothing.Swappable(initial)` in `ultra.maths`: Klang's class, five lines, mutable.
   - (b) `Stream<Double?>.smooth(smoothing: Stream<Smoothing?>)`: the law is a stream; a new law takes the next step
     from the current motion, `null` passes values at once (as `sharePer10Ms <= 0` does now).
   - Recommendation: (b). It fits how streams are composed (a law can be derived with `map` from a setting), keeps
     mutable state out of `maths`, and the operator already owns the lifecycle a second subscription needs. (a) stays
     a one-line user-side workaround.
2. **Under (b): a law change while moving.** Recommendation: no reset of `lastFrameAt`, the next frame steps with the
   new law over the real elapsed time.

## The work

1. The overload in `ultra/streams/src/commonMain/kotlin/ops/smooth.kt`: subscribe to the law stream with the source,
   unsubscribe together; the existing overloads delegate to it with a constant law.
2. Specs (commonTest, `HandClock`): a swap mid-motion keeps position and velocity (Spring after Exponential swings
   on), a swap to `null` lands at once, a swap while settled emits nothing, unsubscribing releases both sources.
3. Docs: the streams operators page, `maths/smoothing`, and the LLM mirrors; CHANGELOG.

## Acceptance

- The swap-keeps-motion spec is RED against an operator that restarts the motion on a new law.
- All three platforms green.

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
- 2026-10-28: the maintainer, in the Klang-Research session (relayed by Klang-Research, not yet confirmed in ultra's
  session): "The streams should be like patterns ... when there is something to be configured on a stream the config
  value itself needs to be a stream ..." ("like patterns": sprudel, where every argument of a pattern function is a
  pattern). It backs option (b), and reads wider: `rest` and `near` as streams too, and the other operators' config.
- 2026-10-29: DESIGNED.
- 2026-10-29: maintainer: "config is a stream" is a rule, with stream overloads beside the constant ones (answer "Rule + stream overloads"); option (b) decided.
- 2026-10-29: gate PASS in round 2 (record in ULTRA-00264); open MINORs handed to the maintainer as a list. Docs written with the change, so no DOCS follow-up.
- 2026-10-29: DONE.
