# Vault after-save/after-delete hooks: re-entrancy can loop forever

Status: OPEN — known issue, no fix decided yet
Date: 2026-07-20
Related: `.claude/tasks-archive/2026-07/20260720-vault-hook-scope.md` (the change that surfaced this)

## The issue

An `OnAfterSave` hook that writes back into the same repository re-triggers itself:

```
repo.save(x) → onAfterSave(x) → repo.save(x') → onAfterSave(x') → …
```

Nothing in `Repository.Hooks` or `VaultHookScope` bounds this. The same applies to `OnAfterDelete`
via a hook that deletes.

This is not hypothetical for the shape of work these hooks invite: denormalisation, audit trails,
search re-indexing and "touch the parent aggregate" logic all write while handling a write.

## Why it matters more now

The failure mode depends on which `VaultHookScope` is active, and deferral makes it **worse**:

| Mode | Behaviour | Severity |
|------|-----------|----------|
| `VaultHookScope.Inline` | Recurses on the caller's stack → `StackOverflowError` | Bad, but self-limiting and fails fast and loudly |
| `DeferredVaultHookScope` (bound) | Each iteration is a **fresh coroutine** launched on the application scope. There is no stack to exhaust. | Unbounded loop spawning unbounded coroutines, plus unbounded DB writes, until the process dies |

Before the hook-scope work, hooks were always launched detached, i.e. the second row of the table
was the *only* behaviour — so this hazard is pre-existing, not introduced. But the deferred path is
now the intended production path for heavy hooks, which is exactly the case where somebody is most
likely to write a hook that saves.

Aggravating factor: in deferred mode hook failures are caught and logged, so a runaway loop shows
up as log spam and rising DB load rather than as a crash. It will be diagnosed late.

## Options to consider

1. **Depth guard.** Track hook nesting depth in the coroutine context (a `CoroutineContext.Element`
   survives `launch` if propagated deliberately; note it does NOT propagate implicitly across the
   `DeferredVaultHookScope` boundary today, since it launches on the app scope). Abort with a clear
   error past a small depth (e.g. 3).
2. **Per-entity in-flight set.** Keep a set of `(repo.name, _key)` currently inside a hook; skip or
   fail re-entry for the same entity. Bounds self-referential loops precisely while still allowing
   a hook to write to *other* repos.
3. **Forbid re-entry entirely.** Make repository writes from inside an after-save hook throw, and
   direct that work to `BackgroundJobs`. Strictest and most predictable; may be too restrictive.
4. **Do nothing, document it.** Note the constraint on the `OnAfterSave` / `OnAfterDelete` KDoc and
   rely on discipline. Cheapest; leaves a production-killing footgun armed.

Leaning towards 1 or 2 — 2 is more precise, 1 is far simpler and also catches mutual recursion
across repositories (A's hook writes B, B's hook writes A), which 2 does not.

## Notes for whoever picks this up

- There are currently **zero** concrete `OnAfterSave` / `OnAfterDelete` implementations in
  production code — only marker interfaces resolving to empty lists. So nothing is broken today;
  this is about the trap being armed before the first real hook lands.
- A guard needs a test in both modes. Inline is easy; the deferred case needs care so a runaway
  test cannot wedge CI — bound the test with `withTimeout` and assert the guard trips rather than
  waiting for exhaustion.
- Relevant code: `ultra/vault/src/jvmMain/kotlin/VaultHookScope.kt`,
  `ultra/vault/src/jvmMain/kotlin/Repository.kt` (`Hooks.applyOnAfterSaveHooks`),
  `karango/core/src/main/kotlin/vault/EntityRepository.kt` (`fireOnAfterSaveHooks`),
  `monko/core/src/main/kotlin/MonkoRepository.kt` (`fireOnAfterSaveHooks`).
