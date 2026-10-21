# ULTRA-00205: Kronos: collapse chained `advanceBy` into one offset

> **Status (2026-10-18): DONE.** (archived 2026-10-18). Started 2026-10-17, requested by the maintainer from the spross
> project, where a simulated clock advances once per simulated minute.

**Security-critical:** no (a clock helper; no authz).

## Background

`Kronos.advanceBy(duration)` (`ultra/datetime/src/commonMain/kotlin/Kronos.kt`) wraps the receiver
in a new `AdvancedBy(inner = this, provider)` and `AdvancedBy.instantNow()` is
`inner.instantNow().plus(duration)`. Every call adds one layer:

```kotlin
val k = Kronos.systemUtc.advanceBy(1.minutes).advanceBy(1.minutes).advanceBy(1.minutes)
// AdvancedBy(AdvancedBy(AdvancedBy(systemUtc, 1 min), 1 min), 1 min): three nested calls per instantNow()
```

The same happens through a `Kronos.Mutable`: `mutable.mutate { advanceBy(dt) }` per step nests one
wrapper per step, since `mutate` replaces the inner with the block's result and the block wraps.
A simulation that advances by a minute for a season (90 days, 129,600 steps) then recurses
129,600 deep on every `instantNow()` and on `describe()`, which is a stack overflow on Kotlin/JS
long before and a growing cost on the JVM. The workaround in spross today is
`mutable.set(Kronos.fixed(at))` per step, which is flat but loses the "advance" reading of the
code.

`describe()` has the same shape: a chain of `n` advances gives `n` nested
`KronosDescriptor.AdvancedBy`, and `Kronos.from(descriptor)` rebuilds the chain.

## Spec

Design agreed with the maintainer 2026-10-17 (supersedes the first draft's "sum the two lazies",
which only moved the recursion: an unread chain of lazies-over-lazies recurses `n` deep on its first
evaluation — exactly the 200,000-step acceptance case).

`AdvancedBy` holds `inner` (never an `AdvancedBy`), a `settled: Duration` (everything already known)
and `pending` (lambdas nobody has needed yet) — built as a persistent newest-first cons list
(`AdvancedBy.Pending`), not a copied `List`: a copy per append would be O(n²) over a long unread chain.

- [x] `advanceBy(d: Duration)` on an `AdvancedBy` folds into `settled`: `AdvancedBy(inner, settled + d,
      pending)`. Never calls a pending lambda. A chain of any length of `Duration` advances is one node.
- [x] `advanceBy { p }` appends `lazy(p)` to `pending` and calls nothing. **The lambda form stays fully
      lazy** — thebase uses it for a clock offset that must first be read from the database. Provider
      contract unchanged: called exactly once, the first time the clock is read; never at chain time.
- [x] Reading folds `settled` + pending values in a loop (oldest first, as today) and caches the total.
      No recursion anywhere. Once a node's total has been read, the next `advanceBy` on it starts fresh
      from that total, so resolved lambdas are never kept. Old and new nodes share the same `Lazy`
      instances, so a provider is called once even when both are read.
- [x] `pending` only grows through consecutive UNREAD `advanceBy { }` calls — inherent: each is a
      lambda nobody has called yet. Flat list, iterated, never recursed.
- [x] Both overloads share the representation (a `Duration` is just a known part).
- [x] `Kronos.Mutable.mutate { advanceBy(dt) }` called `n` times keeps the inner one layer deep
      (`mutate` needs no change). A spec runs 200,000 `mutate { advanceBy(1.minutes) }` on a mutable
      fixed clock, on the JVM and on JS: `instantNow()` is start + 200,000 minutes and `describe()` is a
      single `AdvancedBy`.
- [x] Laziness spec: a lambda followed by 200,000 `advanceBy(1.minutes)` is not called during the chain
      and exactly once on first read; a lambda never needed is never called; reading old and new node
      calls it once.
- [x] `describe()` of a collapsed chain is `AdvancedBy(durationMs = total, inner = inner.describe())`
      (total truncated to ms once, not per layer). A NESTED descriptor read from an old file still
      instantiates (`Kronos.from` walks the nesting) and describes back flat. Pinned by decoding an
      old-format JSON string. The `KronosSpec` round-trip row is rewritten: nested in, flat out, same
      instant.
- [x] A zero or negative sum stays `AdvancedBy(inner, 0)`, not the inner itself (stable shape).
- [x] `Kronos.Mutable.set` cycle guard FIXED (maintainer, 2026-10-17, after review round 1 found it
      pre-existing broken): it walks the INCOMING kronos through `MutableImpl` and `AdvancedBy` layers
      and ignores one that reads this mutable; `mutate` goes through the same guard. A rejected set stays
      silent (the original intent). Cannot see through third-party wrappers (e.g. spross `ReadOnlyKronos`).
- [x] Maintainer decisions after review round 2 (2026-10-18): a rejected `set`/`mutate` THROWS
      `IllegalArgumentException` (was: silent — that froze the clock on an easy mistake); the cached total
      uses `LazyThreadSafetyMode.PUBLICATION` (no lock held while shared provider lazies run, as before the
      collapse); `Kronos.from` sums a nested descriptor's layers in a loop (no recursion), accepts ±infinite
      layers (an infinite advance round-trips) and rejects only OPPOSITE infinities, with a named error
      (maintainer, after round 3 showed rejecting every infinity broke the round trip); `Kronos.Mutable` KDoc
      states what the guard sees and that it is not safe against concurrent writers. thebase has no
      `Kronos` override (maintainer), so the Duration overload no longer routing through the lambda one
      needs no release note.

## Implementation notes

- `ultra/datetime/src/commonMain/kotlin/Kronos.kt` — `AdvancedBy(inner, settled, pending)`; both
  `advanceBy` overloads overridden there; the interface defaults build the first node. `Kronos.from`
  sums a nested descriptor's layers iteratively, innermost first (as a live nested chain reads), over the
  innermost `SystemClock`, and names the only failing case (opposite infinities). Nesting is still
  WRITTEN today when a `Mutable` sits between layers, so this path is not legacy-only.
- `describe()` stays nested when a `Mutable` sits between layers (`AdvancedBy(m, …)` over a mutable that
  holds an `AdvancedBy`) — correct: the outer layer must keep following the mutable. "Flat" holds per chain.

## Test evidence

- [x] `KronosSpec` — collapse, laziness, order, zero shape, sub-ms sum, 200k chains (eager, lazy, Mutable),
      old-format nested JSON decoded and described back flat.
- [x] Mutation-checked (persistence: the descriptor is serialized), all RED on the named test, source
      restored and `cmp`-verified: no collapse (old nesting → `StackOverflowError` at 200k on JVM),
      Duration overload forcing pending, lambda overload forcing older, providers not shared via `Lazy`,
      newest-first order, zero sum unwrapping to inner, per-step ms truncation, `Kronos.from` dropping
      nested inner.
- [x] Round-1 fix batch mutation-checked, all RED on the named spec, restored + `cmp`: read-node lambda
      branch → `ZERO` / double-count old pending / `settled`; `MutableImpl` forwarding `advanceBy` to its
      inner; swallowing a throwing provider; guard walking current `_inner`; guard skipping `AdvancedBy`;
      `mutate` bypassing the guard; guard rejecting everything.
- [x] Rounds 3–4 batches mutation-checked, all RED on the named spec, restored + `cmp`: set silent; `from`
      without require / rejecting every infinity / sign flipped / rejecting same-sign infinities / rejecting
      an infinite total then an opposite finite layer / outermost-first order / old recursion
      (`StackOverflowError` at 200k nesting) / reading only the outer layer. NOT mutation-checkable in the
      common fixture: `PUBLICATION` vs `SYNCHRONIZED` for `total`, and the `total.isInitialized()` arms
      (identical values; they buy O(1) per step) — guarded by reading only.
- [x] **Final tree (2026-10-18T07:53):** `:ultra:datetime:jvmTest` / `jsTest` / `linuxX64Test` 79/79 each
      (XML), `compileKotlinLinuxArm64` green, `:ultra:maths:jvmTest` / `jsTest` 28/28, repo compile sweep
      zero `^e:` — one chain, exit 0.
- [x] After round-1 fixes: `:ultra:datetime:jvmTest` / `jsTest` / `linuxX64Test` 74/74 each (XML,
      2026-10-17T21:50–21:51); `:ultra:maths:jvmTest` / `jsTest` (`EasingSpec`, uses `mutate { advanceBy }`)
      28/28.
- [x] Before round-1 fixes: `:ultra:datetime:jvmTest` / `jsTest` (browser) / `linuxX64Test` green, 68/68 each (XML);
      `compileKotlinLinuxArm64` green; repo compile sweep (`compileKotlinJvm compileTestKotlinJvm
      compileKotlinJs compileTestKotlinJs compileKotlin compileTestKotlin --continue`) zero `^e:`.

## Review record — the LEDGER

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `Kronos.kt:142` | Lambda advance on an already-read node (`total.isInitialized()` branch) has no test; `total.value`→`ZERO`, →`settled`, or keeping old `pending` (double count) all survive | fix | Verified: `KronosSpec:282` chains a Duration on a read node (line 147 branch); `:286` builds `second` before `first` is read. Test added + mutants run |
| 1 | MINOR | `Kronos.kt:148`, `:123` | `+INFINITE` then `-INFINITE` now throws (`Duration.plus`) — at chain time for the Duration overload, at first read for lambdas; old code clamped the `Instant` per layer | maintainer-decision (MINOR list) | Real (raised by all three charters, and by the coordinator to the maintainer pre-implementation). Needs > ~146M-year durations; nothing decodes descriptors from outside. Options: accept fail-fast, or reject non-finite `durationMs` in `Kronos.from` |
| 1 | MINOR | `Kronos.kt:159-172` | Pre-existing: `Mutable.set` cycle guard walks the CURRENT `_inner`, not the incoming one, and stops at `AdvancedBy` — `m.set(m)` / `m.set(m.advanceBy(x))` then overflows on every read | fix (maintainer: "if there is a pre-existing bug please fix this one") | Verified by reading: `_inner.recurse {}` starts at `_inner`. Guard now walks the incoming kronos through `MutableImpl` + `AdvancedBy`; `mutate` routed through `set`. Specs added + mutated |
| 1 | MINOR | `KronosSpec.kt:30`, `:40` | Round-trip row :30 is now flat-in/flat-out; old-format test pins describe + wire format but never `instantNow()` | maintainer-decision (MINOR list) | "Same instant" is implied (describe pins the total; other specs pin inner + total) but cannot be asserted directly: a fixed clock describes as `SystemClock` (task "Related") |
| 1 | MINOR | `KronosSpec.kt` | No spec pins (a) an advance over a `Mutable` keeps following it, (b) a throwing provider is retried, not cached (thebase's DB case) | fix (test-only, reviewed in round 2) | Verified both hold today by reading `MutableImpl` (no `advanceBy` override) and Kotlin `lazy` (a throwing initializer is not cached). Specs added + mutated |
| 1 | MINOR | `Kronos.kt:114` | Comment "matching the order of the former nested implementation" is wrong for `describe()` as first read (old: newest first) | fix | Verified: old `describe()` evaluated `duration` before `inner.describe()`. Comment reworded |
| 1 | MINOR | `Kronos.kt:207` | Duration overload no longer routes through the lambda overload; a third-party `Kronos` overriding only the lambda form no longer intercepts it | maintainer-decision (MINOR list) | No override in ultra/spross (grep); thebase not on disk |
| 1 | MINOR | `Kronos.kt:193` | KDoc "called exactly once" is really "at most once successfully" (never if unread; retried after a throw) — predates change | maintainer-decision (MINOR list) | Pre-existing wording; belongs with the docs follow-up |

| 2 | MINOR | `Kronos.kt:176-178` | Routing `mutate` through the SILENT guard turns `m.mutate { m.advanceBy(dt) }` (a likely mistake, invited by the broken docs example) from an immediate stack overflow into a silently frozen clock | maintainer-decision | "Silent" was the coordinator's reading of the old guard's intent, never confirmed by the maintainer. No working caller can notice either way (reviewers: no false positives; old guard never fired) |
| 2 | MINOR | `Kronos.kt:159`, `:17`, `:20` | Guard KDoc "ignores an inner that reads this instance" overclaims — only `MutableImpl`/`AdvancedBy` layers are seen; `advanceBy { }` providers, clocks and third-party wrappers (spross `ReadOnlyKronos`) are not. Public `set`/`mutate` KDoc never mentions the rejection | maintainer-decision (MINOR list; wording depends on the row above) | Verified by reading the walk at `:161-167` |
| 2 | MINOR | `Kronos.kt:113-124` | `total` is a SYNCHRONIZED lazy held while it forces shared provider lazies: a provider that reads its own chain (already a bug) can now DEADLOCK across two threads where it used to stack-overflow. Fix: `lazy(LazyThreadSafetyMode.PUBLICATION)` for `total` only | maintainer-decision (MINOR list) | Verified by reading: old code took only the provider's lock; contradicts round-1 CLEAN "no new deadlock", which held only for non-cyclic providers |
| 2 | MINOR | `Kronos.kt:160-173` | Check-then-act: two threads doing `a.set(b.advanceBy(x))` / `b.set(a.advanceBy(y))` concurrently can both pass and build a cycle; `_inner` is not `@Volatile` (pre-existing) | maintainer-decision (MINOR list) | Real; old guard caught nothing, so not a regression. Options: KDoc "single writer", or a lock |
| 2 | MINOR | `Kronos.kt:123`, `:148` | Summing Durations loses sub-ns precision once the total exceeds ~146 years (`Duration` switches to ms); old code added per layer to an ns `Instant` | maintainer-decision (MINOR list) | Same root as the round-1 INFINITE row; no practical impact |
| 2 | MINOR | `Kronos.kt:142`, `:147` | Deleting both `total.isInitialized()` arms survives every test (shared resolved `Lazy`s give identical values) — only cost is O(n²) on a read-every-step lambda loop | maintainer-decision (MINOR list) | Fixture cannot express it except by timing; recorded as "guarded by reading only" |

**Round 2 process note.** Phase 1 was NOT blind: all three reviewers read the task file in one call and saw
this ledger (they disclosed it). "Do not read section X" in a brief cannot be enforced against a single
file. A separate phase 2 was not run: every overlap with a round-1 row was flagged and not re-raised in
phase 1, the round-1 MAJOR fix was re-checked there (impl charter: guard table and mutant list), and the
round-1 ledger holds no rejected CRITICAL/MAJOR for duty (c) to audit. Next time: hand phase-1 reviewers a
copy of the task file without the Review record.

**Round 2: 0 CRITICAL, 0 MAJOR (all three charters) → clean round, loop terminates.** Probed CLEAN in
round 2: guard has no false rejections (walked edges are always read, `in` is identity), `recurse` terminates
on any graph and is iterative, guard cost per `mutate { advanceBy }` step is a 2-element walk, flat
descriptors are readable by older ultra versions, spross and EasingSpec unaffected.

Round-2 MINORs dispositioned by the maintainer 2026-10-18: cyclic set/mutate THROW; PUBLICATION total;
honest guard KDoc + single-writer note; `Kronos.from` range check; thebase has no override. Applied as one
batch, reviewed in round 3 (tiered: one `reviewer-xhigh` over all three charters, blind copy of this file).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 3 | MINOR | `Kronos.kt:69` vs `:143` | `from` rejecting every non-finite `durationMs` breaks the round trip of `advanceBy(INFINITE).describe()` (Long.MAX_VALUE ms) and of old stored records | maintainer-decision → fix | Maintainer chose: accept ±∞ per layer, sum iteratively, name only opposite infinities. Rewritten + 4 mutants RED; reviewed in round 4 |
| 3 | MINOR | `KronosSpec.kt:62-84` | Boundary test pinned only the accepting side; off-by-one `abs(ms) <= Long.MAX_VALUE / 2` survived | fix (test-only, mutation-checked, not re-reviewed) | Mutant RED after adding ±4_611_686_018_427_387_903; superseded by the round-3 rewrite of `from`, which replaced that test |
| 3 | MINOR | `Kronos.kt:207` | KDoc "called exactly once" contradicts the retried-after-throw spec | fix (KDoc-only, not re-reviewed) | Verified against `KronosSpec` "a provider that throws is retried" (`calls shouldBe 2`) |
| 3 | MINOR | task file | "`Kronos.from` unchanged" stale | fix | Restated |
| 3 | MINOR | `kronos.astro:77-79`, `:116-118` | Pre-existing wrong docs; set/mutate contract changed | out-of-scope → docs follow-up | Created on archive |

**Round 3: 0 CRITICAL, 0 MAJOR → clean.** CLEAN in round 3: PUBLICATION keeps one call per provider on
JVM/Native/JS, exceptions not cached; a throwing set/mutate leaves the mutable unchanged; every production
caller sets a non-cyclic clock; security NO FINDINGS.

After round 3 the maintainer chose: `from` accepts ±∞ per layer, sums iteratively, names opposite
infinities. Reviewed in round 4 (tiered: one `reviewer-xhigh`, all three charters, blind copy). Note: the
round-3 rows above were written after round 4 had started (its phase 1 was blind, so nothing was lost).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 4 | MINOR | `Kronos.kt:71-79` | Loop summed outermost first; old form innermost first — differs once a running sum saturates (~146M years), e.g. `[-M, M, M]` | fix (mutation-checked, not re-reviewed) | Now `foldRight` over the collected layers; order spec RED under outermost-first |
| 4 | MINOR | `Kronos.kt:67` | Comment called nesting legacy-only; `describe()` still nests with a `Mutable` between layers | fix | Factually wrong; reworded |
| 4 | MINOR | `KronosSpec.kt:60-80` | Require mutants rejecting same-sign infinities, or an infinite total then an opposite finite layer, survived | fix (test-only, mutation-checked) | "Only opposite infinities are rejected" spec added; both mutants RED |

**Round 4: 0 CRITICAL, 0 MAJOR → clean.** CLEAN in round 4: the require is exactly "the next `+` would
throw" (Kotlin 2.4.10 `Duration.plus` source), saturated finite sums meeting an opposite infinity are
caught, `from` calls no provider and does not recurse, error message leaks nothing.

Not surviving verification: none. Probed and CLEAN (all three charters): no recursion on any read path;
O(1) chaining (shared cons list); JVM thread-safety (synchronized `lazy`, oldest-first lock order, no new
deadlock); provider failure behaves as before (no poisoning); collapse never crosses a `Mutable`;
`Kronos.from` recursion depth pre-existing and bounded by the decoder; infinite total describes as
`Long.MAX_VALUE` ms and decodes back to INFINITE. SURPLUS: nothing removable (all three).

Docs (pre-existing errors, for the docs follow-up): `kronos.astro:77-79` (`mutable.advanceBy` does not
move the mutable — should be `mutate { advanceBy(..) }`), `kronos.astro:116-118` (`AdvancedBy(millis = …)`
does not compile), `kronos.astro:107` ("for persisting" is lossy for fixed clocks).

**Verdict: gate PASS** (2026-10-18). Rounds: 1 (1 MAJOR fixed), 2–4 clean. No open CRITICAL/MAJOR. All
MINORs dispositioned by the maintainer and applied; the last batch (round 4) was mutation-checked but not
re-reviewed. Not security-critical → no red-team task. Docs follow-up (public API touched: describe shape,
`set`/`mutate` throw, `from` semantics): ULTRA-00207 (docs kronos).

## Related, not in this task

- `Kronos.fixed(instant)` builds a `UsingClock(FixedClock)`, and `UsingClock.describe()` returns
  `KronosDescriptor.SystemClock`, so a fixed clock describes itself as the system clock and
  `describe().instantiate()` does not give the fixed instant back. A `KronosDescriptor.Fixed(epochMs)`
  would make a fixed clock round-trip; it is a serialized format addition and deserves its own
  decision.
- spross needs a read-only view of a `Kronos` (`mutable()` on a `Mutable` returns itself, so a
  Mutable handed out as a `Kronos` can be mutated by anyone who calls `mutable()` on it). spross
  writes that view in its own `common` module as a candidate for ultra; if it proves useful, it
  moves here as `Kronos.readOnly()`.

## History

- 2026-10-17: written from the spross pit-stop on the simulated clock (`spross/docs/plans/world-and-clock.md`).
- 2026-10-17: spec redesigned with the maintainer (settled + pending lambdas); lambda laziness confirmed load-bearing.
- 2026-10-17: maintainer asked to fix the pre-existing `Mutable.set` cycle guard in this task.
- 2026-10-18: gate PASS after 4 rounds; archived.
- 2026-10-21: numbered ULTRA-00205 (was `docs/tasks-archive/2026-10/20261017-kronos-advanceby-collapse.md`).
