# Streams: fix CutoffStream subscription leaks under reentrancy

**Status:** DONE (archived 2026-08-24) — gradle verification completed, all targets green
**Plan:** standalone — found while building `switchMap`
(`.claude/tasks-archive/2026-08/20260824-streams-switchmap.md`)
**Security-critical:** no (confirmed by the security review — no trust boundary, no red-team task)

## Spec

`CutoffStream` assigned its unsubscribe handles *after* a `subscribeToStream` call that
synchronously delivers a value to subscribers. Anything a subscriber did in that window saw a null
handle, so the operator could not release what it was in the middle of establishing. Every defect
below was reproduced against the real file before being fixed.

- [x] **C1** Cutting off from inside the source's first delivery left the source subscribed AND kept
      publishing to subscribers that should hear nothing.
- [x] **C2** The last subscriber unsubscribing during a resume delivery leaked the source
      subscription permanently into a stopped operator.
- [x] **C3** Subscribing the same handler instance twice ran `start()` twice (the
      `subscriptions.size == 1` guard never sees 2 — the set dedupes), orphaning a predicate
      subscription.
- [x] **C4** Subscribing again from inside the first delivery double-started the same way.
- [x] **C5** (review) A predicate handler firing *after* `stop()` — from an already-taken notify
      snapshot — resubscribed the source onto a dead operator. Permanent leak; with a `ticker()`
      source, a coroutine that never stops.
- [x] **C6** (review) A first subscriber arriving while cut off was **never called at all**,
      violating `Stream.kt:15-17`; the second subscriber got the value. Pre-existing asymmetry.
- [x] **C7** (review) A source value could still be delivered *after* the cut-off took effect, when
      another subscriber of the same source flipped the predicate earlier in the notify snapshot.
- [x] **C8** (review) A throw inside `start()` bricked the operator: the ghost subscriber kept
      `subscriptions` non-empty, so `stop()` was unreachable and the source was never resubscribed.

## Implementation notes

- **Subscribe ordering now matches the library's dominant pattern** (`StreamWrapperBase.kt:41-49`,
  `StreamCombinator.kt`, `switchMap.kt:91`): `start()` runs before the subscriber is added, so its
  publishes land in an empty handler set, and the subscriber is then served explicitly via
  `sub(lastValue)`. This is what fixes C6 and C8, and it removes the need to reason about the first
  subscriber's value arriving as a side effect of `subscribeSource()`.
- **`sourceCount`** is a monotonic generation token. `unsubscribeSource()` bumps it, which is what
  invalidates a subscription still being established; the establishing frame then releases its own
  handle rather than storing it. The source handler also carries its generation and drops values
  from an outdated one (C7).
- **`|| predicateUnsubscribe == null`** in the same guard covers a `stop()` that happened *before*
  the generation was captured, which the counter cannot see (C5). This mirrors `switchMap.kt:161`.
- **`starting`** closes the window while the predicate subscription is itself being established: a
  predicate whose `subscribeToStream` runs reentrant code would otherwise start the operator twice.
  The same hole existed in the committed `switchMap.kt` and was fixed there too.
- **`start()` applies `isCutOff = predicate()`** rather than relying on the predicate's first
  emission, and is wrapped in `try/catch { stop(); throw t }` (C8). Every stream in the library
  emits `invoke()` on subscribe, so this is equivalent today — and it additionally fixes a predicate
  that emits nothing on subscribe, which previously left the source unsubscribed forever.
- `isCutOff`'s initializer is now `false` (a placeholder): `start()` always overwrites it before any
  read, and the old `predicate()` initializer ran a user-visible side effect at construction time.

## Test evidence

- [x] `ultra/streams/src/commonTest/kotlin/ops/CutoffSpec.kt`: **15 cases** (4 pre-existing + 11 new)
- [x] `ultra/streams/src/commonTest/kotlin/ops/SwitchMapSpec.kt`: **27 cases** (+1 for the shared
      `starting` fix)
- [x] **Defects reproduced BEFORE fixing** — the real repo sources compiled standalone with
      `kotlinc` and probed: 5 failing checks pre-fix, all passing post-fix, pre-existing behaviour
      (incl. `cutoffWhenNot`) unchanged.
- [x] **Both specs run through the real kotest 6.2.3 engine**: CutoffSpec 15/15, SwitchMapSpec 27/27.
- [x] **Mutation-tested — 7 mutants, each killed by exactly the intended test(s):** generation guard
      disabled (C1+C2), `size == 1` guard restored (C3), original `start()` restored (C4),
      `predicateUnsubscribe` arm dropped (C5), `isCutOff = predicate()` → `false` (starts-cut-off),
      `established` starts `true` (restart churn), source-handler generation check removed (C7),
      `start()` cleanup removed (C8), `starting` removed (both operators).
- [x] Neither operator has a production call site anywhere in the repo — only their own specs and
      the jsTest `CutoffTickerSpec`.

### Gradle verification — done, after the lock finally freed

- [x] `./gradlew :ultra:streams:jvmTest` — `CutoffSpec` 15, `SwitchMapSpec` 27, **whole module 69
      tests / 0 failures**. Confirmed via `build/test-results/**/TEST-*.xml`, not the exit code.
- [x] `:ultra:streams:linuxX64Test` — `CutoffSpec` 15, `SwitchMapSpec` 27, 0 failures.
- [x] `:ultra:streams:jsTest` (browser) — **`CutoffTickerSpec` 3 / 0 failures**, the one exerciser
      of `cutoffWhen` that could not be checked outside gradle. `CutoffSpec` 15 and `SwitchMapSpec`
      27 also ran in the browser. The static prediction that the ticker spec was unaffected held.
- [x] Compile sweep (`compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs
      compileKotlin compileTestKotlin --continue`) — no `^e:`.

**Two staleness traps checked rather than assumed**, both of which `CLAUDE.md` warns about and both
of which showed `UP-TO-DATE` on this run:

- `jvmTestClasses UP-TO-DATE` — but the XML holds 15/27 testcases including the newest names (with
  the `[jvm]` suffix kotest appends), which a stale compile could not contain.
- `compileKotlinJs UP-TO-DATE` — legitimately current, because another agent's sweep had already
  compiled JS while these edits were on disk. Confirmed by finding `sourceCount`/`_sourceCount` in
  `build/compileSync/js/test/testDevelopmentExecutable/kotlin/ultra-ultra-streams.mjs`.

## Review record (/feature-review, 2026-08-24)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | FAIL → fixed | 1 HIGH (C5), 1 MEDIUM already fixed mid-review (C8), 2 MEDIUM test gaps, 3 LOW; style clean |
| 2. Domain expert | APPROVE with findings | 1 MEDIUM (C6+C8 — adopt the library ordering), 3 LOW; endorsed the semantics and confirmed no `jsTest` risk |
| 3. Security | PASS (no boundary findings) | 1 LOW NEW (`starting`), 2 LOW pre-existing; leak amplification under a hostile subscriber drops ~100x |

All three converged on the same root cause. Findings C5–C8 came out of the gate itself; C6 and C7
were independently reproduced by me before acting on them, and one reviewer claim did **not**
survive: my own first C2 probe tested something unreachable (a subscriber cannot unsubscribe during
its *own* first delivery — it does not hold the handle yet), so it was rewritten as the reachable
resume-path version.

**Accepted, not fixed** (recorded so they are not re-litigated):

- **Same handler instance subscribed twice is one subscriber.** It is notified once per value and
  the *first* release tears the operator down. Pre-existing and library-wide — `StreamSourceImpl`,
  `StreamWrapperBase`, `StreamCombinator` and `SwitchMapStream` all store subscribers in a `Set`.
  Fixing it means changing the library's subscription model, which is out of scope here. The spec
  now pins the real behaviour rather than implying refcounting.
- **A subscriber added during `start()` sees the initial value twice** (once from `sub(lastValue)`,
  once from `start()`'s own publish). Accepted cost of the reentrant-predicate case; asserted
  explicitly in the spec.
- **`notifyHandlers` iterates a snapshot without re-checking membership** (`streams.kt:22`), so a
  handler unsubscribed mid-notification still runs. This is the root enabler of C5/C7 and affects
  every operator; the fix belongs in `notifyHandlers`, not here.
- **`invoke()` is stale while nobody is subscribed** — unlike `StreamMapper` and `SwitchMapStream`,
  which recompute. Pre-existing, unchanged, and arguably correct for a "frozen" operator.
- **Naming**: `sourceCount`/`switchCount` are generation tokens rather than counts of live
  subscriptions. Left as-is for consistency across the two operators.
