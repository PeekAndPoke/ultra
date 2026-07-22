# Vault after-save/after-delete hooks: structured concurrency

Status: DONE — archived 2026-07-20 (review gate passed; open items handed to follow-up tasks)
Date: 2026-07-20

## Problem

`Repository.Hooks.applyOnAfterSaveHooks` / `applyOnAfterDeleteHooks` fired hooks via a global
`VaultScope.launch { yield(); ... }`:

- Broke structured concurrency — work escaped the caller's coroutine into a detached global scope.
- `VaultScope.shutdown()` was never called anywhere in the repo, so the `SupervisorJob` leaked and
  in-flight hooks could be alive at process exit.
- Hook exceptions were swallowed silently (nothing observed the launched job).
- The `yield()` gave no real ordering guarantee — deferral came from `launch`, not from `yield`.

## Design

Two parts:

1. **Hooks are `suspend` and run inline.** `applyOnAfterSaveHooks` / `applyOnAfterDeleteHooks` are
   now `suspend` and simply await the hooks. All call sites were already `suspend`, and
   `Cursor.map` already takes a `suspend` transform.

2. **Deferral is opt-in, rooted in the Ktor `Application`.** A `VaultHookScope` service decides
   whether hooks run inline (`runHook`) or are launched. It is late-bound because the Ktor
   `Application` does not exist at kontainer-build time (`ultraVault(config)` runs during module
   definition; the `Application` only arrives via `AppLifeCycleHooks.onAppStarting(application)`).

   CONSTRAINT (learned the hard way — see review finding 1): `DeferredVaultHookScope` must have
   **no constructor dependencies**. Kontainer promotes any singleton with a transitively dynamic
   dependency to SemiDynamic, i.e. one instance per kontainer — and since every HTTP request builds
   its own kontainer, `bind()` would then be invisible to all of them. That is why `Log` is passed
   to `bind(scope, log)` rather than injected.

### Why not simply `withContext(Dispatchers.IO)`

Switching dispatcher changes *which thread* runs the hooks, not *whether the caller waits*. The
caller still suspends for the full hook duration. Only `launch` avoids waiting.

### Why root in `Application` rather than a fresh global scope

`Application` is a `CoroutineScope` whose job is cancelled on app shutdown, so in-flight hooks are
cancelled instead of leaking. Established precedent in this repo:
- `funktor/cluster/.../workers/WorkersRunner.kt:31` — `app.launch(...)`
- `funktor/core/.../lifecycle/AppLifeCycleBuilder.kt:47` — `runBlocking(app.coroutineContext)`,
  commented "so we never create a detached scope".

NOTE: `WorkersRunner` passes `SupervisorJob() + Dispatchers.IO` into `launch`. An explicit `Job` in
the context *replaces* the parent, detaching from the app's job tree — which would forfeit
cancellation-on-shutdown. So `VaultHookScope` launches with `Dispatchers.IO` only, keeping the app
job as parent, and contains failures with a per-hook `try/catch` that rethrows
`CancellationException`.

## Files

- `ultra/vault/src/jvmMain/kotlin/VaultHookScope.kt` (new)
- `ultra/vault/src/jvmMain/kotlin/Repository.kt` — hooks suspend, `VaultScope` usage removed
- `ultra/vault/src/jvmMain/kotlin/vault_module.kt` — `VaultScope` deleted, `VaultHookScope` registered
- `karango/core/src/main/kotlin/vault/KarangoDriver.kt` + `vault/EntityRepository.kt`
- `monko/core/src/main/kotlin/MonkoDriver.kt` + `MonkoRepository.kt`
- `funktor/core/src/jvmMain/kotlin/` — binder that binds the `Application` on app start

## Context

There are currently **zero** concrete `OnAfterSave` / `OnAfterDelete` implementations in production
code — only marker interfaces resolving to empty lists. Deferral is therefore inert today; it is
built so a future slow hook does not block writes. For durable "do it later" work that must survive
restarts, prefer `funktor/cluster/.../backgroundjobs/BackgroundJobs.kt` over fire-and-forget.

## Review record

`/feature-review` run 2026-07-20 — 3 parallel reviewers (implementation/style, domain, security),
findings verified by the coordinator against the code before acceptance.

| # | Sev | Finding | Status |
|---|-----|---------|--------|
| 1 | CRITICAL | `DeferredVaultHookScope` was promoted to **SemiDynamic** (one instance per kontainer) because its `log: Log` dependency is transitively dynamic via `UltraLogManager`. `bind()` hit the startup kontainer only, so every request got an unbound instance — deferral was dead on arrival. Found independently by all 3 reviewers; reproduced with a failing test before fixing. | FIXED — `Log` moved out of the constructor into `bind(scope, log)`; guarded by `VaultHookScopeWiringSpec` (identity across kontainers + `ServiceProvider.Type.Singleton`) |
| 2 | HIGH | No test covered repository → `hookScope` routing. | FIXED — `karango/core/src/test/kotlin/e2e/crud/E2E-Crud-HookScope-Spec.kt` asserts insert/save/remove emit the expected descriptions through a recording scope, against a real ArangoDB |
| 3 | MEDIUM | `bind` unguarded: launching into an already-cancelled scope produces an already-cancelled coroutine, so the hook never runs *and* nothing is logged. | FIXED — `runHook` falls back to inline when `!scope.isActive` |
| 4 | MEDIUM | Cancelled hooks vanished silently (`CancellationException` rethrown without logging). | FIXED — logged at WARN before rethrow |
| 5 | MEDIUM | Two tests were vacuous — the "does not block" test asserted nothing, and "cancelled" asserted a condition that also holds for a leaked, forever-suspended hook. | FIXED — rewritten to assert ordering positively (`hookFinished shouldBe false` while parked) and to record cancellation via a `CancellationException` catch |
| 6 | LOW | `launchHook` misnamed (contract is "await *or* launch"); dangling KDoc links (`[VaultHookScope]` unimported, `[Deferred]` nonexistent). | FIXED — renamed to `runHook`; KDoc reworded |
| 7 | MEDIUM | Claim: driver ctor param makes `ultraVault` a hard requirement of `KarangoModule`/`MonkoModule`. | REJECTED — `KarangoModule` already injects vault's `Database` + `EntityCache` into `KarangoCodec` (`karango/core/src/main/kotlin/index.kt:48`), so the coupling pre-dates this change |

### Resolved after review (owner decisions)

- **Failure contract unified (was the open HIGH).** Both implementations now contain-and-log; hook
  failures never reach the caller. Rationale: after-save hooks run once the write is already
  committed, so failing the repository call would report failure for an operation that succeeded,
  and a deferred hook has no caller to propagate to. Hooks own their error handling. Implemented as
  a shared `runHookContained`; `CancellationException` is still rethrown so cancellation stays
  cooperative. `VaultHookScope.Inline` became a class taking a `Log`, defaulting to `FallbackLog`
  (stderr) rather than `NullLog` — a failure that is neither propagated nor logged would be
  invisible, which is the defect this task set out to remove.
  TRADE-OFF: a broken hook no longer fails a test; it prints to stderr.
- **`KtorConfig.isProduction` now accepts `"prod"`** (fixed by the owner) — covered by
  `funktor/core/src/jvmTest/kotlin/config/ktor/KtorConfigSpec.kt`. This mattered more than it
  looked: `Deployment.environment` **defaults to `"prod"`**, so before the fix a default-configured
  app was classified non-production and served full stack traces via `ApiStatusPages.withCause`.

- **`ApiStatusPages` now fails closed.** The condition was inverted to ask
  `config?.ktor?.isNotProduction == true`, so an unknown environment (no kontainer / no
  `AppConfig`) exposes only `cause.message`, never a stack trace. The predicate was extracted to
  `ApiStatusPages.exposesStackTraces(config)` so it is testable without booting Ktor — covered by
  `funktor/rest/src/jvmTest/kotlin/ApiStatusPagesSpec.kt`, whose null-config case fails against the
  old logic.

### Open — handed off to follow-up tasks

- **Re-entrancy can loop forever in deferred mode** → `.claude/tasks/20260720-vault-hook-reentrancy.md`
- **Wider error-disclosure audit** (triggered by the `ApiStatusPages` fix; found that production
  still returns `cause.message` containing full AQL, and that the Insights GUI has no auth gate) →
  `.claude/tasks/error-disclosure/20260720-error-response-disclosure-audit.md`
- **Bulk writes.** `EntityRepository.batchInsert` awaits hooks serially per row in inline mode; a
  slow hook makes large imports very slow.
- **Binary compatibility.** `KarangoDriver`/`MonkoDriver` gained a constructor parameter. Source
  compatible, **not** binary compatible — precompiled downstream callers get `NoSuchMethodError`.
  Needs a release note.

### Verification

| Suite | Result |
|---|---|
| `ultra:vault:jvmTest` | 241 pass, 0 fail |
| `karango:core:test` (real ArangoDB) | 1646 pass, 0 fail |
| `monko:core:test` | 237 pass, 0 fail |
| `funktor:core:jvmTest` | 765 pass, 0 fail |
| `funktor:rest:jvmTest` | 52 pass, 0 fail |
| `funktor:auth:jvmTest` | 86 pass, 0 fail |
| `funktor-demo:server` | compiles |

Not security-critical (no auth/tenancy/token surface; security review explicitly cleared
cross-tenant context leakage — identity rides a per-request kontainer in a Ktor `AttributeKey`, not
coroutine-locals), so no red-team follow-up task created.
