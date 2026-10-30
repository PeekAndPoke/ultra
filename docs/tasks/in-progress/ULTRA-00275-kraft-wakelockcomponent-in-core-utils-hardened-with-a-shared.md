# ULTRA-00275: Kraft: WakeLockComponent in core/utils, hardened, with a shared counter for stacked components

> **Status (2026-10-30): IN PROGRESS.** The maintainer, 2026-10-30: move gfx-playground's WakeLockComponent into Kraft core utils, harden it, and let several be mounted at once (a shared counter: the lock is released when it is down to 0). For the next release.
>
> **Part of:** ULTRA-00257 (Epic: Kraft forms and components)

**Security-critical:** no

## Why this exists

The maintainer, 2026-10-30: "Please move this component into Kraft/core/utils. Check the component and harden it. One
additional thing we need to add, is to allow for 'stacking', meaning that multiple of the WakeLockComponent might be
in the dom at the same time, so we need to implement something like a shared Semaphore, that only releases the
wakelock when the counter is down to 0 again. So increase when mounting and decrease on unmount."

Source: `/opt/dev/peekandpoke/gfx-playground/src/jsMain/kotlin/ui/WakeLockComponent.kt` and `wakelock.kt`
(Apache-2.0, the maintainer's), used by gfx-playground's `TreesPage.kt:209`.

Checked, 2026-10-30. What the hardening has to fix:

1. **Its focus listener never goes:** `removeEventListener("focus", ::onFocus)` passes a new function object, so every
   mount leaves a listener on the window (the bug ULTRA-00243 fixed in the router).
2. **Unmount while the request is pending leaks the lock:** the request resolves after the unmount, sets the sentinel
   on a component that is gone, and nothing ever releases it.
3. **Two requests at once:** mount and focus both launch a request while `isWakeLockActive` is still false; the second
   sentinel replaces the first, which is never released.
4. **Unsupported browsers:** `nav.wakeLock` null goes on to call `request` on null, throws, and retries three times.
5. **Focus is the wrong signal:** the browser releases a screen wake lock when the page is hidden; the lock must be
   taken again on `visibilitychange` to visible. Focus changes do not release it, so releasing and requesting on
   every focus is churn.
6. **The local `Navigator` external interface** shadows the DOM type's name in its package.

## Decide before implementing

1. **Shape.** Recommendation: a `WakeLockController` with a shared `WakeLockController.default` (the lock is the
   page's, as `WindowController.default` is), and `acquire(): () -> Unit` as the counter: the first holder requests the
   lock, the last release lets it go; the returned release is idempotent. `isActive: Stream<Boolean>`. The component
   `Tag.WakeLockComponent()` acquires on mount and releases on unmount. It looks the controller up as an app attribute
   and falls back to the default, so a test passes a fake: no change to the app builder.
2. **Name.** Keep `WakeLockComponent` (gfx-playground's), in `io.peekandpoke.kraft.utils`. Recommendation: yes.
3. **Retries.** Recommendation: after a failed request (NotAllowedError when the page is not visible, or no
   permission) up to three retries a second apart while someone holds it and the page is visible; otherwise the next
   visibilitychange to visible tries again.

## The work

1. `kraft/core/src/jsMain/kotlin/utils/WakeLock.kt`: the externals (`WakeLockSentinel`; navigator's `wakeLock` read
   dynamically), `WakeLockController` with the counter, one request in flight at most, release when the count is 0
   (also when a pending request resolves after that), re-request on visibility, `isActive`.
2. `Tag.WakeLockComponent()` (renders nothing visible), acquiring on mount, releasing on unmount.
3. Specs (kraft/core-tests, with a fake lock API): two components mounted, one unmounted: still held; both: released;
   unmount while the request is pending: released when it resolves; one request for two mounts at once; hidden → the
   browser releases → visible: requested again; unsupported: no request, no error; no window listener left.
4. Docs: the Kraft utilities page and its LLM mirror; CHANGELOG.
5. Tell gfx-playground to use Kraft's (after the release).

## Acceptance

- Stacked components hold one lock until the last unmounts; every spec row red against the old component's behaviour.

## History

- 2026-10-30: created on the maintainer's request; the original checked the same day.
- 2026-10-30: IN PROGRESS.
- 2026-10-30: maintainer: "Build it like that" (controller with a shared default, acquire() as the counter, isActive, the component).
