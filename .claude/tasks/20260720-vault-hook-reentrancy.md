# Vault after-save/after-delete hooks: re-entrancy can loop forever

**Status:** TODO — known issue, no fix decided yet
**Plan:** none — surfaced by `.claude/tasks-archive/2026-07/20260720-vault-hook-scope.md`
**Security-critical:** no (availability/DoS risk, not a data-boundary issue)

## Spec

An `OnAfterSave` hook that writes back into the same repository re-triggers itself, unbounded:

```
repo.save(x) → onAfterSave(x) → repo.save(x') → onAfterSave(x') → …
```

Nothing in `Repository.Hooks` or `VaultHookScope` bounds this. Same for `OnAfterDelete` via a hook
that deletes. This is a realistic shape for the work these hooks invite — denormalisation, audit
trails, search re-indexing, "touch the parent aggregate".

- [ ] Hook re-entrancy is bounded in **both** `Inline` and `Deferred` modes, with identical
      observable semantics
- [ ] The guard catches direct self-recursion **and** mutual recursion across repositories
- [ ] Tripping the guard does not fail the originating write (consistent with the established
      contract that after-save hook failures are contained, never propagated)
- [ ] Tripping the guard is loud in logs — it must not look like normal operation
- [ ] The depth limit is a named constant, ideally configurable
- [ ] Tests cover both modes and cannot wedge CI if the guard regresses

## Analysis

### Current failure modes — verified

| Mode | Mechanism | Bound | Symptom |
|---|---|---|---|
| `VaultHookScope.Inline` (`VaultHookScope.kt:39-43`) | `runHookContained` calls `block()` on the caller's coroutine, so each nested save deepens the **same** stack | JVM stack depth | `StackOverflowError`, fails fast and loudly. Contained by `runHookContained` (`VaultHookScope.kt:114`) and logged, so the originating write still succeeds |
| `DeferredVaultHookScope` bound (`VaultHookScope.kt:97`) | `bound.launch(Dispatchers.IO)` — every iteration is a **fresh coroutine**; the previous one returns immediately | **none** | No stack to exhaust. Grows until something else gives out |

The deferred case is the dangerous one, and it degrades in this order: unbounded DB write
throughput first (each iteration is a real `save`), then `Dispatchers.IO` thread-pool saturation as
coroutines pile up faster than they complete, then heap pressure from the queued coroutines. Because
`runHookContained` catches and logs (`VaultHookScope.kt:114-116`), the operator sees **log spam and
rising DB load**, not a crash — so it will be diagnosed late and may look like a traffic spike.

> Pre-existing, not introduced: before the hook-scope work, hooks were always launched detached, so
> the unbounded row was the only behaviour. But the deferred path is now the *intended* production
> path for heavy hooks — exactly where someone is most likely to write a hook that saves.

### Re-entrancy shapes, and which guard catches which

| Shape | Depth guard | Per-entity in-flight set |
|---|---|---|
| Direct self-write (A saves A) | yes | yes |
| Mutual recursion (A's hook writes B, B's hook writes A) | yes | **no** — different entities, set never collides |
| Fan-out (one save → N saves, each → N) | bounds depth, **not breadth** | no |

Mutual recursion is why the depth guard is the stronger primitive. Neither bounds fan-out; that
needs a separate breadth/budget notion and is out of scope here (note it, don't solve it).

### The context-propagation trap — the crux

`VaultHookScope.kt:97` is `bound.launch(Dispatchers.IO)`. The new coroutine's context derives from
`bound` (the **application** scope) plus `Dispatchers.IO` — **the caller's `CoroutineContext` is not
inherited**. So the obvious implementation of a depth guard, a `CoroutineContext.Element` read via
`currentCoroutineContext()`, **silently resets to zero on every deferred hop** and provides no bound
at all in precisely the mode that needs it.

This is the single most important fact for the implementer. A depth guard is still the right answer,
but the depth must be captured *before* the launch and re-attached *explicitly*:

```kotlin
// in DeferredVaultHookScope.runHook, before launching
val next = currentCoroutineContext()[HookDepth]?.next() ?: HookDepth(1)

bound.launch(Dispatchers.IO + next) {          // safe: an Element is not a Job, so the
    runHookContained(description, log, block)  // application job stays the parent
}
```

Adding a non-`Job` element to `launch` does **not** replace the parent job, so this does not
reintroduce the detachment problem documented at `VaultHookScope.kt:94-96`.

### Where the check goes, and what it does when it trips

Not obvious, and the asymmetry matters. If the guard **throws**:

- in `Inline`, the throw happens on the caller's coroutine inside `runHookContained`, so it is
  caught at `VaultHookScope.kt:114` and logged — the write still succeeds;
- in `Deferred`, it is caught inside the launched coroutine — same.

So a throwing guard *does* terminate the loop (the hook body never runs, so no nested save), and the
containment does not defeat it. But it produces an `error`-level log with a stack trace for what is a
policy decision, and it relies on the containment behaving identically in both modes forever.

**Recommendation: skip-and-log rather than throw.** Check the depth in `runHook` before dispatching;
if over the limit, log at `error` with the description and depth and return without running the
hook. Deterministic, identical in both modes, no exception-control-flow, and cannot be accidentally
re-classified by a future change to `runHookContained`.

### Options considered

| # | Option | Verdict |
|---|---|---|
| 1 | Depth guard via `CoroutineContext.Element` | **Recommended.** Catches self- and mutual recursion. Requires the explicit re-attachment above — without it, it is a no-op in deferred mode |
| 2 | Per-`(repo, key)` in-flight set | Rejected as primary: misses mutual recursion, and needs a concurrent set with careful removal on every exit path (including cancellation). More machinery, less coverage |
| 3 | Forbid repository writes inside hooks entirely | Rejected: too restrictive — denormalisation is a legitimate and common reason to have an after-save hook at all |
| 4 | Document only | Rejected: leaves a process-killing footgun armed, and the deferred symptom is quiet |

### Cost

Reading a `CoroutineContext` element is a cheap map lookup. The real cost is making the depth
visible to the hook body in `Inline` mode, which needs `withContext(next) { block() }` — a genuine
(if small) overhead on the write path. Measure before worrying; if it matters, `Inline` could skip
depth tracking entirely and rely on `StackOverflowError`, at the price of the semantics no longer
being identical across modes. Prefer identical semantics unless the numbers say otherwise.

### Choosing the limit

Legitimate chains exist: save → hook updates denormalised parent → parent's hook updates a search
index is depth 3. Suggest a default of **5** with a named constant, and make it configurable via
`VaultConfig` if a downstream app has a deeper legitimate chain. Too low is a correctness bug; too
high just delays the runaway.

## Implementation notes

- `HookDepth` belongs next to `VaultHookScope` in `ultra/vault/src/jvmMain/kotlin/VaultHookScope.kt`
  as an `internal` `CoroutineContext.Element` with a `companion object : CoroutineContext.Key`.
- Both `VaultHookScope.Inline.runHook` (`VaultHookScope.kt:39-43`) and
  `DeferredVaultHookScope.runHook` (`VaultHookScope.kt:84-100`) need the check; putting it in a
  shared helper alongside `runHookContained` (`VaultHookScope.kt:106`) keeps them honest.
- Callers are `fireOnAfterSaveHooks` / `fireOnAfterDeleteHooks` in
  `karango/core/src/main/kotlin/vault/EntityRepository.kt` and
  `monko/core/src/main/kotlin/MonkoRepository.kt` — they should need no change if the guard lives in
  the scope.
- There are currently **zero** concrete `OnAfterSave`/`OnAfterDelete` implementations in production
  code (only marker interfaces resolving to empty lists), so nothing is broken today. This is about
  disarming the trap before the first real hook lands — and therefore a good time to do it, since no
  existing behaviour can regress.

### Open questions

- Should the guard count *total* hook invocations per originating request rather than depth? That
  would also bound fan-out, but needs a request-scoped budget and is a bigger design. Deliberately
  not proposed here.
- Should tripping the guard be surfaced anywhere other than logs (a metric, an insights entry)?
  Depends on whether this repo has a metrics facility — not investigated.

## Test evidence

- [ ] Inline: a hook that unconditionally re-saves terminates, the originating `save` still
      succeeds, and the guard is logged
- [ ] Deferred: same, asserting the number of saves is bounded by the limit — **must** be wrapped in
      `withTimeout` and assert on a bounded counter, so a regressed guard fails the test instead of
      hanging CI
- [ ] A regression test proving the deferred guard actually chains: without the explicit
      `+ next` on the `launch`, this test must fail
- [ ] Mutual recursion across two repositories terminates
- [ ] A legitimate 2–3 deep chain is **not** blocked (guards against a too-low limit)
- [ ] End-to-end: not required — the guard is in `ultra/vault` and testable with a fake scope; the
      karango/monko e2e hook-routing test already exists
      (`karango/core/src/test/kotlin/e2e/crud/E2E-Crud-HookScope-Spec.kt`)
- [ ] Full test command(s) run + green: `./gradlew :ultra:vault:jvmTest :karango:core:test
      :monko:core:test`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...
