# ULTRA-00233: Kraft modals: every modal answers exactly once (Handle<RESULT>)

> **Status (2026-10-24): IN PROGRESS.** Plan confirmed by the maintainer on 2026-10-24 (decisions below). One design review round done (record below).

**Security-critical:** no

## Why this exists

Klang's look review (KLANG-00323, against Kraft 0.111.0) found defects in the Semantic UI modals. Klang works around
them in its own YesNoModal (klang `a5ca09c5`) and will drop the workaround once Kraft ships the fix. Spross asked
what OkCancel answers when the modal is dismissed. All of the following hold in master (verified 2026-10-21 and
again by the review on 2026-10-23):

1. **Buttons are divs.** `OkCancelModal` renders `ui.negative.button { }` / `ui.positive.button { }`
   (`kraft/semanticui/src/jsMain/kotlin/modals/OkCancelModal.kt:177,185`), and `SemanticTag.invoke` is a DIV
   (`ultra/semanticui/src/commonMain/kotlin/SemanticTag.kt:47`). No focus, no Enter, no focus ring.
   `SemanticTag.Button` (`:65`) renders a real `<button>`, but without a `type`.
2. **Two results from one modal.** `FadingModal.close` ignores repeat calls once fading
   (`kraft/semanticui/src/jsMain/kotlin/modals/FadingModal.kt:114`), but Cancel calls `onResult(Cancel)` itself
   after `close()` (`OkCancelModal.kt:180-181`). Ok then Cancel within the fade answers Ok (after 150 ms) AND Cancel.
3. **`closeAll()` runs no `onClose` handlers** (`kraft/core/src/jsMain/kotlin/modals/ModalsManager.kt:100-102`),
   unlike `close()` (`:87-97`). A modal waiting for its close never gets an answer.
4. **Dismissing gives no answer.** A backdrop click (`FadingModal.kt:93-96`) or back navigation (`:63-67`) calls
   `close()` without a result, so `OkCancelModal.onResult` never fires.

Found by the review, also in master:

5. **The modal stage renders without keys** (`kraft/core/src/jsMain/kotlin/modals/ModalsStage.kt:37-39`), unlike
   popups (`popups/PopupsStage.kt:71`). When a modal closes and another of the same class takes its place in the
   same render, Preact reuses the old component instance with the new props: its state (`fadingOut = true`) stays,
   `init` and `onMount` do not run again, and the new modal is stuck, invisible, and never answers.
6. **The renderer runs on every stage render** (`ModalsStage.kt:35-40`), so anything a renderer registers piles up.
   `funktor/inspect/src/jsMain/kotlin/logging/LogsBulkActionPopup.kt:28-29` registers `handle.onClose { }` inside
   `modals.show { }`, so its callback runs once per render that happened while it was open.
7. **An unmount without a fade leaves `<body>` unscrollable.** Only `fadeOut()` removes `Style.noScroll`
   (`FadingModal.kt:131`); there is no `onUnmount`. Today `closeAll()` does exactly that.

All of these come from one missing guarantee: a modal has no contract to answer. The handle is the only place that
sees every way a modal goes away (a button, the backdrop, back navigation, `closeAll()`, a caller's
`handle.close()`, a modal that does not extend `FadingModal`), so the contract lives there.

## Decided (maintainer, 2026-10-23: "all 4 seem to be correct")

1. **The contract is in kraft core's handle**, not in a semanticui base class.
2. **OkCancel's `onResult` becomes `(Result?) -> Unit`**: `null` means dismissed. One contract everywhere, no third
   enum value. The compiler marks most callers (`ifOk`, `handle`, an exhaustive `when`), but NOT these, which
   compile unchanged and now send a dismissal down the destructive branch: `if (result == Cancel) … else delete()`,
   `when (result) { Cancel -> …; else -> delete() }`, and string templates. CHANGELOG and the migration note say so.
3. **`showAndAwait` ships in this change.**
4. **The ride-alongs ship too:** real `<button type="button">`s, and focus on Cancel when the modal opens (an
   OkCancelModal often confirms a destructive action, so Enter must not confirm by accident).

## The design

```kotlin
// kraft/core/src/jsMain/kotlin/modals/ModalsManager.kt
class Handle<RESULT : Any> internal constructor(...) {
    /** The first call decides the answer; later calls, and calls after close() began, are ignored. Does not close. */
    fun resolve(result: RESULT)

    /** Called once when the modal is gone: the decided answer, or null if it was dismissed. */
    fun onResult(block: (RESULT?) -> Unit): Handle<RESULT>

    /** Called once when the modal is gone, after the onResult handlers. Now also on closeAll(). */
    fun onClose(block: () -> Unit): Handle<RESULT>

    /** Removes the modal. The answer is what resolve() decided, else null. */
    fun close()

    internal fun render(div: DIV) = view(div, this)
}

interface ModalsManager : Stream<List<Handle<*>>> {
    fun <RESULT : Any> show(view: ModalRenderer<RESULT>): Handle<RESULT>
    fun close(handle: Handle<*>)
    fun closeAll()
}

// kraft/core/src/jsMain/kotlin/modals/modals.kt
/** Renders a modal. Runs on EVERY render of the modal stage: never register handlers in it. */
typealias ModalRenderer<RESULT> = DIV.(ModalsManager.Handle<RESULT>) -> Unit

/**
 * Shows the modal and suspends until it is gone. Cancelling the coroutine closes the modal; the answer is then
 * what resolve() decided, else null. Kraft's `launch` scope is global, so a caller's component unmounting does not
 * cancel it.
 */
suspend fun <RESULT : Any> ModalsManager.showAndAwait(view: ModalRenderer<RESULT>): RESULT?
```

- **Exactly once, by removal.** `close()` and `closeAll()` take the handles off the stack first and deliver only to
  the handles they removed. A second `close()`, a `closeAll()` after a `close()`, or a `close()` called from inside
  the handle's own `onResult` finds nothing to remove and delivers nothing. There is no separate `delivered` flag;
  the removal is the mechanism, and the mutation checks target it.
- **Order.** Per handle: all `onResult` handlers, then all `onClose` handlers. `closeAll()` delivers in stack order
  (oldest first), on a snapshot, so a `closeAll()` or `show` called from a handler acts on the stack as it is then,
  and a follow-up modal opened from a handler stays open.
- **A throwing handler does not cost the others their answer.** Each handler runs in `runCatching`, failures go to
  `console.error`, and delivery continues: with the handle's other handlers, and with the other handles of a
  `closeAll()`.
- **`RESULT : Any`**, so `null` can only mean "dismissed". Handles where nothing is answered use `RESULT = Unit`:
  `Unit` means done, `null` means dismissed. Call sites whose renderer does not pin the type write `show<Unit> { }`.
- **The stage keys every modal** by handle id: `ModalsStage` wraps each in `div { key = "modal-${it.id}" }`, as
  `PopupsStage` does. Kraft passes every tag attribute to Preact's `h()` as a prop
  (`kraft/core/src/jsMain/kotlin/vdom/preact/PreactElements.kt:102`), so the key reaches Preact and a new handle
  always gets a new component instance.
- **Handlers are registered once, never in a renderer.** A component wires `props.handle.onResult` in its `init`,
  reading `props` at delivery time; callers register on the handle that `show` returns. The docs state the rule.
- **`FadingModal<P : FadingModal.Props<RESULT>, RESULT : Any>`** with `Props<RESULT>.handle: Handle<RESULT>`:
  - `close(result: RESULT)`: if already fading out, return; else `handle.resolve(result)`, then fade.
  - `close()`: a dismissal. It fixes the answer at "none": the handle's internal `closing` state makes every later
    `resolve` a no-op, so a dismissal followed by a stray click on Ok during the fade answers `null`, not Ok. The
    backdrop and back navigation go through `close()`.
  - The answer arrives after the fade, when `FadingModal` calls `handle.close()`. Deliberate, and Klang's model
    (`klang/klangui/src/jsMain/kotlin/comp/YesNoModal.kt:110-128`): a second Enter during the fade stays inside the
    modal and does not reach the page.
  - `onUnmount` removes `Style.noScroll` from `<body>`, through a count of open FadingModals so the class goes only
    when the last one goes (today the first of two stacked modals to fade clears it). Covers `closeAll()`, a
    cancelled `showAndAwait` and a direct `handle.close()`.
  - `close(onClose: suspend () -> Unit)` with its 150 ms delay: `@Deprecated` for one release, pointing to
    `close(result)` plus `onResult`. Kotlin resolves a bare `close()` to the new no-argument overload, so the
    backdrop and back navigation would stop reaching subclasses that override the deprecated one (Spross's
    `PictureGallery.kt:85-89`, Klang's `YesNoModal.kt:110`). For the deprecation release, `close()` and
    `close(result)` therefore funnel through the deprecated `close(onClose = null)` (`@Suppress("DEPRECATION")`), so
    those overrides keep running. The migration note tells Klang and Spross to move to `close()` / `close(result)`.
- **`OkCancelModal`** becomes a `FadingModal<OkCancelModal.Props, OkCancelModal.Result>` (qualified: a class's
  nested types are not in scope in its own supertype list, and a bare `Result` is `kotlin.Result`). Ok calls
  `close(Result.Ok)`, Cancel calls `close(Result.Cancel)`, and `init` wires `props.handle.onResult { props.onResult(it) }`.
  Both buttons render as `SemanticTag.Button` with `type = ButtonType.button` set explicitly; Cancel gets the focus
  in `onMount`.
- **Esc is not part of this change.** No Kraft modal handles Esc today (Klang's subclass does). It is ULTRA-00239,
  through a reusable key-binding mechanism.

**Breaking, and the compiler finds the type changes:** every `ModalsManager.Handle` becomes `Handle<RESULT>`, every
`FadingModal<P>` becomes `FadingModal<P, RESULT>`, and `ModalRenderer` gains a type parameter. Sites:

- kraft core: `ModalsManager.kt:16,49,52` (stream type, `close`), `modals.kt:6-7` (the typealias), `ModalsStage.kt:31,38`.
- kraft semanticui: `FadingModal.kt`, `OkCancelModal.kt`.
- In-repo users: funktor-demo's `AddMemberModal`, `MemberRolesModal` and `MembersPage.kt:205,225,244`; funktor
  inspect's `LogsBulkActionPopup.kt:28` (also a FadingModal; its renderer-registered `onClose` moves to the returned
  handle); the fomanticui example `ModalDialoguesPage.kt:30`. That is 5 `modals.show` sites.
- Docs: `docs-site/src/pages/ultra/kraft/overlays.astro:32,45,73`, `semantic-ui.astro:229-243`,
  `docs-site/src/data/llms/kraft.md:1855,1962,1975,2007`, and the built example bundles under `docs/kraft/examples/`
  (rebuild).
- Open worktree `../ultra-worktrees/insights-kraft` carries copies of these files: it merges after this or rebases
  onto it.
- Outside (they migrate in their own repos after the release; the note lists these): Klang's `YesNoModal`,
  `CodeToolModal`, `CodeSongPage.kt:204`, `KlangCodeEditorComp.kt:276`, `SprudelMiniNotationEditorTool.kt:207`;
  Spross's `PictureGallery` and `_picture_helpers.kt:44`.

## Decided (maintainer, 2026-10-24: "confirmed")

- `AddMemberModal` / `MemberRolesModal` move their data into `RESULT` ("yes return"); they are the demo of the
  pattern.
- Esc is a follow-up, ULTRA-00239, implemented through a reusable key-binding mechanism (Klang is building
  generalized key bindings), not a listener hard-wired into `FadingModal`.

## The work

0. **Compile spike, one file:** the three call shapes (`mini(handle = handle) { }` as in `MembersPage.kt:244-254`, a
   FadingModal subclass passed `handle`, a plain `div`) against the new signatures, to settle type inference
   before the migration. Adjust the API shape if inference fails.
1. **Core:** `Handle<RESULT : Any>`, `resolve` with the `closing` state, `onResult`, removal-then-delivery with
   `runCatching` per handler, `closeAll()` delivering, `showAndAwait`, the keyed stage.
2. **Semanticui:** `FadingModal<P, RESULT>` with `close(result)` / `close()`, the noScroll count in `onUnmount`,
   the deprecated `close(onClose)` and the funnel.
3. **OkCancelModal:** results through the handle wired in `init`, real buttons with `type="button"`, focus on
   Cancel; `mini` / `tiny` / `small` take `(Result?) -> Unit`.
4. **Migrate** the in-repo sites listed above.
5. **Docs:** the overlays and semantic-ui pages and the LLM mirror, written against the settled code, including the
   rule "never register handlers in a renderer"; rebuild the example bundles; a CHANGELOG entry for the breaking
   change and the silent `else`-branch pattern.
6. **Tell Klang and Spross** (through Mission Control) the version, the migration list, the `close(onClose)`
   deprecation and the silent patterns of Decided 2.

## Testing plan

Two layers: plain unit tests of the manager (no DOM), and browser tests with `TestBed.preact` and KQuery for what
only the DOM shows. The suites run in the existing Karma/Chrome setup (`:kraft:core-tests:jsTest`,
`:kraft:semanticui:jsTest`).

### Layer 1: manager unit tests (`kraft/core-tests/src/jsTest/kotlin/modals/ModalsManagerSpec.kt`)

`DefaultModalsManager` needs no DOM: it only holds a `StreamSource`, and the renderer is never called until a stage
mounts. kotest's JS tests are already `suspend`, and kotlinx.coroutines is on the classpath, so `showAndAwait`
needs no coroutines-test setup: launch it with `CoroutineStart.UNDISPATCHED`, take the handle from `manager()`,
then resolve and close, or cancel the job.

| # | Case | Expected |
|---|---|---|
| 1 | `resolve(A)`, then `close()` | `onResult(A)` once, then `onClose` once |
| 2 | `close()` without `resolve` | `onResult(null)` once |
| 3 | `resolve(A)`, `resolve(B)`, `close()` | `onResult(A)`: the first call decides |
| 4 | `close()` twice; and `close()` then `closeAll()` | handlers run once |
| 5 | three open, `closeAll()` | each handle's `onResult` and `onClose` run once, oldest first; the stack is empty |
| 6 | `close()` called from inside the handle's own `onResult` | no second delivery, no error |
| 7a | inside `onResult` during `close()` | `manager()` no longer contains the handle |
| 7b | an `onResult` handler opens a new modal, during `closeAll()` | the new modal stays open |
| 8 | the first handle's `onResult` throws, during `closeAll()` over two | the second still gets `null`; the first's `onClose` still runs |
| 9 | `showAndAwait`, then `resolve(A)` + `close()` | returns `A` |
| 9a | `showAndAwait` (the stack holds 1), then the job is cancelled | the stack is empty; `onResult(null)` |
| 9b | `showAndAwait`, `resolve(A)`, then the job is cancelled | the modal is closed; `onResult(A)` |

### Layer 2: browser tests (`kraft/semanticui/src/jsTest/kotlin/modals/OkCancelModalSpec.kt`)

`val m = DefaultModalsManager()`, `TestBed.preact(appSetup = { setAttribute(ModalsManager.key, m) }, view = { })`;
the test calls `m.show { h -> mini(handle = h, …) { answers += it } }` directly and records every answer. The stage
is auto-mounted inside the TestBed root, so KQuery reaches the modal. Fades use
`Transition(fadeInTimeMs = 0, fadeOutTimeMs = 100)`. Presence and absence are asserted in the DOM, not as
visibility (the Semantic UI CSS may not be loaded). Every test ends with `m.closeAll()` in a `finally`, so no
modal stays mounted with an active back-navigation trap or the body class (see the friction list).

| # | Case | Expected |
|---|---|---|
| 10 | click Ok | answers stay `[]` right after the click, then `[Ok]`; the modal is gone |
| 11 | click Cancel | answers `[Cancel]` |
| 12 | click Ok, then Cancel, with no await between the clicks | answers `[Ok]`: defect 2. `KQuery.click()` is synchronous, so this needs no timing window |
| 12b | click the backdrop, then Ok, with no await between | answers `[null]` |
| 13 | click the backdrop (the dimmer itself) | answers `[null]` |
| 14 | click inside the modal content | no answer, the modal stays (pins the target check of 13) |
| 15 | `closeAll()` while open | answers `[null]`; the modal is gone; `<body>` has no noScroll class |
| 15b | two modals stacked, the top one closes | `<body>` keeps the noScroll class until the second goes |
| 16 | both buttons | `tagName == "BUTTON"`, `type == "button"` |
| 17 | on open | `document.activeElement` is the Cancel button |
| 18 | back navigation while open: wait until the modal is in the DOM, `window.history.back()`, wait for `popstate` | answers `[null]` |
| 19 | Ok's `onResult` opens a second OkCancelModal | the second is in the DOM, not fading out, has focus, and answers once: defect 5 |
| 20 | open A, open B on top (A's renderer runs again), click Ok on A | A answers exactly `[Ok]`: defect 6 |
| 21 | `showAndAwait` whose coroutine is cancelled while the modal is open | the modal is gone; `<body>` has no noScroll class |

**Mutation checks** (`.claude/skills/review-loop/`; each must turn the named tests RED):

- deliver to the handle passed to `close()` even when it was not on the stack → 4, 6;
- `resolve` becomes last-wins → 3, 12;
- the `closing` state is ignored by `resolve` → 12b;
- handlers run before removal → 7a;
- `closeAll()` replaces the list without delivering → 5, 15;
- no `runCatching` around handlers → 8;
- `close(result)` skips `resolve` → 10;
- the stage key removed → 19;
- `onUnmount` does not remove the body class → 15, 21.

### TestBed and KQuery: what is cumbersome (the maintainer asked for it, to improve the tools)

Checked against the code by the review; confirm each while writing the tests, then move it to the findings table.

- **Keyboard activation cannot be simulated** (confirmed). `keyDown("Enter")` dispatches an untrusted event, and
  browsers do not run a button's default action for it, so "Enter on the focused Cancel answers Cancel" is tested
  through its parts (16, 17). A synthetic key event DOES reach Kraft's own listeners (`kquery_events.kt:29-39`), so
  a key handler such as Esc is testable. A real key press needs trusted input (Chrome DevTools Protocol
  `Input.dispatchKeyEvent`, as Playwright does).
- **`awaitCss` returns an empty query on timeout instead of failing** (`kquery_select.kt:47-68`), and nothing waits
  for an element to disappear.
- **`eventually` exists as four private copies** (`ResizeHooksSpec.kt:31`, `ComponentErrorBoundarySpec.kt:151`,
  `AddonRegistrySpec.kt:28`, `kraft/addons/avatars/src/jsTest/kotlin/AvatarsAddonSpec.kt:23`). One shared helper in
  kraft/testing (or kotest's `io.kotest.assertions.nondeterministic.eventually`) makes "await gone" a one-liner.
- **TestBed never unmounts the app** (`TestBed.kt:113` only removes the div; `VDomEngine` has no unmount), so
  `onUnmount` never runs, and listeners, history entries and body classes leak into later specs. This is why every
  layer-2 test closes its modals in a `finally`.
- **Every FadingModal touches `window.history`** (`BackNavigationTrap.kt:46-52,107-135`), shared by all specs on
  the Karma page; a `back()` with no modal mounted can leave the page.
- **No virtual clock**, but this plan does not need one (12 and 12b are deterministic). Kraft's delays run on its
  private global scope (`kraft/core/src/jsMain/kotlin/utils/async.kt:12`), so `runTest` could not drive them
  without `Dispatchers.setMain`, and kotlinx-coroutines-test is not a kraft test dependency.
- **No focus helper**: `focus()` exists (`kquery_events.kt:102`), `isFocused()` does not; a one-line workaround.
- Refuted: "no way to reach the app's managers". `appSetup` can inject one with `setAttribute`, and
  `ComponentRef.Tracker` with `.track(ref)` (as `UiInputFieldSpec.kt:15-19` does) reaches any component.

### Tool-improvement findings

| date | tool | what was cumbersome | proposal |
|---|---|---|---|

## Acceptance

- [ ] Tests 1-21 green, and the mutation checks above each RED for the named tests.
- [ ] No `ModalsManager.Handle` without a type argument left in this repo; the demo and the fomanticui example
      build; the example bundles under `docs/kraft/examples/` rebuilt.
- [ ] Docs and LLM mirror updated; CHANGELOG notes the breaking change and the silent `else`-branch pattern.
- [ ] Klang and Spross told the version and the migration list, through Mission Control.
- [ ] The TestBed/KQuery friction that held is in the findings table, each with a follow-up ticket or a reason not to.

## Review record

Round 1, 2026-10-23, design review of this ticket (no code yet). Two `reviewer-high` reviewers, blind:
domain/API design (D) and implementation & test plan (I). Security not run: no auth or data boundary. The
coordinator checked the load-bearing claims against the code (keys, noScroll, LogsBulkActionPopup, the global
scope, the call-site count) before applying them.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `ModalsStage.kt:37-39` | (D, I) No keys: a follow-up modal of the same class reuses the closed one's instance and is stuck | fix: keyed wrapper; tests 19 | Confirmed: no key in the stage, `PopupsStage.kt:71` keys. I's "Kraft has no key support" does not hold: attributes go to `h()` as props (`PreactElements.kt:102`) |
| 1 | MAJOR | `ModalsStage.kt:35-40`, `LogsBulkActionPopup.kt:29` | (D, I) Handlers registered in a renderer pile up per render | fix: register once in `init`, rule in docs, migrate LogsBulkActionPopup; test 20 | Confirmed in the code |
| 1 | MAJOR | design, FadingModal bullet | (D) A dismissal does not fix the answer: backdrop, then a stray Ok during the fade answers Ok | fix: `closing` state; test 12b | The design as written allowed it |
| 1 | MAJOR | design, "Removal before delivery" | (D) One throwing handler costs the rest of a `closeAll()` their answer | fix: `runCatching` per handler; test 8 | The design said nothing about it |
| 1 | MAJOR | `FadingModal.kt:131` | (D; I as MINOR) An unmount without a fade leaves `<body>` unscrollable | fix: `onUnmount` with a count; tests 15, 15b, 21 | Confirmed: only `fadeOut()` removes the class |
| 1 | MAJOR | testing plan, mutation checks | (I) The `delivered` guard is never exercised; the stated mutants would survive | fix: removal is the mechanism, `delivered` dropped, mutants rewritten, test 7 split | Correct: removal by id already prevents a second delivery |
| 1 | MAJOR | `FadingModal.kt:106`, Spross `PictureGallery.kt:85-89` | (I; D as MINOR) Overrides of the deprecated `close(onClose)` stop running on dismissal | fix: funnel through the deprecated overload for one release; migration note | Kotlin prefers the overload without defaults |
| 1 | MINOR | `async.kt:12` | (D) `showAndAwait` cancellation rarely happens: the launch scope is global | fix: documented in its KDoc | Confirmed |
| 1 | MINOR | design, cancellation | (I) KDoc said "answer null" while `close()` keeps a resolved answer | fix: resolved answer wins; test 9b | Contradiction in the ticket |
| 1 | MINOR | design signature | (D, I) `Handle<*>` cannot call `view`; inference at `show` sites; `Result` in the supertype is `kotlin.Result` | fix: internal `render`, `show<Unit>`, qualified supertype, step 0 compile spike | |
| 1 | MINOR | Decided 2 | (D, I) "The compiler marks every caller" is not true for `else`-branch patterns | fix: worded in Decided 2, CHANGELOG, migration note | Not a challenge to the decision |
| 1 | MINOR | design | (D, I) Esc named but not built or tested | fix: dropped from this change, open decision | No Esc handling anywhere in kraft |
| 1 | MINOR | ticket text | (D, I) LogsBulkActionPopup IS a FadingModal; 5 `modals.show` sites, not 9; docs sites, bundles and the worktree missing | fix: corrected | Confirmed (`LogsBulkActionPopup.kt:24`, grep) |
| 1 | MINOR | testing plan | (I) Layer-2 tests leak history and listeners, not just 18 | fix: `closeAll()` in a `finally`; friction list | `TestBed.kt:113` never unmounts |
| 1 | MINOR | testing plan | (I) Opener and `modals()` unnecessary; `type` must be set; test 12 needs no window | fix: inject the manager; plan reworded | |
| 1 | SURPLUS | design | (D) Deprecate `onClose` in favour of `onResult` | reject | `onClose` is public API in use; keeping it costs only the order rule, and this change is breaking enough |

CLEAN (probed, held): sequential `close()` / `closeAll()` / double `close()` with removal-then-delivery; the fade
launch calling `handle.close()` after a `closeAll()` (absorbed by removal); back navigation's trap deactivation is
idempotent (`BackNavigationTrap.kt:56-75`); Ok-then-Cancel fixed by first-resolve-wins; answer-after-fade matches
Klang's model; the four original defects match master; the stage mounts inside the TestBed root; `P : Props<RESULT>`
is a valid bound; no `allWarningsAsErrors`, so the deprecation only warns.

## History

- 2026-10-21: created from Klang's KLANG-00323 findings.
- 2026-10-23: redesigned as one contract in the handle (`Handle<RESULT>`, answer `RESULT?`), with the maintainer;
  testing plan added; design review round 1 (7 MAJOR, 9 MINOR, all applied but one surplus suggestion).
- 2026-10-24: plan confirmed; member modals return their data; Esc filed as ULTRA-00239.
- 2026-10-24: IN PROGRESS.
