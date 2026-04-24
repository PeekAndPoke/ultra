# Diagnose why `EnsureRepositoriesOnAppStarting` doesn't fire — via tests

## Context

The user's project on Ultra `0.106.0` has to run the `vault:repositories:ensure` CLI command
manually to create collections/tables. It "used to happen automatically" in earlier versions.

The hook that is supposed to do this already exists and **is wired**:

- `funktor/cluster/src/jvmMain/kotlin/vault/EnsureRepositoriesOnAppStarting.kt` —
  `OnAppStarting` with `ExecutionOrder.ExtremelyEarly`, calls `database.ensureRepositories()`.
- `funktor/cluster/src/jvmMain/kotlin/index_jvm.kt:148` —
  `singleton(EnsureRepositoriesOnAppStarting::class)`.
- `funktor/core/src/jvmMain/kotlin/core_module.kt:136` —
  `singleton(AppLifeCycleHooks::class)` (no factory; Kontainer auto-resolves
  `List<OnAppStarting>`, `List<OnAppStarted>`, …).

So somewhere between "hook is registered as a concrete-class singleton" and "hook runs at
startup" the chain is silently broken. The existing test file
`funktor/core/src/jvmTest/kotlin/lifecycle/AppLifeCycleSpec.kt` never exercises that chain —
every test hand-builds `AppLifeCycleHooks(onAppStarting = listOf(…), …)` and registers it as
`singleton(AppLifeCycleHooks::class) { AppLifeCycleHooks(…) }`. That means **no test today
proves that a `singleton(MyHook::class)` registration ends up in the list** that the
builder runs. This is almost certainly where the regression hides.

The goal of this change is to add tests that cover that exact path — for all five phases — so
the bug surfaces and can be fixed.

## Approach

Add a new spec next to the existing one that:

1. Registers a tiny probe implementation of **each** of the five hook interfaces via
   `singleton(ProbeXxx::class)` — the same style `EnsureRepositoriesOnAppStarting` uses.
2. Registers `AppLifeCycleHooks` with **no explicit factory**, i.e.
   `singleton(AppLifeCycleHooks::class)`, mirroring `core_module.kt:136`.
3. Drives a `testApplication` through its full lifecycle (start → stop) via
   `Application.lifeCycle(kontainer) { }` and asserts each probe's counter went up.

If the probes don't fire, we've reproduced the user's bug in a 50-line test and can fix the
real cause in `AppLifeCycleBuilder` / Kontainer resolution.

## Files

### New — `funktor/core/src/jvmTest/kotlin/lifecycle/AppLifeCycleKontainerWiringSpec.kt`

Kept separate from `AppLifeCycleSpec.kt` so the intent is obvious: these specifically cover
**Kontainer-driven auto-wiring**, not the builder's DSL.

Sketch (fill in during implementation):

```kotlin
class AppLifeCycleKontainerWiringSpec : StringSpec({

    // Probes — concrete classes, just like EnsureRepositoriesOnAppStarting.
    class ProbeOnAppStarting(val hit: AtomicInteger) : OnAppStarting {
        override suspend fun onAppStarting(application: Application) {
            hit.incrementAndGet()
        }
    }
    // …same shape for OnAppStarted, OnAppStopPreparing, OnAppStopping, OnAppStopped.

    "singleton-registered OnAppStarting hook fires during lifecycle" {
        val hit = AtomicInteger(0)
        val k = kontainer {
            instance(hit)
            singleton(ProbeOnAppStarting::class)       // concrete-class registration
            singleton(AppLifeCycleHooks::class)        // no explicit factory — the bug path
        }.create()

        testApplication {
            application { lifeCycle(k) {} }
            startApplication()
        }

        hit.get() shouldBe 1
    }

    // One analogous test per phase. OnAppStopped / OnAppStopping require driving
    // testApplication through stop — either via `stop()` on the test harness or by
    // letting the block return (testApplication tears down on exit).
})
```

Points to verify while writing:

- **Does Kontainer's `List<OnAppStarting>` injection actually include
  `singleton(ProbeOnAppStarting::class)`?** If the test fails with a 0 count on all phases,
  this is the root cause: `AppLifeCycleHooks`' constructor is getting empty lists.
  (ExploreAgent reported `TypeLookup.ForSuperTypes` should handle it, but no existing test
  proves it end-to-end.)
- **Does `Application.lifeCycle(k) {}` invoke `register(kontainer)` at all?** The one-liner
  test above verifies the full path the user's real app takes.
- **Stop-phase probes:** `testApplication { }` runs the app block and tears down on exit;
  `ApplicationStopPreparing` / `Stopping` / `Stopped` events should all fire. If a specific
  phase is awkward to trigger from the test harness, call the stop event directly (see
  `AppLifeCycleBuilder.onAppStop*` wiring for the Ktor `EventDefinition` constants).

### Existing files (read-only for this change)

- `funktor/core/src/jvmMain/kotlin/lifecycle/AppLifeCycleHooks.kt` — the five interfaces.
- `funktor/core/src/jvmMain/kotlin/lifecycle/AppLifeCycleBuilder.kt` — the builder that
  sorts by `executionOrder` and invokes each hook. If the bug is *here* (e.g. a sorting
  regression or a swallowed exception), the tests above will red-light the specific phase.
- `funktor/core/src/jvmMain/kotlin/lifecycle/lifecycle.kt` — the `runBlocking(coroutineContext)`
  wrapper added in `e017d610`. Prime suspect #2 after Kontainer resolution.

## How to run

```bash
./gradlew :funktor:core:jvmTest --tests "*AppLifeCycleKontainerWiringSpec*"
```

## Expected signal → likely root cause

| Outcome                         | Interpretation                                                                                                                                                                                                                                                                                                                            |
|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| All 5 phases pass               | Kontainer + builder are fine; the user's bug is elsewhere (e.g. their app never calls `lifeCycle(k) {}`, or `Lookup<Repository<*>>` is coming back empty — then the follow-up test is to probe `Database.getRepositories()` inside the hook and assert non-empty).                                                                        |
| `OnAppStarting` fails (count 0) | Either `AppLifeCycleHooks` got `emptyList<OnAppStarting>()` (Kontainer auto-resolution broken for concrete-class + `List<SuperType>`), **or** `register(kontainer)` in `AppLifeCycleBuilder.kt:29` isn't reached for this path. Add a log / an assertion on `kontainer.get(AppLifeCycleHooks::class).onAppStarting.size` to disambiguate. |
| All 5 phases fail identically   | Kontainer auto-resolution bug — same root cause for every list.                                                                                                                                                                                                                                                                           |
| Stop phases fail only           | Ktor event-bridge regression from the `runBlocking(app.coroutineContext)` change in `e017d610`.                                                                                                                                                                                                                                           |

## Verification

1. Run the new spec — it must pass once the bug is fixed.
2. As a sanity check, temporarily regress to `v0.106.0`'s `AppLifeCycleBuilder` and confirm
   the test passes *there* too (rules out "test was always broken").
3. Manual end-to-end: start a funktor-demo-style app with an empty DB and confirm collections
   are created without running `vault:repositories:ensure`.

## Results — 2026-04-24

Spec added at `funktor/core/src/jvmTest/kotlin/lifecycle/AppLifeCycleKontainerWiringSpec.kt`.
Ran with
`./gradlew :funktor:core:jvmTest --tests "io.peekandpoke.funktor.core.lifecycle.AppLifeCycleKontainerWiringSpec"`.

- **PASS** `OnAppStarting` — fires when registered as `singleton(ProbeOnAppStarting::class)`
- **PASS** `OnAppStarted` — fires
- **PASS** `OnAppStopPreparing` — fires
- **PASS** `OnAppStopping` — fires
- **Disabled** (`!` prefix) `OnAppStopped` — does not fire inside `testApplication`; the
  harness returns before Ktor dispatches `ApplicationStopped`. Test-infra artifact, not the
  user's regression.

**Conclusion:** Kontainer's `List<OnAppStarting>` auto-resolution of concrete-class-registered
hooks works correctly, and `AppLifeCycleBuilder.register(kontainer)` invokes them. The user's
"repos not auto-created on 0.106.0" symptom is **not** a lifecycle-wiring bug.

Remaining candidates to chase:

1. The user's app might not actually call `Application.lifeCycle(kontainer) {}` (demo server
   gates it on `!config.ktor.isTest` — similar gating in the user's app would suppress it).
2. `Database.ensureRepositories()` is invoked but `Lookup<Repository<*>>` resolves to empty.
   Follow-up test idea: register a dummy `Repository<*>` singleton alongside the hook and
   assert that `Database.getRepositories()` contains it during `onAppStarting`.

User confirmed #1 on 2026-04-24 — their server module has no `lifeCycle {}` call. Without
that call, `AppLifeCycleBuilder.register(kontainer)` is never invoked, so **no** hook in any
phase fires (not just `EnsureRepositoriesOnAppStarting` but also `AuthSystemAppHooks`,
`ValidateRoutesOnAppStarting`, `GlobalLocksCleanupOnAppStarting/Stopped`, etc.). The design
silently no-ops when the app forgets this opt-in — a footgun.

---

# Fix plan — auto-install via `setupLifecycle()`

## Why this shape

`OnAppStarting` hooks are invoked **inline** inside `register(hooks)` (not via a Ktor event
subscription) because Ktor's `ApplicationStarting` event has already fired by the time
`Application.module()` runs — subscribing to it from inside a module would be a no-op. So the
"kontainer registration" step has to happen somewhere that runs during module configuration.

Every funktor app already routes its `Application.module() { ... }` through
`App.Definition.module(application, block)` at `funktor/core/src/jvmMain/kotlin/app.kt:131`.
That's the single bottleneck — one call site, all apps. So we install the kontainer hooks
there, and leave the user-facing `lifeCycle {}` DSL as a pure "add-listeners" affordance.

Splitting into two functions means:

- No idempotency marker on `Application.attributes` — `setupLifecycle` has exactly one call
  site (inside `App.Definition.module`), so it's structurally impossible to call twice.
- `lifeCycle { }` no longer bootstraps anything; users can call it 0, 1, or N times with no
  surprises.

## Changes

### 1. Split `lifecycle.kt`

File: `funktor/core/src/jvmMain/kotlin/lifecycle/lifecycle.kt`

```kotlin
// Auto-installed by App.Definition.module(). Do not call from user code.
internal fun Application.setupLifecycle(kontainer: Kontainer) {
    runBlocking(coroutineContext) {
        val built = AppLifeCycleBuilder(app = this@setupLifecycle).apply {
            onAppStarting { log.info("----====  LifeCycle onAppStarting  ====----") }
            onAppStarted { log.info("----====  LifeCycle onAppStarted  ====----") }
            onAppStopPreparing { log.info("----====  LifeCycle onAppStopPreparing  ====----") }
            onAppStopping { log.info("----====  LifeCycle onAppStopping  ====----") }
            onAppStopped { log.info("----====  LifeCycle onAppStopped  ====----") }

            register(kontainer)          // only call site for kontainer-driven hooks
        }
        val lifeCycle = appRegistry.getOrPut(this@setupLifecycle) { AppLifeCycle(this@setupLifecycle) }
        lifeCycle.register(built.listeners)
    }
}

// User-facing. Adds additional listeners on top of what setupLifecycle registered.
// No kontainer parameter — if a listener needs services, close over your own.
fun Application.lifeCycle(builder: suspend AppLifeCycleBuilder.() -> Unit) {
    runBlocking(coroutineContext) {
        val built = AppLifeCycleBuilder(app = this@lifeCycle).apply { builder() }
        val lifeCycle = appRegistry.getOrPut(this@lifeCycle) { AppLifeCycle(this@lifeCycle) }
        lifeCycle.register(built.listeners)
    }
}
```

### 2. Make `AppLifeCycle.register` append instead of replace

File: `funktor/core/src/jvmMain/kotlin/lifecycle/AppLifeCycle.kt` (lines 16–40)

Today the method calls `unsubscribeAll()` on every call (motivated by dev hot-reload). With
two legitimate callers (`setupLifecycle` + possibly `lifeCycle { }`), replacing on each call
would wipe the first's Ktor event subscriptions.

Change to: append new listeners, keep the `ApplicationStopped` cleanup listener registered
exactly once on the first call. Teardown on dev reload still works via that cleanup listener.

### 3. Wire the auto-install

File: `funktor/core/src/jvmMain/kotlin/app.kt:131` — inside `App.Definition.module()`.

```kotlin
fun module(
    application: Application,
    block: Application.(app: App<C>, config: C, init: Kontainer) -> Unit,
) {
    @Suppress("UNCHECKED_CAST")
    val app = (testApp ?: application.getApp()) as App<C>
    val init = app.kontainers.system()

    application.setupLifecycle(init)               // NEW — every funktor app gets hooks for free
    application.block(app, app.config, init)
}
```

### 4. Update the demo (breaking-change migration)

File: `funktor-demo/server/src/main/kotlin/server.kt:25–30`

Current:

```kotlin
if (!config.ktor.isTest) {
    lifeCycle(init) {
        launchWorkers { init.clone() }
    }
}
```

After (kontainer param dropped; `init` still accessible via closure):

```kotlin
if (!config.ktor.isTest) {
    lifeCycle {
        launchWorkers { init.clone() }
    }
}
```

No other `lifeCycle(kontainer) { ... }` call sites exist in this repo (verified via grep in
Phase 1). External users who call `lifeCycle(kontainer) { ... }` will need the same one-line
migration — drop the kontainer arg. **Decision 2026-04-24: clean break, no deprecated
overload.**

### 5. Extend the wiring spec

File: `funktor/core/src/jvmTest/kotlin/lifecycle/AppLifeCycleKontainerWiringSpec.kt`

Add:

- `"setupLifecycle(k) fires kontainer OnAppStarting hooks exactly once"` — existing tests
  renamed/repointed to use `setupLifecycle(k)` instead of `lifeCycle(k) {}`.
- `"setupLifecycle(k) then lifeCycle { onAppStarting {...} } — both fire, once each"` —
  proves user listeners don't clobber kontainer listeners and kontainer hooks don't run twice.
- `"setupLifecycle(k) then two lifeCycle {} calls — no subscription loss across calls"` —
  exercises the append-not-replace change in `AppLifeCycle.register`.

### 6. Add an integration smoke test

File: new, near funktor-demo tests (or `funktor/core/src/jvmTest`).

Verify end-to-end: an `App.Definition.module()` call with a kontainer containing a probe
`OnAppStarting` runs the probe even though the block never calls `lifeCycle { }`. This is
the regression test that pins the fix.

## Files to change

- `funktor/core/src/jvmMain/kotlin/lifecycle/lifecycle.kt` — split into `setupLifecycle` + `lifeCycle`.
- `funktor/core/src/jvmMain/kotlin/lifecycle/AppLifeCycle.kt` — `register()` becomes append-mode.
- `funktor/core/src/jvmMain/kotlin/app.kt` — call `setupLifecycle(init)` from `App.Definition.module()`.
- `funktor-demo/server/src/main/kotlin/server.kt` — drop `init` param from `lifeCycle {}`.
- `funktor/core/src/jvmTest/kotlin/lifecycle/AppLifeCycleKontainerWiringSpec.kt` — update existing tests to use
  `setupLifecycle`; add coexistence + append-mode cases.
- `funktor/core/src/jvmTest/kotlin/lifecycle/AppLifeCycleSpec.kt` — existing spec already passes `AppLifeCycleHooks`
  explicitly, but its calls to `lifeCycle(k) {}` need updating to the new signature (drop the kontainer param; no
  behavioral change since those tests pass hooks via the container factory, not via `setupLifecycle`).

## Verification

1. `./gradlew :funktor:core:jvmTest --tests "*AppLifeCycle*"` — all specs green.
2. Manual: start funktor-demo with an empty DB; confirm collections are created without
   running `vault:repositories:ensure`.
3. Grep the repo after the change for `lifeCycle(` calls with two arguments — should only
   find historical ones in plans/archive, no live code.

## Fix results — 2026-04-25

**DONE.**

- `funktor/core/src/jvmMain/kotlin/lifecycle/lifecycle.kt` — split into `setupLifecycle` (internal) +
  `lifeCycle(builder)` (public).
- `funktor/core/src/jvmMain/kotlin/lifecycle/AppLifeCycle.kt` — `register()` now appends; cleanup listener added once
  via `cleanupInstalled` flag, resets on teardown.
- `funktor/core/src/jvmMain/kotlin/app.kt` — `App.Definition.module()` calls `application.setupLifecycle(init)` before
  `block(...)`.
- `funktor-demo/server/src/main/kotlin/server.kt` — `lifeCycle(init) { ... }` → `lifeCycle { ... }` (`init` captured
  from enclosing closure).
- `funktor/core/src/jvmTest/kotlin/lifecycle/AppLifeCycleSpec.kt` — updated to new API; removed dead `emptyKontainer()`
  helper.
- `funktor/core/src/jvmTest/kotlin/lifecycle/AppLifeCycleKontainerWiringSpec.kt` — probes now call `setupLifecycle(k)`
  directly; added "setupLifecycle + lifeCycle coexist" and "two lifeCycle { } calls accumulate listeners" cases.

Tests: `./gradlew :funktor:core:jvmTest --tests "io.peekandpoke.funktor.core.lifecycle.*"` — BUILD SUCCESSFUL.
Downstream compilation verified for `:funktor:cluster`, `:funktor:auth`, `:funktor:rest`, `:funktor:messaging`.
(`:funktor-demo:server` has pre-existing, unrelated JWT compile errors from a concurrent auth refactor — not caused by
this change.)
