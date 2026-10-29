# ULTRA-00268: Kraft WindowController: one shared default instance, reachable outside components, and steerable in tests

> **Status (2026-10-29): DESIGNED.** Plan written 2026-10-29, waiting for the maintainer's choices (Decide section). From the maintainer's question (2026-10-29) whether WindowController should be a singleton, prompted by Klang's resting-while-unfocused work (KLANG-00389). Not scheduled.
>
> **Part of:** ULTRA-00257 (Epic: Kraft forms and components)

**Security-critical:** no

## Why this exists

The maintainer, 2026-10-29: "Should WindowController be a singleton (object) instead of a class?" It came up with
Klang's wish that all UI work rest while the window has no focus (KLANG-00389), where `hasFocus` is the signal.
Checked against the code the same day:

- **Listeners pile up.** `WindowController`'s `init` adds four listeners (`visibilitychange`, `focus`, `blur`,
  `resize`; `kraft/core/src/jsMain/kotlin/utils/WindowController.kt:84-87`) and nothing removes them.
  `KraftApp.Builder` creates a new one per app (`kraft/core/src/jsMain/kotlin/index-jsMain.kt:75`), and `TestBed`
  creates an app per mount (`kraft/testing/src/jsMain/kotlin/TestBed.kt:101`): every mount in a spec run leaves four
  listeners behind, and each focus or resize event runs all earlier controllers.
- **Only reachable from components.** `Component<*>.windowCtrl` looks it up through the component tree
  (`WindowController.kt:13`), so code outside a component (Klang's `Frames.ui = all.cutoffWhen(UiWork.resting)`)
  cannot read `hasFocus`.
- **Not steerable in tests.** The class is final and reads the real `document`; a spec cannot set `hasFocus`.
- **The state is global.** Everything it tracks is the browser's, so two instances can never disagree: one instance is
  the truthful model.
- **`hasFocus` has no spec;** `ResizeHooksSpec` covers only the focus/blur callbacks.

## Decide before implementing

1. **`object` or a shared default.** Recommendation: a shared default, `WindowController.default` (created lazily),
   used by `KraftApp.Builder`, keeping the class and the `windowCtrl(...)` override, as `I18nController.default()`
   does. A plain `object` could not be replaced per app or in a test.
2. **Steering in tests.** An interface with the browser-backed default and a test controller whose `hasFocus` a spec
   sets, or an internal `emit` hook. Recommendation: the interface, so a fake needs no DOM events.
3. **Hidden vs. unfocused.** `hasFocus` merges a hidden tab and a visible window behind another app. Recommendation:
   add `isVisible: Stream<Boolean>` from `document.visibilityState`, keep `hasFocus` as it is.

## The work

1. `WindowController.default`, used by the builder; the class behind an interface; `isVisible`.
2. Specs: N TestBed mounts add the listeners once; `hasFocus` follows focus/blur/visibilitychange; a test controller
   drives a component's pause.
3. KDoc and the Kraft docs page on app-level controllers; LLM mirror.

## Acceptance

- Mounting 20 TestBeds and firing one `focus` runs the controller's handler once (RED today).
- `WindowController.default.hasFocus` is readable outside any component.

## History

- 2026-10-29: created on the maintainer's word; checked against the code the same day.
- 2026-10-29: DESIGNED.
- 2026-10-29: maintainer: option (a), a shared default `WindowController.default`; the interface for test fakes and `isVisible` as planned.
