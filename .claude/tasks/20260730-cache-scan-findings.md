# ultra/cache: scan findings

**Status:** SCAN IN PROGRESS — 2026-07-30
**Plan:** module-by-module review (`cache` is module 5, after `vault`, `common`, `reflection`, `log`)
**Security-critical:** no

Four read-only agents reviewed the module in parallel, one file group each. Every finding below was
re-checked by the coordinator against the code; the marker says how far that check got.

- `[verified]` — coordinator confirmed the mechanism in the source, or by running something.
- `[reported]` — agent's claim, mechanism plausible, not independently confirmed yet.
- `[unreachable]` — real defect, no caller can reach it today.
- `[rejected]` — did not survive the check.

## Module shape

| File | Lines | Reviewed by |
|---|---|---|
| `FastCache.kt` | 749 | agent 1 |
| `ObjectSizeEstimator.kt` + 4 platform actuals | 184 + 75 | agent 3 |
| `ValueSortedMap.kt` | 122 | agent 2 |
| `NullableCache.kt` | 81 | agent 4 |
| `Cache.kt` | 62 | agent 4 |

Consumers, repo-wide — only two:
- `ultra/slumber` `DataClassSlumberer.kt` — `Cache<Any?, Any?>` + `FastCache.Builder`
- `ultra/vault` `caching.kt` — `NullableCache`, via `DefaultEntityCache` and `SharedRepoClassLookup`

`ObjectSizeEstimator.kt` and `NullableCache.kt` were themselves written/reworked during the earlier
`ultra/common` pass. Agents were told to scrutinise rather than trust them. **That paid off: A1 is a
regression introduced by that rework.**

---

## ROOT CAUSE — one defect, six symptoms `[verified]`

**`removeSilently` records no action.** `FastCache.kt:749-760`:

```kotlin
private fun removeSilently(key: K) = sync {
    val hasPendingUpdates = lastActionsKeys.contains(key)
    if (!hasPendingUpdates) {
        val removed = map.remove(key)
        if (removed != null) {
            evictionListeners.forEach { listener -> listener(key, removed) }
        }
    }
}                                       // <-- no addAction(RemoveAction(key)) anywhere
```

Each behaviour removes the key from its **own** `data` before calling `removeSilently`, so the
*evicting* behaviour stays consistent. Every **other** configured behaviour never learns. Symptoms:

| Symptom | Effect |
|---|---|
| F1 `refreshAfterWrite` resurrects evicted entries | `writeTimestamps` keeps the key, the refresh re-`put`s it, the evictor drops it again — permanent churn and unbounded loader traffic |
| F8 `MaxMemoryUsageBehaviour.totalSize` is wrong | counts entries another behaviour already evicted; the memory cap is enforced against a number that includes freed bytes, so live entries get evicted for phantoms |
| F6 `clear()` leaves behaviour state stale | `totalSize`, `data`, `writeTimestamps`, `refreshingKeys` all survive a `clear()` |
| F12 dead `Entry.value` | every behaviour holds a strong ref to every value; combined with the above, those refs **outlive the cache entry** |

And the **inverse** race, same function:

| F3 immortal entries | `evict()` does `data.remove(key)` *first*, then `removeSilently` may **decline** (pending action). `process` runs outside `sync` (`FastCache.kt:618-620`), so a concurrent `get` opens that window. The entry stays in `map` with no behaviour tracking it — never expired, never evicted again. `ExpireAfterWriteBehaviour` only re-inserts on a `PutAction`, so the read that saved it does not restore tracking. |

Fixing the notification path addresses F1, F8 and F6 together; F3 additionally needs `evict()` to
drop its tracking only when `removeSilently` reports it actually removed the entry.

---

## A. Confirmed, high severity

### A1. The cycle guard hashes the object it is guarding — cyclic graphs overflow the stack `[verified]`
`ObjectSizeEstimator.kt:101`

```kotlin
val bucket = buckets.getOrPut(obj.hashCode()) { mutableListOf() }   // <-- hashes first
if (bucket.any { it === obj }) return false                        // <-- identity check second
```

Any node with a structural `hashCode` — a `data class`, `List`, `Set`, `Map` — recurses forever
inside `hashCode()` and dies *before* the `===` check can report the cycle. The guard cannot survive
the thing it exists to guard against.

Reachable from ordinary use: `MaxMemoryUsageBehaviour` estimates on every put **and every hit**
(`FastCache.kt:380-381`, actions at `:714`/`:726`). Caching two data classes with a parent/child
back-reference turns every `get()` into a `StackOverflowError` thrown out of the cache.

Introduced by the rework — the previous JS implementation used a `WeakSet`, which matches by
reference and never hashes. The existing test `"a cyclic graph still terminates"` is **vacuous**:
`EstCycle` is a plain class with identity `hashCode`, the one shape that cannot fail.

Fix: bucket on an identity counter, or scan a plain list with `===`. Add a data-class cycle to the spec.

### A2. One throw kills the whole maintenance loop, permanently and silently `[verified]`
`FastCache.kt:604-625`

`ActionProcessingLoop.run()` calls `behaviour.process(cache, updates)` inside `while (true)` with no
`try`/`catch`. One exception ends the coroutine for good: no expiry, no max-entries eviction, no
memory bound, no statistics, no refresh. `get`/`put` keep working, so the cache silently grows
without bound.

**Reachable trigger — a user `onEviction` handler that throws.** `Builder.onEviction` (`:83`) ->
`evictionListeners` (`:662`) -> invoked at `:757` inside `removeSilently`, called from `process` at
`:220`, `:279`, `:339`, `:424`, `:582`.

(The agent that reported this proposed an index-desync trigger instead; that one is `[unreachable]`.
The mechanism is the same, the trigger above is the one that matters.)

Blast radius checked, not assumed: `Cache.defaultCoroutineScope` is
`CoroutineScope(Dispatchers.Default + SupervisorJob())` (`Cache.kt:25`), so a dead loop does **not**
cancel sibling caches. Contained to one instance.

Compounding, on the same path (`FastCache.kt:757`):
- eviction listeners run **inside `sync { }`**, i.e. user code under the cache lock;
- one throwing listener skips the remaining listeners, including the statistics recorder (`:665`);
- once the loop is dead, `lastActions` grows without bound and **pins every value ever read or
  written** — a retaining leak layered on top of the disabled eviction.

### A3. `getOrPut` re-runs the provider for a cached miss `[verified]`
`NullableCache.kt:56`

```kotlin
entries[key]?.decode() ?: provider().also { entries[key] = it.encode() }
```

`MISSING` is non-null, so `?.decode()` yields `null` and the elvis fires — the provider runs again.
The outer fast path (`:52`) handles the sentinel correctly, so it only bites in the window between
the fast read and taking the lock.

`getOrPutAsync` does the same double-check **correctly** at `:68` (branches on presence, not on
decoded nullness), so this is an oversight in one of two near-identical bodies.

Reachable: `SharedRepoClassLookup` is a kontainer **singleton** (`vault_module.kt:40`), shared
across requests, so two threads really do race here. An agent reproduced `provider calls = 2` for a
null result vs `1` for a non-null one with a latch-controlled copy of the method.

Fix: mirror the async path — branch on presence, never on decoded nullness.

---

## B. Confirmed, needs a decision

### B1. `getOrPutAsync` holds one non-reentrant `Mutex` across the provider `[verified]`
`NullableCache.kt:60-68`. The mutex is per-instance, not per-key, and wraps an arbitrary suspending
provider. `kotlinx.coroutines.sync.Mutex` is not reentrant, so a provider that re-enters
`getOrPutAsync` suspends forever.

No current in-repo path nests — a landmine, not a live break. But vault documents this as the
`RefCodec` resolution path (`caching.kt:19-20`) and Ref resolution nests by nature. Separately, a
request resolving 50 refs serialises them all behind one another's DB round trips, defeating the
per-request cache.

### B2. On Native every custom object estimates to a constant 32 bytes `[reported]`
`nativeMain/ObjectSizeEstimatorPlatform.kt:9` -> `ObjectSizeEstimator.kt:193`. `getFieldsOf` returns
`null`, so any non-collection object is charged `objectHeader + 2 * pointerSize`. A
`data class Doc(val body: ByteArray)` holding 10 MB is charged 32 bytes and `maxMemoryUsage` never
evicts. The `2L` is an unexplained magic number.

Decision needed: document `maxMemoryUsage` as JVM/JS-only, require a caller-supplied sizing function
on Native, or accept it.

### B3. On JS every number is charged 1 byte `[reported]`
`ObjectSizeEstimator.kt:128`. `is Byte` reportedly compiles to `typeof obj === 'number'`, swallowing
`Int`/`Short`/`Float`/`Double` and leaving the later branches dead. Agent read four unreachable
`typeof obj === 'number'` tests in sequence out of the compiled output. **Not yet confirmed by a
running test — worth one, since it is an 8x under-estimate on the browser target.**

### B4. Collections, maps and boxed primitives are systematically under-counted on the JVM `[reported]`
`ObjectSizeEstimator.kt:168-183`. Charges `objectHeader + n*pointerSize` for a collection and
`+ 2n*pointerSize` for a map, omitting the backing array, its growth slack, and the
`HashMap.Node`/`LinkedHashMap.Entry` per entry (~40 bytes each). Elements are charged their raw
value size, not the box.

Direction and mechanism are solid; exact multipliers are JDK/layout dependent. This is the module's
largest systematic error, and it is in the unsafe direction (a `maxMemoryUsage(100 MB)` cache over
map-shaped values holds several hundred MB). Errors elsewhere point the other way (Strings
over-counted under compact strings), so they do not predictably cancel.

### B5. `ValueSortedMap` declares `MutableMap` but honours almost none of it `[unreachable]`
`ValueSortedMap.kt:66`, `:69`, `:72`, `:27`. `keys` is a live view of the key index only (mutating it
desyncs `sorted`); `values` and `entries` are detached copies, so `setValue` and removal through them
are silently discarded; `SimpleEntry` and the map itself lack `equals`/`hashCode`.

**All four collapse into one fix.** The class is `internal` and `FastCache` uses exactly six members —
`set`, `get`, `remove`, `size`, `isNotEmpty`, `ascending()`. `keys`, `values`, `entries` and
`descending()` are never called from production code. Dropping `: MutableMap<K, V>` deletes all four
defects instead of repairing them — the ChildFinder / `kMutableMapType` / `types.kt` pattern.

### B6. `Cache<K, V>` does not bound `V : Any`, and its own accessors then disagree `[reported]`
`Cache.kt:52`. `Cache<Any?, Any?>` is legal and slumber instantiates it (`DataClassSlumberer.kt:70`).
`FastCache` uses `map[key] != null` as its presence test (`:714`, `:726`) while `put` stores null
unconditionally. After `put(k, null)`: `size == 1` and `keys` contains `k`, but `has(k) == false`,
`get(k) == null`, and `getOrPut` re-runs the producer forever.

Same root cause as the null-value hole agent 1 flagged at `FastCache.kt:755`, where `map.remove(key)`
returning null cannot distinguish absent from present-and-null, so a null entry evicts without
notifying listeners or counting a statistic.

---

## C. Confirmed, mechanical

- **C1** `ObjectSizeEstimator` class KDoc claimed the visited set "guards against infinite loops
  caused by circular references". It does not (A1). Already corrected by the agent. `[verified]`
- **C2** `vault/caching.kt:81` explains the `MISSING` sentinel with "`ConcurrentHashMap` forbids null
  values". `NullableCache` is backed by `mutableMapOf` + `RunSync`/`Mutex` (`NullableCache.kt:22-26`),
  not a `ConcurrentHashMap` — and `ConcurrentHashMap` is not imported in `caching.kt`, so the
  `[...]` link is dead. Wrong fact **and** a style-rule violation, in an already-reviewed module.
  `[verified]`
- **C3** `FastCache.kt:100` — `behaviours` is handed to the built cache **by reference**, so reusing
  the builder afterwards mutates an already-built cache. `[reported]`
- **C4** `descending()` freezes its start index at iterator construction (`ValueSortedMap.kt:136`),
  so consuming it after the map shrinks throws `IndexOutOfBoundsException`. `ascending()` re-reads
  `sorted.size` and does not. Test-only reachability. `[verified]`
- **C5** `put`/`remove` silently skip a `binarySearch` miss (`ValueSortedMap.kt:93`, `:117`), turning
  a broken ordering invariant into permanent silent orphans. Trigger needs an inconsistent
  `Comparable`; `T` is `Long` at all four call sites. `error()` is the cheap correct fix. `[verified]`
- **C6** JVM `getFieldsOf` swallows `InaccessibleObjectException` in a blanket
  `catch (_: Throwable)` (`jvmMain/ObjectSizeEstimatorPlatform.kt:24-27`), returning an **empty
  non-null** list. So the `?:` fallback is not taken and `Instant`, `LocalDate`, `UUID`,
  `BigDecimal`, `StringBuilder` are all charged exactly 16 bytes. `[reported]`
- **C7** No recursion depth bound in `estimate` (`ObjectSizeEstimator.kt:142`, `:197`). Distinct from
  A1 — bites even with identity hashing. JS stack limits are ~10k frames. `[reported]`
- **C8** JVM reflection follows synthetic fields (`jvmMain/...Platform.kt:14-15`) — only `static` is
  skipped, so `this$0` and lambda captures are walked. Caching a value holding an inner-class or
  lambda reference can charge an entire Kontainer service graph to one entry. `[reported]`
- **C9** Full reflective walk with `setAccessible` on **every cache hit**, nothing memoized
  (`jvmMain/...Platform.kt:14-22`). Agent measured 0.18 us per plain object and **7.3 us** per object
  with inaccessible fields (exception stack-trace fill dominates). `[reported]`
- **C10** `FastCache.clear()` does not tell the behaviours (`:706`) — their tracking maps, `totalSize`
  and refresh timestamps survive, and pending actions dropped there are lost to statistics.
  `[reported]`
- **C11** `NullableCache.getOrPut` runs the provider inside `RunSync(lock)` (`:58`). On Native
  `RunSync` ignores its `lock` argument and uses **one process-wide spin lock**
  (`common/nativeMain/RunSync.kt:15`) — so a slow provider makes every other Native thread burn CPU.
  It *is* re-entrant (`LockDepth`), so the deadlock half of this is already mitigated by design.
  `FastCache.getOrPut` deliberately does the opposite and computes outside the lock (`:707-709`).
  `[verified]`
- **C12** JS `js("typeof v === 'function'")` reaches for a Kotlin local by name
  (`jsMain/...Platform.kt:22`). Works today; if the compiler ever renames `v`, `typeof <undeclared>`
  returns `"undefined"` rather than throwing, so the check silently inverts. `jsTypeOf(v)` is the
  supported form. `[reported]`
- **C13** JS reads `dyn[k]`, which invokes own accessor properties — estimating an object can run
  user code, and it can throw out of a cache put. JVM `f.get` does not. `[reported]`

---

## C-bis. FastCache, remaining mechanical items `[reported unless marked]`

- **F9** statistics counters (`:494`) and `totalSize` (`:378`) are plain fields, written on the loop
  thread and read by `snapshot()`/the getter from caller threads — no `@Volatile`, atomic or lock.
  A non-volatile `Long` write is not guaranteed atomic (JLS 17.7). Tests only observe after a
  `delay`, which inserts enough happens-before to hide it.
- **F13** `getOrPut`'s "already there" branch (`:751`) records no `ReadAction`, so an access served
  there is invisible to statistics *and* to `ExpireAfterAccess`/`MaxEntries` — the key ages as if
  never touched and is evicted early.
- **F14** no config validation: `maxEntries` silently rewrites a non-positive value to `1`;
  `maxMemorySize <= 0` is accepted (then evicts everything every iteration); `hardTtl < refreshAfter`
  is accepted; every TTL goes through `inWholeMilliseconds`, so a sub-millisecond `Duration`
  truncates to `0` and expires instantly.
- **F4** `refreshingKeys` (`:550`, `:609`) is a plain `mutableSetOf` mutated from the refresh
  coroutine on a `Dispatchers.Default` thread while `process()` reads and writes it — unsynchronised.
  The same `catch (_: Exception)` swallows `CancellationException` (an `Exception` on JVM) and every
  loader error without a trace.
- **F5** hard-TTL eviction and `remove()` do not cancel an in-flight refresh (`:595`, `:608`), which
  then unconditionally `put`s the value back — so the hard TTL is not hard. The existing test
  (`FastCacheRefreshAfterWriteSpec.kt:74`) asserts at 700 ms and ends before the loader finishes, so
  it is **vacuous with respect to exactly this**.
- **F15** `ObjectSizeEstimator.estimate` runs on **reads** as well as writes (`:405`) — a full
  recursive graph walk per read-touched key per loop iteration.
- **F16** eviction listeners run user code inside `sync { }` (`:789`). On Native `RunSync` is one
  process-wide spin lock, so a slow handler stalls every `RunSync` caller in the process.
- **F17** LRU resolution is one loop iteration (`:320`) — every action in a batch shares the same
  `now`, so ties break on batch arrival order, not real access time. Plausibly intended given the
  class KDoc's performance framing; flagged, not filed. `FastCacheMaxEntriesEvictionSpec.kt:49`
  only passes because it sleeps 200 ms between puts.

## C-ter. Test-suite hygiene (outside the reviewed files)

- `FastCacheEntryExpirationSpec.kt` contains a class named `FastCacheExpireAfterAccessSpec`;
  `FastCacheMaxEntriesEvictionSpec.kt` contains `FastCacheMaxEntriesSpec` — filename/class mismatches.
- `FastCacheMaxMemoryUsageSpec.kt:188` — the randomized `totalSize` invariant, i.e. **the one test
  that would expose F8** — is `.config(enabled = false)`.
- No test exercises `refreshAfterWrite` together with any eviction behaviour (F1), behaviour state
  after `clear()` (F6), or a nullable `V` (F7).

---

## D. Test-quality findings

- `"a cyclic graph still terminates"` is **vacuous** — uses the one class shape that cannot trigger
  A1 (see above).
- `NullableCacheSpec` is entirely single-threaded, so the single-flight claim it exists to protect is
  untested, and the negative-caching test passes only via the fast path while the broken branch
  (A3) is never entered.
- Both estimator specs are largely **relative** assertions (`shouldBeGreaterThan 0L`, or comparing
  two estimates). On Native all four dedup/identity/cycle tests pass while every probe object is
  charged the same constant (B2). Nothing pins what a data class actually costs.
- `ValueSortedMapSpec.kt:129` maps entries to `Pair` before comparing, dodging the missing
  `equals`/`hashCode` rather than pinning it.
- `CacheExtensionsSpec` never exercises a nullable-`V` cache, so B6 is invisible to it.

---

## E. Probed and CLEAN — recorded so the next pass does not re-tread

**ValueSortedMap** — tie handling is correct (monotonic `id` tie-breaker means comparator-equality
implies node identity, so `binarySearch` can never drop a sibling); value re-insert removes the old
node before inserting; `Node.sort` is snapshotted at insert so mutating the value cannot corrupt the
ordering; `clear` correctly does **not** reset `nextId`; no `!!`, no unchecked casts; multiplatform-safe.

**ObjectSizeEstimator** — no realistic overflow (every path widens to `Long` before multiplying); the
per-call visited set is correct and stateless across calls, which was the rework's actual goal; `===`
identity comparison is the right choice; all eight primitive-array branches present and correctly
ordered before `is Array<*>`; JVM type-check order is a correct total order; the `Any::class.java`
superclass walk terminates; estimating key and value separately double-counts a shared object, which
is the **safe** direction; `wasmJs` is not a target so no `actual` is missing.

**NullableCache / Cache** — the sentinel is sound (`V : Any` means no legitimate value can be
`MISSING`; the single `@Suppress("UNCHECKED_CAST")` is justified and unreachable for the sentinel);
no `!!`; a throwing provider caches nothing on both paths; `withLock` releases on cancellation; lock
coverage is uniform across `size`/`clear`/`has`/`get`/`put`; the `Cache.kt` extensions are single
reads, not get-then-put races; `NullableCache` owns its map rather than wrapping a cache, so there is
no underlying state to drift; unbounded growth is real but documented and both vault consumers are
bounded.

**FastCache** — `Cache.defaultCoroutineScope` is `SupervisorJob`-backed, so a dead loop is contained
to one cache instance; the `WeakReference` + capture-nothing pattern in `init` is correct and
deliberate; the refresh path *is* wrapped in `try`/`catch` (`:586-592`), unlike the behaviour loop.

---

## Deferred to the slumber pass (not cache findings)

- `DataClassSlumberer.Cached.slumber` (`DataClassSlumberer.kt:122-126`) keys the cache on `data`
  alone and ignores `context`. If two `Slumberer.Context`s can produce different output for the same
  value, whichever ran first wins for both.
- `SlumberCache.remove` (`:112`) is the only override that skips the `excludedClasses` guard.

---

## MECHANICAL ROUND — DONE 2026-07-30

| Fix | Where | Mutation | Killed by |
|---|---|---|---|
| Cycle guard no longer hashes the tracked object (A1) | `ObjectSizeEstimator.kt:93` | reinstated `obj.hashCode()` | `ObjectSizeEstimatorImplSpec` (2) |
| `behaviour.process` guarded, cancellation rethrown (A2) | `FastCache.kt:639` | removed the guard | `FastCacheLoopResilienceSpec` (2) |
| `getOrPut` branches on presence, not decoded nullness (A3) | `NullableCache.kt:54` | restored the elvis | `NullableCacheConcurrencySpec` (1) |
| `ValueSortedMap` no longer declares `MutableMap` (B5) | `ValueSortedMap.kt` | — (deletion) | n/a |
| `ascending()` fails fast on comodification (F6) | `ValueSortedMap.kt:130` | removed the check | `ValueSortedMapSpec` (1) |
| `binarySearch` miss now `error()`s instead of orphaning (C5) | `ValueSortedMap.kt:49` | — (assertion) | n/a |
| `descending()` snapshots instead of freezing an index (C4) | `ValueSortedMap.kt:158` | — | n/a |
| `build()` copies the behaviour list (C3/F11) | `FastCache.kt:106` | shared the list again | `FastCacheBuilderIsolationSpec` (1) |
| `maxEntries`/`maxMemorySize` validated (F14) | `FastCache.kt:301`, `:369` | removed `require` | `FastCacheConfigValidationSpec` (1) |
| Dead `Entry.value` dropped from all four behaviours (F12) | `FastCache.kt` | — (deletion) | n/a |
| `jsTypeOf` instead of `js("typeof v === …")` (C12) | `jsMain/…Platform.kt:24` | — | n/a |
| vault KDoc: wrong mechanism + dead `[ConcurrentHashMap]` link (C2) | `vault/caching.kt:81` | — | n/a |

**6 mutations, 6 killed, each hitting exactly its own spec.** No mutation killed an unrelated test.

**One probe was wrong first time, and the correction matters.** The initial builder-isolation test
asserted a *downstream* effect (that a post-`build()` `maxEntries(1)` would evict). The mutation
**survived** it — that eviction did not reproduce within 300 ms. Re-pointing the test at the actual
invariant (`cache.behaviours.size`) killed the mutation immediately. So the shared-list defect is
real, but its observable consequence is smaller than the agent reported. Recorded rather than
smoothed over: the first version of that test certified nothing.

New specs: `FastCacheLoopResilienceSpec` (2), `FastCacheConfigValidationSpec` (2),
`FastCacheBuilderIsolationSpec` (1), `NullableCacheConcurrencySpec` (2, jvmTest — needs real
threads), plus 2 non-vacuous cycle tests and 1 comodification test.

Also replaced two `ValueSortedMapSpec` tests that only existed to exercise the deleted
`keys`/`values`/`entries`, one of which dodged the missing `equals`/`hashCode` by mapping to `Pair`.

### Test evidence

- jvmTest **126**, jsBrowserTest **115**, linuxX64Test **114** — 0 failures on all three.
  Every new common spec confirmed present in the JS results, not just the JVM ones.
- Native results were re-run from scratch: the ones on disk were stale by a day and would have been
  misleading.
- Dependent modules after deleting their compiled test classes: slumber **1232**, vault **285**, 0 failures.
- Compile sweep (`compileKotlin{,Test}{Jvm,Js,}`) — 0 errors.

## Follow-ups

- [ ] **DESIGN, awaiting the maintainer** — the five questions: eviction notification (root cause),
      `V : Any` bound, `maxMemoryUsage` credibility, `NullableCache` per-key single-flight, refresh
      cancellation. Agreed approach: **write the failing tests first**, then decide.
- [ ] `VisitedSet` is now O(n²) by identity scan. Correct, but an identity-keyed set
      (`IdentityHashMap` / JS `Set` / native fallback) needs an expect/actual — deferred with the
      estimator design question.
- [ ] Deferred mechanical, because they touch code the design fixes will rewrite: statistics/`totalSize`
      memory visibility (F9), `getOrPut`'s missing `ReadAction` (F13), JVM `Class -> Field[]`
      memoization (C9), the blanket `catch (_: Throwable)` in JVM `getFieldsOf` (C6).
- [ ] Test-suite hygiene: two filename/class mismatches, and re-enable
      `FastCacheMaxMemoryUsageSpec.kt:188` once `totalSize` is correct.
