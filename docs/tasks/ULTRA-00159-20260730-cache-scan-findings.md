# ULTRA-00159: ultra/cache: scan findings

> **Status (2026-07-30): PAUSED.** SCAN IN PROGRESS — 2026-07-30
>
> **Part of:** ULTRA-00255 (Ultra libraries epic)

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

---

## DESIGN ROUND — failing specs written 2026-07-30, awaiting decisions

`CacheKnownDefectsSpec` (commonTest, 7) and `CacheKnownDefectsJvmSpec` (jvmTest, 3).
**Deliberately RED and deliberately NOT COMMITTED** — they pin each defect so its scope is a fact
rather than a description. Every one asserts what the existing contract already promises.

| # | Assertion | Fails on | Actual failure |
|---|---|---|---|
| Q1a | a key evicted by `maxEntries` is not reloaded by `refreshAfterWrite` | all 3 | `"a"` reloaded at indexes **[0, 2, 4, 6, 8]** — five reloads in 600 ms |
| Q1b | `totalSize` stops counting an entry another behaviour evicted | all 3 | `expected 100, was 200` — exactly double, the evicted entry still counted |
| Q2 | a stored `null` is visible to `has`/`getOrPut` | all 3 | `has("k")` -> `false` while `size == 1` |
| Q3a | an estimate reflects how much data an object holds | **native only** | flat charge, `ByteArray(8)` == `ByteArray(100_000)` |
| Q3b | a `Double` costs more than a `Byte` | **JS only** | both charged 1 byte |
| Q3c | a JDK type with inaccessible fields is not charged an empty object's size | JVM | `StringBuilder` holding 200k chars -> **16** |
| Q3d | JDK leaf types are not all one constant | JVM | `BigDecimal(35 digits)` and `Instant` both -> **16** |
| Q3e | a map is charged for its per-entry nodes | JVM | overhead **16016** for 1000 entries = 16 B/entry vs ~48 B real |
| Q4 | `getOrPutAsync` does not deadlock on a nested resolve | all 3 | `TimeoutCancellationException` after 3 s — a real deadlock |
| Q5 | a hard-evicted entry does not come back when the loader returns | all 3 | it reappears |

**The platform predictions held exactly**: Q3a fails on native and nowhere else; Q3b fails on JS and
nowhere else. Both pass on the platforms where the defect does not apply, so they are pinning the
platform gap rather than a generic weakness. This is the first running confirmation of the JS
number-sizing claim, which until now was only read out of compiled output.

**Two probes were wrong on the first attempt** and are recorded because the corrections change what
the findings mean:

- **Q5 initially PASSED.** A single late assertion missed it: the resurrected entry gets a *fresh*
  write timestamp, so the hard TTL evicts it again ~200 ms later and a late snapshot shows it absent.
  The resurrection is real but transient — it needs polling, not a delay-then-check.
- **Q3e initially PASSED** against a total-size threshold, because the `String` keys dominate the
  total and hid the container overhead completely. Isolating overhead (`estimate(map) -
  sum(estimate(k) + estimate(v))`) is what makes the 16-vs-48 bytes/entry gap visible.

---

## DESIGN ROUND — RESOLVED 2026-07-30

Maintainer decisions, and what was implemented for each.

### Q1 — evictions are now announced (agreed)

`removeSilently` records a `RemoveAction`, so **every** behaviour learns about an eviction, not just
the one that asked for it. Every behaviour already handled `RemoveAction`, so the notification path
did not have to be invented. It also now returns whether it actually removed anything.

Fixes Q1a (refresh no longer reloads evicted keys) and Q1b (`totalSize` stops counting freed bytes).

### Q2 — the cache is null-tolerant, `V : Any` NOT introduced (maintainer's preference)

Rather than bounding `V`, `FastCache` now distinguishes "absent" from "present and null" throughout —
`has`, `get`, `getOrPut` and `removeSilently`.

The cost is nil for caches that never store null: `map[key]` returning non-null already settles
presence, so only the null case pays the extra `containsKey`. Implemented as a private
`lookup(key): Holder<V>?`, where `null` means absent and a `Holder` may legitimately carry `null`.

This keeps slumber's `Cache<Any?, Any?>` (`DataClassSlumberer.kt:70`) working untouched, which the
`V : Any` route would have broken.

Folded in: `getOrPut`'s "another thread won" branch now records a `ReadAction` (F13). Without it a
key served only through that branch aged as if never touched and was evicted early.

### Q3 — documented, not fixed (maintainer: "put a comment")

`ObjectSizeEstimator`'s KDoc now states the accuracy limits per platform, with the measured figures:
native charges every custom object a flat 32 bytes; JS charges every number 1 byte; JVM under-counts
map entries ~3x and charges JPMS-inaccessible types (`Instant`, `BigDecimal`, `StringBuilder`) a flat
16 bytes. `Builder.maxMemoryUsage` says outright that it is a budget in estimated bytes, not a memory
bound.

The five Q3 specs were **deleted** — they pinned defects that are staying, and leaving them red or
inverting them into characterisation tests would have blessed the behaviour as contract.
The measurements survive here and in the KDoc.

### Q4 — per-key single-flight (agreed)

The process-wide `kotlinx.coroutines.sync.Mutex` is gone. `getOrPutAsync` now claims a key under the
short existing lock and runs the provider with **no lock held**, so nested resolution works and
unrelated keys do not serialise. Concurrent callers for one key join a `CompletableDeferred`.

A joiner whose owner failed retries rather than inheriting an exception raised on another call
stack, so a failed provider does not poison the key.

**Replacing the mutex removed a guarantee it gave for free**, so three tests were added to show the
replacement still provides it: single-flight for a non-null result, for a null result, and recovery
after a failing provider.

### Q5 — refresh re-checks membership (agreed, "if possible")

Possible and cheap: a private `putIfPresent`. A refresh whose loader outlives the hard TTL no longer
resurrects the entry. Same protection for an explicit `remove()` racing an in-flight refresh.

Folded in while there: the refresh `catch` no longer swallows `CancellationException`, and a failed
refresh is reported instead of vanishing (F4, partially).

### Mutation results — design round

| Mutation | Killed by |
|---|---|
| M7 stop recording `RemoveAction` | `Q1a` **and** `Q1b` — both root-cause symptoms, correctly |
| M8 presence test back to `value != null` | `Q2` |
| M9 restore the mutex-based `getOrPutAsync` | `Q4` |
| M10 refresh puts back unconditionally | `Q5` |
| M11 never join an in-flight provider | `NullableCacheConcurrencySpec` (2) |

**5 mutations, 5 killed**, each hitting exactly its own test and nothing else.

### Test evidence — design round

- cache: jvmTest **134**, jsBrowserTest **120**, linuxX64Test **119** — 0 failures.
- slumber **1232**, vault **285** — 0 failures, after deleting their compiled test classes.
- Compile sweep across every module and target — 0 errors.

### F3 — RESOLVED 2026-07-30 (second pass)

All five ask-first sites wired to `removeSilently`'s return value: the two TTL `evict()`s, the
`MaxEntries` and `MaxMemory` evict+loop pairs, and `RefreshAfterWrite`'s hard-TTL loop (which
forgot `writeTimestamps` before asking — the second unhealable site alongside `ExpireAfterWrite`).

The re-read sharpened the reachability picture first: a decline implies a pending action, and that
action reaches every behaviour next round — so behaviours that re-track on reads (`ExpireAfterAccess`,
`MaxEntries`, `MaxMemory`) self-healed one iteration later, while `ExpireAfterWrite` (ignores reads
by contract) and `RefreshAfterWrite` never recovered: **immortal, never-refreshed entries**.
`MaxMemory` additionally decremented `totalSize` before asking (transient under-count).

The naive one-line fix is a trap: `while (data.size > maxEntries)` over kept-tracked entries picks
the same declined key forever. Both size/overflow loops now walk a snapshot, skip declined keys for
one round (transiently over budget, by design) and retry next iteration once the pending drains.

**Tests-first, deterministic**: `FastCacheEvictionDeclineSpec` (4, commonTest, all 3 platforms)
drives each behaviour's `process()` by hand against a cache whose loop has no behaviours — an
un-drained `get()` guarantees "declined", a drain-wait guarantees "accepted". All 4 red before the
fix, green after. First version of the maxMemory/maxEntries tests used the initial `put` as the
decline trigger, which races the loop's *first* drain — rewritten to mid-sleep `get`s like the
other two.

**Mutations: 5/5 killed** (M12 ExpireAfterWrite remove-first, M13 Refresh forget-first, M14
MaxMemory decrement-first, M15 naive while-loop, M16 MaxEntries remove-first), each by exactly its
own test. Two honesty notes:
- **M15 was caught by assertion, not the predicted hang**: in the test the drainer runs on another
  coroutine, so the naive loop busy-waits ~100 ms until the next drain frees the key and the
  mid-assert catches the wrongly-completed eviction. In production, drain and `process()` share one
  coroutine, so the same code **would** hang forever there. Mutation dead either way.
- **The `ExpireAfterAccess` site is unverifiable by design.** Its remove-first form is fully
  self-healing (any decline-causing pending is a Read/Put, and both re-track with a fresh stamp
  next round, producing identical state to ask-first). Changed to ask-first anyway — relying on
  "every decline implies a re-tracking action" couples it silently to `removeSilently`'s decline
  condition — but no test can distinguish the two forms today, so a mutation there survives. Said
  rather than papered over with a vacuous test.

Evidence: cache jvm **138** / js **124** / native **123**, slumber 1232, vault 285 — 0 failures;
compile sweep clean.

### Still open, deliberately

- **F9** statistics/`totalSize` memory visibility; **C9** JVM `Class -> Field[]` memoization;
  **C6** the blanket `catch (_: Throwable)` in JVM `getFieldsOf`; **F15** estimating on reads;
  **F16** eviction listeners running under the lock; **F17** LRU resolved only per loop iteration.
- `VisitedSet` is O(n²) by identity scan — correct, but an identity-keyed set needs an expect/actual.
- Test hygiene: two filename/class mismatches, and `FastCacheMaxMemoryUsageSpec.kt:188` is still
  `.config(enabled = false)` — it can be re-enabled now that `totalSize` is correct.

## TRIAGE ROUND — 2026-11-13 (ULTRA-00316 track B, branch `wt/cache-log`)

The READY items of the epic triage (ULTRA-00255 report, cache table), one commit each, no public API
change. Every test below was RED before its fix; "mutation" names what was broken to see it go red again.

| Item | Commit | Change | Pinned by | Mutation → red |
|---|---|---|---|---|
| F14 rest | 90b59048, **revised by 1f19eaad** (no throws, see review round 1) | Every TTL validated at construction: non-positive refused; a positive TTL below 1 ms counts as 1 ms (the clock's resolution; truncated to 0 it expired an entry in the very iteration that recorded its write; refusing a positive duration would gain nothing); `hardTtl` must exceed `refreshAfter` (compared in ms), else the hard branch always wins and nothing is ever refreshed | `FastCacheConfigValidationSpec` (4 new) | drop the positive `require` → non-positive test; drop the 1 ms floor → both sub-ms tests; drop the hardTtl `require` → hardTtl test |
| F9 | b34f1826 | Statistics: one `@Volatile` reference to an immutable `CacheStats` (single writer: the loop), so a snapshot is visible and consistent; `totalSize` `@Volatile`. Also removes the `totalSize` marker (its second half was fixed by the `RemoveAction`) | `FastCacheVisibilitySpec` (jvmTest, 2): a reader thread spins until the value changes; red on the pre-fix code | drop each `@Volatile` → its test (reader never sees the write, JIT-hoisted) |
| F4 | 710c1dc3 | `refreshingKeys` guarded by the behaviour's own lock, never nested in the cache lock; keys claimed in the scan before any launch; a failed refresh releases under the lock | `FastCacheRefreshConcurrencySpec` (jvmTest): 20 000 keys, failing loaders, 5 fresh trials; pre-fix code red 7 of 8 runs (lost releases: 39994-39999 of 40000) | claims back in the launch loop, unguarded → red (lost release). **Not pinned:** the lock in `release()` alone — that mutant survived 2/2, since with claims made before launch a release only races other releases on distinct buckets |
| C10/F6 | 1e0b9a1d | `clear()` records a `RemoveAction` per entry (the channel every removal already uses, delivered on the loop thread, so custom behaviours learn too) and keeps the pending actions | `FastCacheClearSpec` (4 new): maxEntries no longer evicts live entries for cleared ones; `totalSize` back to 0; refresh stops; pending actions counted | no `RemoveAction`s → the 3 behaviour tests; drop pending actions → the statistics test |
| C9, C6, C8 | 74f61fa4, **revised by 028ac18b** (only `this$N` skipped, see review round 1) | JVM estimator: fields per class memoized in a `ClassValue` (pins no class loader); static and synthetic fields skipped (an inner class's `this$0`, an anonymous object's captures; an indy lambda's captures are ordinary fields on a hidden class and are still walked — probed); `trySetAccessible` instead of a blanket `catch (Throwable)` | `ObjectSizeEstimatorSpec` (3 new): inner class, anonymous object, per-class lookup | walk synthetic fields → the 2 synthetic tests; a class-blind memo → the per-class test (and 6 others). **Not pinned:** the memo's speed (C9) and the absent catch (C6, no error reachable through an opened field) |
| `NullableCache.clear()` race | 13777c5b | `clear()` drops the running claims too; a provider caches only while its claim is current and releases only its own claim | `NullableCacheClearSpec` (3, commonTest, deterministic: UNDISPATCHED ordering, no timing) | unconditional insert → tests 1, 2; `clear()` keeps `inFlight` → tests 1, 2; failure path releases unconditionally → test 3 |
| `Cache.getOrPut` atomicity | 3120ada9 | KDoc: not atomic, FastCache may run the producer more than once, losers get the stored value, `NullableCache` for single-flight | – | – |
| Stale markers | 5e9bebcc (+ b34f1826, 1e0b9a1d, 74f61fa4) | Removed after checking each: onEviction null skip and refresh of evicted keys (fixed by the `RemoveAction`); `Cache.get` null (Q2 design, now in its KDoc); estimator JS numbers, JVM collections, native flat charge (Q3, stated in the class KDoc's Accuracy section; the `2L` now explained); the JPMS line of that KDoc follows `trySetAccessible`. Kept, still true and PARKed: `FastCache` F17 (`:344`), F15, `lastActions` unbounded, F16; `NullableCache` C11; estimator C7; JS C13 | – | – |
| Test hygiene | 6fe65429 | `FastCacheEntryExpirationSpec.kt` → `FastCacheExpireAfterAccessSpec.kt`, `FastCacheMaxEntriesEvictionSpec.kt` → `FastCacheMaxEntriesSpec.kt`; randomized `totalSize` test enabled. It failed on JVM and native (not JS) only because it checked 50 ms after the puts against a 50 ms loop period; with 200 ms it passed 20/20 on each of JVM, JS, native | – | eviction keeps the bytes → the randomized test (and 3 others) |

Evidence (XML, after the last commit): cache jvm **155** / js **135** / native **134**, 0 failures,
0 skipped; the two consumers slumber **1249** and vault **287**, 0 failures. Log jvm 69 (ULTRA-00149).

Seen, not caused here (later traced to tests letting their cache be collected, see round 2 outcome): two timing-tight native tests (`FastCacheRefreshAfterWriteSpec` "only one refresh
per key at a time", `FastCacheOnEvictionSpec` "onEviction receives the correct value") each failed once
under a parallel JVM + JS + native build at load ~5, and passed 3/3 alone.

### Review round 1 — 2026-11-13

Every finding was first written as a test and run on the code as it stood; CONFIRMED means that test
was red. Each commit holds the test and the fix.

| Finding | Verdict | Commit | Fix | Mutation → red |
|---|---|---|---|---|
| A: cancelled `getOrPutAsync` joiner spins | CONFIRMED (thread still spinning after 5 s) | f2646742 | wait with `join()`: no rethrow of the owner's failure, throws on the caller's own cancellation | back to `runCatching { await() }` → joiner test; bare `await()` → "owner's cancellation is not the joiner's" |
| B: `lastByKey` hides a put behind a read, a remove behind a miss | CONFIRMED, all of B1 (refresh stuck after a read in the put's batch), B2 (`expireAfterWrite` never applies after put+get), B3 (ghost after clear/remove + miss: maxEntries evicted live entries, maxMemoryUsage kept bytes) | 7309fbd2 | write-based behaviours read an internal `lastWriteByKey` (last non-read action); every behaviour treats a miss as the key being gone. `removeSilently` stays boolean: every disappearance records a `RemoveAction`, and only a later miss could hide it | `lastByKey` back in expireAfterWrite → B2; in refresh → B1; miss arm dropped in maxEntries / maxMemoryUsage / refresh → its test. **Not pinned:** the miss arm of the two expiry behaviours (a ghost there only costs a futile eviction attempt per pass, invisible from outside) |
| C: in-flight refresh overwrites a newer value; a failed refresh releases a newer claim | CONFIRMED both ("refreshed from old" replaced "fresh"; a third loader started) | 4efb3d9b | the refresh captures the value it starts from and replaces only that (identity compare-and-set); claims carry a token, a refresh releases only its own | identity check dropped → overwrite test; release ignoring the token → claim test; the F4 spec still catches unguarded claims (39998/40000) |
| Loader throwing an `Error` keeps its key claimed | CONFIRMED (jvmTest) | 54806344 | release in `finally` unless the result reached the cache | no release in `finally` → 3 tests; the old two-catch shape → exactly the Error test |
| Throwing eviction listener during hard-TTL eviction leaves claimed keys unloaded | CONFIRMED (hand-driven) | 86899928 | launches before evictions | the original order is the experiment that was red |
| `NullableCache.clear()` KDoc wrong about who gets the result | text | 78be3928 | only the caller that ran the provider; waiting callers re-run | – |
| `hardTtl` message prints raw durations | superseded | – | the throw itself is gone (maintainer decision 1) | – |
| `lastActions` TODO says "or the loop dies" | text | 4533b12f | only a cancelled scope stops the loop | – |
| Key and exception message printed to stdout | CONFIRMED (stdout captured, jvmTest) | 28c77b2a | the exception's class only, at both sites | key back in the refresh line → refresh test; `$e` back in the behaviour line → behaviour test |
| Timing-dependent tests | test only | cb8cc919 | positive conditions polled with a deadline; the F4 spec polls per-key counts; "only one refresh" checks inside a 250 ms window that cannot hold a legitimate second load (300 ms loading + 200 ms TTL) | unguarded claims → F4 spec; no deduplication → "only one refresh" |
| `onEviction` / `evictionCount` must not fire on `clear()` | NOT CONFIRMED as a defect: green on current code; test kept | 0145a81b | – | `clear()` calling the listeners → red |
| `FastCache.getOrPut` KDoc: producer across remove/clear | text | 1d5d5b9d | – | – |

Maintainer decisions applied afterwards:
- **1f19eaad TTL fallbacks, no throws.** Zero or negative TTL (`expireAfterAccess`, `expireAfterWrite`,
  `hardTtl`): "do not keep", expires at the next pass. Below 1 ms: 1 ms. `Duration.INFINITE`: never
  expires. Zero or negative `refreshAfter`: also "do not keep" - no refresh, expiry at the next pass;
  chosen over the old "reload every key on every pass", which hammers the loader's source and keeps
  nothing a TTL of 0 asks to keep. `hardTtl` not above `refreshAfter`: expires at `hardTtl`, never
  refreshed. Tests: 5 fallback tests; mutants (non-positive as "never", the zero-refresh mapping
  dropped, INFINITE capped) each red.
- **028ac18b estimator skips only `this$N`.** Probed field names: inner class `this$0` (synthetic);
  class delegation `$$delegate_0` (synthetic); suspend lambda `$big` (synthetic, beside `label`,
  `arity`, `_context`, `intercepted`, `completion` from its base classes); anonymous object `$big`
  (synthetic); a plain lambda compiles to a hidden class with `arg$1` (not synthetic). Tests: the
  anonymous-object test flipped (the 80 KB array is charged), a delegating wrapper is charged its
  delegate; mutants "skip every synthetic" and "skip none" each red.

Probe: `ObjectSizeEstimatorPlatform` is a public `expect object`, so `getFieldsOf` is public API,
reachable from outside the module.

Evidence after the last commit: cache jvm **173** / js **147** / native **146**, consumers slumber **1249** / vault **287**, log jvm **69**; 0 failures, 0 skipped.

### Review round 2 — 2026-11-13

- Implementation & code style: **CLEAN**.
- Domain expert: **CLEAN**.
- Security: **NOT CLEAN**, one MAJOR (M1). Two rounds without a clean one: verified with a test only,
  not fixed; the maintainer decides how to go on.

**M1 — a write TTL and the refresh hard TTL never apply to a key read in every pass: CONFIRMED.**
`removeSilently` declines on any pending action, reads included (`FastCache.kt` `lastActionsKeys`,
reset at the loop's swap), so a read between the swap and the eviction spares the key, and the retry
next pass meets the same condition. Non-asserting probe (JVM, default loop delay, 100 ms TTL, 5 runs
each, survival measured up to 10 s; "red" = still cached after 2 s):

| Variant | Red | Survived (ms) |
|---|---|---|
| `expireAfterWrite`, 1 thread spinning `get("a")` | 0/5 | 172, 1631, 623, 281, 522 (all past the TTL) |
| `expireAfterWrite`, 4 spinning threads | 1/5 | 8114, 753, 1023, 755, 1418 |
| hard TTL (`refreshAfterWrite(10 s, hardTtl = 100 ms)`), 1 thread | 4/5 | 3982, 1677, 8291, >10000, >10000 |
| hard TTL, 4 threads | 4/5 | 6883, 182, 2500, 2127, 4472 |
| `expireAfterWrite`, one thread flooding misses + one reader of "a" with `yield()` | 5/5 | >10000 ×5 |

The miss flood widens the window: a large batch takes long to process, and a read of the hot key
always lands in it. Pinned as a known defect (842d2c57, `FastCacheHotKeyKnownDefectSpec`, jvmTest,
the flood shape, both the write TTL and the hard TTL): asserts the key survives 2 s; 5/5 per test as
committed; turns red when `removeSilently` stops declining (checked). JS cannot open the window
(`process()` has no suspension point); native can, untested.

Fix as the reviewer proposes (a second set of keys with a pending put or remove beside
`lastActionsKeys`; `ExpireAfterWriteBehaviour.evict` and the hard-TTL eviction decline only on that;
access-ordered behaviours keep today's rule): about 15-20 lines in `FastCache.kt` (the set, its swap,
`addAction`, a parameter on `removeSilently`, the two callers), plus tests. It must flip two tests
that bless the defect today: `FastCacheEvictionDeclineSpec` "expireAfterWrite: a declined eviction is
retried once the pending read drains" and "refreshAfterWrite: a declined hard-TTL eviction is
retried, not forgotten" (both assert that a pending READ spares the entry), and the known-defect spec.
Safer shape, same size: one map in place of `lastActionsKeys`, key to "a write is pending", swapped
once, so the two views cannot drift apart. Only a pending put needs sparing (a pending remove means
the entry is gone already). Once the per-key write counter (decision for the A-B-A finding) exists,
"evict only if the write counter is still the one stamped" would replace the doorman for write-based
evictions altogether; larger, since `PutAction` is public and carries no counter.

MINORs, open:
- Unlocked redundant write at `FastCache.kt:685`.
- KDoc "only one refresh per key" at `FastCache.kt:592`.
- Three tests that no longer guard the tracking drop: `FastCacheBatchOrderSpec.kt:87`,
  `FastCacheClearSpec.kt:92`, `CacheKnownDefectsSpec.kt:25`.
- Sleeping tests: `FastCacheRefreshAfterWriteSpec.kt:27-30, 45-53, 96-100, 119-123`; the timing ladder
  `FastCacheRefreshRaceSpec.kt:52-84`.
- Identity compare is open to A-B-A (all three reviewers). Maintainer decision: a per-key write counter.
- `NullableCache.put` during a running `getOrPutAsync` is overwritten by the older result
  (`NullableCache.kt:68-72, 119-125`).
- A `getOrPutAsync` provider reaching its own key hangs; the KDoc at `NullableCache.kt:16-18` is wrong
  for cycles.
- `catch (e: Exception)` at `FastCache.kt:704, 772` lets other `Throwable`s (e.g. `VaultException`)
  through.
- A key in an exception message: `ValueSortedMap.kt:53`.
- An estimator throwing part-way through a batch leaves ghosts (`FastCache.kt:437-439, 478-485`).
- `loopDelay` has no fallback (`FastCache.kt:784`).
- The builder KDoc lacks the TTL fallbacks.
- One lost-race `getOrPut` counts as both a miss and a hit.

### Round 2 outcome — maintainer decisions 2026-11-14, applied

| Item | Decision | Commit | What changed | Evidence |
|---|---|---|---|---|
| M1 hot key outlives the write TTL and hard TTL | "yes fix it" | e0f2efb8, 75b707d4 | One map replaces `lastActionsKeys`: each pending key, and whether a put is among its actions. Write-based expiry (`expireAfterWrite`, the hard TTL) is spared only by a pending put; the access-ordered behaviours keep sparing any pending action | `FastCacheHotKeySpec` (the known-defect spec turned expectation) and the two flipped `FastCacheEvictionDeclineSpec` tests: red on the pre-fix code, green after; two new tests pin that a pending put still spares. Mutants: fix removed → the 2 hot-key and 2 pending-read tests red; a put never marked → the 2 pending-put tests red; access-ordered rule narrowed to puts → the maxMemory/maxEntries decline tests red. The hot-key spec got a scope per cache and a fixed pool of missing keys (e75d59ed) |
| A-B-A in the refresh identity compare | "only document it" if nothing breaks | a2f55876 | KDoc only | See below |
| Background refresh counts as an access | ULTRA-00321 | a2f55876 | KDoc names it: a refresh result is recorded like a put, so it counts as an access for `expireAfterAccess` and the LRU of `maxEntries`/`maxMemoryUsage`, and as a put in the statistics | behaviour unchanged |
| Dropped refresh result is silent | document | a2f55876 | KDoc: a result is dropped without notice when the entry was written, removed or evicted while the loader ran | – |

**A-B-A, worst outcome:** when the application writes back the very instance a running refresh started
from (put Y then put the original X again, or remove then put X), the refresh's compare-by-identity
still matches and it replaces X with the loaded value. A reader then gets the loaded value instead of X
until the next write or refresh (at most one refresh TTL) - i.e. what the refresh would have installed
had the write not happened, and never a miss. No documented guarantee
breaks: the docs promise no miss, one refresh at a time and retry on failure, and say nothing about an
application write beating a running refresh. So KDoc only, no per-key write counter.

MINORs from round 2:

| MINOR | Verdict | Commit |
|---|---|---|
| Unlocked redundant claim write (`FastCache.kt:685`) | CONFIRMED; it was mutation text from the round-1 F4 re-check that survived the restore and went into 4efb3d9b; an unlocked write beside locked releases, so it partly re-opened F4. Deleted | f190a0b4 |
| KDoc "only one refresh per key at a time" (592) | CONFIRMED wrong: a write frees the key, so a later refresh can run beside the older one. Corrected. **The same sentence is in the public docs** (`docs-site/src/pages/ultra/cache/refresh.astro` "Deduplication", `docs-site/src/data/llms/cache.md:123`), outside this track: needs a DOCS follow-up | a2f55876 |
| Three tests no longer guard the tracking drop | CONFIRMED: with the refresh never dropping a write time, all three stayed green (a refresh skips a key that is not cached, so "the loader is never called" held anyway). Rewritten to drive the refresh by hand with what the cache recorded (`ActionTap`), then put the key back unseen: refresh drops nothing → all three red; ignores only misses → the miss test red; `clear()` records no removals → the clear test red; an eviction records no removal → Q1a red. A first version fed everything as one batch and stayed green under the mutant (the miss superseded the put); now batch by batch | e0569dc2 |
| Builder KDoc lacks the TTL fallbacks | CONFIRMED. Added to `expireAfterAccess`, `expireAfterWrite`, `refreshAfterWrite` | 636a4db5 |
| (found here) Tests let their cache be collected while waiting for its loop | The loop holds its cache only weakly and stops once it is collected (by design). Twenty tests stopped using the cache after the put and then waited for the loop: three stalls in about 15 suite runs this round ("only one refresh per key", "onEviction receives the correct value", the loader-Error test), and negative tests ("no load", "no eviction") could pass vacuously. Shown with a latency probe: the dispatcher never stalled (max 3 ms) while the onEviction test failed 5/5; with the cache kept referenced, 3/3 green. `keepReachable(cache)` after the last wait in every such test; 3/3 green under the probe. The two "native timing flakes" recorded in the triage round are these same tests, so most likely the same cause | 04154daa |

Left for the maintainer, one-line verdicts:
- `NullableCache.put` during a running `getOrPutAsync`: **real defect**, a lost write. The provider loads
  the old value, the application puts the new one meanwhile, the provider's result overwrites it - and
  `NullableCache` never evicts, so the old value stays for the instance's lifetime.
- A provider reaching its own key: **real defect**, a permanent hang. `getOrPutAsync(a)` whose provider
  resolves `b` whose provider resolves `a` joins `a`'s own run and waits on itself. The KDoc ("may safely
  resolve other keys") is wrong for cycles, and vault reference resolution can meet cycles.
- `catch (e: Exception)` at `FastCache.kt:704, 772`: **real defect at 772**: a behaviour or eviction
  listener throwing a non-`Exception` `Throwable` (an `Error`, or `VaultException`, which extends
  `Throwable` directly) ends the loop for good, as A2 did for exceptions. At 704 a quirk: the claim is
  released (54806344), but the throwable reaches the scope's handler (on native that ends the process).
- The key in the message of `ValueSortedMap.kt:53`: **quirk, unreachable today**: only a broken
  `compareTo` raises it, and every sort key is a `Long`; the loop prints the class only.
- Estimator throwing part-way through a batch: **real defect**, budget drift: the rest of that batch is
  never applied, so its puts go uncounted and its removals leave ghost bytes (an estimate that throws,
  e.g. a JS accessor, C13).
- `loopDelay` without a fallback: **real defect for odd values**: zero or negative makes `delay` return
  at once, so the loop never suspends and holds a thread busy for good. A fallback such as "at least
  1 ms" would match the TTL decision.
- A lost-race `getOrPut` counts a miss and a hit: **quirk**, statistics only: one call adds 2 to
  `requestCount`.
- Sleeping tests (`FastCacheRefreshAfterWriteSpec.kt:27-30, 45-53, 96-100, 119-123`,
  `FastCacheRefreshRaceSpec.kt:52-84`): **quirk**, flake risk under load, no product defect.

Evidence after the last code commit: cache jvm **177** / js **149** / native **148**, consumers slumber **1249** / vault **287**; 0 failures, 0 skipped.

### Review round 3 — 2026-11-14 (top level, blind then reconciled)

- Implementation & code style: **CLEAN** (6 MINOR).
- Security: **CLEAN** (6 MINOR; "M1 is closed, including the variants I tried").
- Domain expert: **NOT CLEAN**, 1 MAJOR, 4 MINOR.

The maintainer (2026-11-14): "Yes fix the real defects in cache." Every scenario was written as a test
and run on the code as it stood first; "CONFIRMED" means that test was red.

| Finding | Outcome | Commit | Evidence |
|---|---|---|---|
| MAJOR: `maxEntries`/`maxMemoryUsage` do not bind for keys read in every pass | CONFIRMED (JVM: two hot keys with `maxEntries(1)` stay at 2, 5/5; a pool of 1 000 keys read through `getOrPut` with `maxEntries(10)` grows to 1 000, 5/5; JS cannot hit it: nothing is pending while `process()` runs). **Settled by the maintainer: soft bounds, documented** ("let us not overcomplicate things. Since this cache is called FastCache it is ok, when it is not 100% accurate. So documenting as soft bounds might be already enough here.") | 2dd51e25, bf8ce249 | `FastCacheSoftBoundSpec` pins the documented behaviour (1 s per test, 10/10 stable each); KDoc on the builder methods and both behaviours; the "for one round" comments corrected |
| B1 `NullableCache.put` during a running `getOrPutAsync` overwritten | CONFIRMED, fixed: the put wins; every caller, the provider's own included, gets the cached value (as `FastCache.getOrPut`) | 15a84c98 | mutant "provider always stores" → red |
| B2 a provider reaching its own key hangs | CONFIRMED (a → a and a → b → a hung until the timeout), fixed: the chain of keys being resolved travels in the coroutine context; reaching a key further up the own chain fails with `IllegalStateException`, the claim is released. **Not covered, in the KDoc:** a cycle across two concurrent callers (one resolving a then b, the other b then a) still deadlocks; detecting that needs a wait-for graph across callers, not small | b288d043 | mutants: check disabled, chain not propagated, check dropped from the join path → each red. The check on the owner path is reachable only after `clear()` mid-run, not pinned |
| B3 non-`Exception` throwables and `CancellationException` from user code | CONFIRMED (a behaviour throwing `CancellationException` or a plain `Throwable` ended the loop; `maxEntries(1)` + a listener throwing `CancellationException` stopped evicting; a loader's plain `Throwable` reached the scope's handler), fixed: the loop catches every `Throwable` and rethrows a `CancellationException` only when its own job is cancelled; the refresh catches every `Throwable` (a loader's `CancellationException` still ends that refresh quietly, the key is freed). **Errors:** swallowed too, by choice - common code has no `VirtualMachineError` to single out, a `StackOverflowError` from estimating a deep value is exactly what must not end the loop, a dead loop turns any failure into unbounded growth, and an `OutOfMemoryError` resurfaces elsewhere anyway | 42ae84ed | 4 mutants, each red on its tests |
| B4 a throwing listener: one eviction per behaviour and pass, listeners behind it skipped | CONFIRMED (hand-driven: 100 expired keys, 99 left), fixed: each listener in its own try | 42ae84ed | the old pin "a throwing listener starves the ones behind it" flipped; mutant → red |
| B5 estimator throwing mid-batch | CONFIRMED, fixed: a failing estimate keeps the entry at its previous size (0 when new), the batch goes on | 78c8574c | mutant → red |
| B6 `loopDelay` of 0 or below | CONFIRMED (7.2 million passes in 300 ms), fixed: runs as 1 ms, named in the KDoc | 0543b71c | mutant → red |
| C an entry gone earlier in the pass counted as spared, a live one evicted instead | CONFIRMED, fixed: `removeSilently` reports REMOVED / SPARED / ABSENT | d762d242 | mutant → red |
| C a refresh started from a value written after the batch | CONFIRMED, fixed: a key with a put pending is skipped, its claim freed | d762d242 | mutant → red |
| C surviving mutants (sticky put mark; `PENDING_PUT` in `expireAfterAccess`) | both now killed: a read after the put in the pending-put tests; a new `expireAfterAccess` decline test | d762d242 | both mutants → red |
| C sleep-timed decline tests | observe the drain with `ActionTap` | d762d242 | – |
| C `addAction` two lookups | one for a key already pending | d762d242 | – |
| C decline-spec header wrong since M1 | rewritten | d762d242 | – |
| C hung loaders in the race spec; two unneeded `keepReachable` | scope per cache, cancelled; both calls gone | dc2d90ff | – |
| C KDoc: TTL served up to ttl + two loop periods; a failed refresh serves stale and retries every pass; A-B-A for equal-valued instances; the estimator comment | written; the estimator comment now gives measured cost | 447d80e6 | – |
| D estimator cost | measured (JVM): a list of 1k strings 0.22 ms, 10k 17 ms, 30k 138 ms, 100k 1.54 s - quadratic; a response-shaped value of about 21k objects 70 ms. funktor's response cache (`maxMemoryUsage` only, slumber keys = the data-class instances, values = their serialized maps, one entry per nested object): every pass re-estimates key and value of each entry read or written in it, so a response of about 20k objects costs its root entry ~140 ms per pass plus its children; a few such responses per 50 ms keep the loop thread busy back to back | – | plan below, not built |
| D `ValueSortedMap` re-stamp | measured: re-stamping every tracked key in one pass, 1k keys 0.2 ms, 10k keys 5.8 ms (quadratic in array shifts, small constant). Report only | – | – |
| D TTLs on the wall clock | confirmed by reading (`Kronos.systemUtc` in all five behaviours). **Maintainer: "ok switch to a monotonic clock."** Done: `TimeSource.Monotonic` by default, seam `Builder.timeSource(...)` (new public method; the constructor that takes it is internal, the public one keeps its old parameters). Wall clock no longer used anywhere in the module; nothing stores or shows a wall-clock time | 8c3b3797 | `FastCacheTimeSourceSpec`: with a still `TestTimeSource`, 300 ms of real time expire nothing (they did on the wall clock: red before); mutant "ignore the given source" → red |

Stayed CLEAN in round 3 (per the reviewers): implementation and security; M1 and its variants.

**Estimator plan (maintainer decision 3; not built, waiting for confirmation):** see the round-3
report; in short: an identity set where the platform has one (JVM `IdentityHashMap`, JS `Set`, native
an identity-hash wrapper) instead of the linear scan; per collection walk the first 64 elements and
extrapolate the rest by their average; a cap of visited objects per estimate (default 10 000) beyond
which objects are charged a flat header plus pointer without descent; an iterative walk with an
explicit stack, so a deep graph never overflows; per-field/per-property failures charged flat, so an
estimate never throws; optionally size on writes only (reads reuse the stored size, F15). KDoc: what is
counted, limits per platform, cost, and how to plug in an own estimator.

Evidence after the last code commit: cache jvm **196** / js **165** / native **164**, consumers slumber **1249** / vault **287**; 0 failures, 0 skipped.

### Review round 4 — 2026-11-14 (delta, top level)

- Implementation & code style: **CLEAN** (7 MINOR).
- Domain + security: **CLEAN** (7 MINOR). All fixes in the code as recorded; M1 intact.

The MINOR batch, each written as a test and run on the code as it stood first:

| MINOR | Outcome | Commit | Mutation → red |
|---|---|---|---|
| A size bound evicted a live entry in place of one already gone that was not first in LRU order | CONFIRMED (hand-driven, both bounds: {C} instead of {B, C}), fixed: `maxEntries` takes its overshoot from `min(tracked, cache.size)`, an absent key only drops tracking; `maxMemoryUsage`, when over budget, first releases the bytes of tracked entries already gone (one presence check each, only while over budget) | 4060eae0 | overshoot from the tracked count → both maxEntries tests; no release of gone entries → the memory test |
| Refresh claims never launched when a throwable cuts `process()` short | CONFIRMED (twin keys whose `equals()` throws once in the lookup: neither key refreshed again), fixed: the launch loop releases unhandled claims in `finally`. The test's first version lost an add in a plain list from two threads (2 of 6 runs red); it uses a lock-guarded set. It then failed on JS and native, where a map may call `equals()` on the cached key: the twins share one trap since | 6b2c666c, final test fix after 9cee040b | no release in `finally` → red |
| One printed line per failing item, one inside the cache lock | CONFIRMED (a listener failing for 1 000 evictions printed 1 000 lines under the lock), fixed: failures counted per kind and exception class where they happen, printed by the loop once per kind and pass, outside every lock | e0b1ceda | the listener printing per failure → red |
| Dead `api` dependency on `ultra:datetime` | CONFIRMED unused (no source set references it), removed. Repo-wide compile sweep (JVM, JS, native main and test compiles; 88 Kotlin compile tasks re-run, among them slumber, vault, funktor, karango, monko): **zero errors, no module relied on it transitively** | 53d007b6 | – |
| Tests that do not pin what they claim | soft bound pool test counts producer runs (survived a hard-bound mutant before); two-key test waits for both readers; memory test adds an entry with a size; NullableCache two-cache test; time-source test moves time inside `eventually`; the launch-order race test **deleted** (listeners cannot throw out of `process()` any more; cut-short passes are pinned by the `equals()` test) | 06c017ad | hard bound → both soft-bound tests; `currentSize` → 0 → the memory test; cycle check ignoring the cache → the two-cache test |
| KDoc: suspended time on the monotonic clock, `timeSource` as a test hook (like Caffeine's ticker), `withContext` in `getOrPutAsync`, the cycle check's limits | written | 9cee040b | – |

Left open for the maintainer (estimate only, no change):
- A failed estimate charges a NEW entry 0 bytes (`FastCache.kt`, `MaxMemoryUsageBehaviour.handle`): a
  flat default charge, or evicting it - tied to the estimator plan (the plan proposes a configurable
  flat charge, never 0).
- The false-positive edge of the cycle check (a coroutine inheriting the context outlives the
  provider): a fix would let each chain link hold its claim's deferred and ignore completed links;
  about 10 lines.
- `Eviction.REMOVED` vs `ABSENT` could be a Boolean for some callers: left as it is.

Evidence after the last code commit: cache jvm **200** / js **168** / native **167**, consumers slumber **1249** / vault **287**; 0 failures, 0 skipped. Items 2-6 were first run on the JVM only; the final run caught the item-2 test failing on JS and native (a JVM-only HashMap detail in the test), fixed in the test.

## Changelog text

For the coordinator to put into `CHANGELOG.md`. Signature changes: only the new
`FastCache.Builder.timeSource(TimeSource)`. Dependency change: `ultra:cache` no longer exports
`ultra:datetime`. The behaviour changes below have **no compile signal**, so each is listed.

```
### ultra:cache
- Dependency: ultra:cache no longer depends on ultra:datetime. A project that used datetime only
  through cache must now declare it itself.
- FastCache TTLs run on a monotonic clock (TimeSource.Monotonic) instead of the wall clock: a step of
  the system clock no longer expires entries early or keeps them past their TTL. Time the machine
  spends suspended may not count, so an entry can outlive its TTL in wall-clock terms by the length
  of a pause (as in Caffeine and Guava). New, for tests: FastCache.Builder.timeSource(TimeSource).
- FastCache TTLs never throw; odd values fall back. A zero or negative TTL means "do not keep": the
  entry expires at the next processing pass. A positive TTL below 1 ms counts as 1 ms.
  Duration.INFINITE never expires. An entry can be served up to its TTL plus two loop periods.
- refreshAfterWrite with a zero or negative refresh TTL no longer reloads every key on every pass:
  entries expire at the next pass and are never refreshed. A hardTtl not above the refresh TTL
  expires entries at hardTtl, unrefreshed (as before).
- A loop delay of zero or less runs as 1 ms instead of a loop that never suspends.
- maxEntries and maxMemoryUsage are soft bounds (documented): an entry read or written in every
  maintenance pass is not evicted by them, so under such load the cache holds up to the number of
  distinct keys touched within one pass.
- expireAfterWrite and the refresh hard TTL apply to a key read in every pass: only a pending write
  spares an entry from write-based expiry.
- A read or a miss in the same loop window no longer hides a put or a remove. A size bound counts
  what the cache really holds: an entry already gone no longer makes it evict a live one in its place.
- FastCache.clear() tells the expiry, size and refresh behaviours and the statistics. It is not an
  eviction: onEviction handlers and evictionCount do not fire.
- No throwable from a behaviour, eviction listener or refresh loader ends the processing loop or
  reaches the scope's handler (Errors included); a CancellationException thrown by user code is
  treated as a failure. Each eviction listener runs on its own: a throwing one no longer skips the
  others. A size estimator throwing for one entry no longer leaves the memory budget off: that
  entry counts as its previous size, 0 when new.
- Failures are counted and printed once per kind and processing pass, with the count and the
  exception's class only (no key, no message), outside the cache's lock.
- refreshAfterWrite: a refresh replaces only the value it started from (by identity; for booleans,
  enums, small numbers, interned strings and JS primitives any equal value), skips a key written
  after the pass decided to refresh it, drops a refused result without notice, frees its key on any
  failure (also when a pass is cut short before launching it), and keeps serving the stale value
  while it retries every pass. A refresh's result counts as an access and as a put (open:
  ULTRA-00321).
- StatisticsBehaviour.snapshot() and MaxMemoryUsageBehaviour.totalSize are safe to read from any
  thread.
- NullableCache: a put while getOrPutAsync runs wins over the provider's result; a provider reaching
  a key its own call chain is resolving fails with IllegalStateException instead of hanging (not
  detected: a cycle across two concurrent callers, or one reached through a coroutine that does not
  inherit the provider's context; a coroutine inheriting it that outlives the provider can get a
  false IllegalStateException); getOrPutAsync runs the provider in a child scope, so coroutines it
  launches into its own context are awaited before caching, an already cancelled caller does not
  start it, and a value computed while the caller was cancelled is discarded; clear() is no longer
  undone by a running provider; a cancelled caller waiting on another caller's provider no longer
  spins.
- JVM ObjectSizeEstimator: field lookup memoized per class; an inner class is no longer charged its
  outer instance. KDoc: the estimator's cost is quadratic in the objects of a value (measured).
- KDoc: Cache.getOrPut is not atomic; Cache.get returns null for a key cached as null.
```

## Follow-ups
- [ ] ULTRA-00326 (cache docs after ULTRA-00159): the public docs say "only one refresh per key at a
      time", which is wrong (a write frees the key, a later refresh can run beside the older one):
      `docs-site/src/pages/ultra/cache/refresh.astro:74` and `docs-site/src/data/llms/cache.md:123`.
      Also to document there: soft size bounds; TTLs on a monotonic clock and `Builder.timeSource`;
      the TTL fallbacks (zero/negative, below 1 ms, `INFINITE`) and the loop-delay fallback; an entry
      served up to ttl plus two loop periods; refresh semantics (identity compare, dropped results,
      retry every pass, refresh counts as an access - ULTRA-00321); `clear()` is not an eviction;
      `NullableCache` cycles; the estimator's limits and cost, and how to plug in an own estimator.
- [ ] ULTRA-00321: should a background refresh count as an access? (maintainer: "I guess it should
      not count as access").
- [ ] Estimator: the plan in round 3 waits for the maintainer's confirmation.
- [ ] `VisitedSet` is now O(n²) by identity scan. Correct, but an identity-keyed set
      (`IdentityHashMap` / JS `Set` / native fallback) needs an expect/actual — deferred with the
      estimator design question.
- [x] Deferred mechanical, because they touch code the design fixes will rewrite: statistics/`totalSize`
      memory visibility (F9), `getOrPut`'s missing `ReadAction` (F13), JVM `Class -> Field[]`
      memoization (C9), the blanket `catch (_: Throwable)` in JVM `getFieldsOf` (C6). F9, C9, C6 in
      the triage round 2026-11-13; F13 earlier.
- [x] Test-suite hygiene: two filename/class mismatches, and re-enable
      `FastCacheMaxMemoryUsageSpec.kt:188` once `totalSize` is correct. Triage round, 6fe65429.

## History

- 2026-10-21: numbered ULTRA-00159 (was `docs/tasks/20260730-cache-scan-findings.md`).
- 2026-11-13: triage round (ULTRA-00316 track B): the READY items implemented on `wt/cache-log`,
  awaiting the coordinator's review gate.
- 2026-11-13: review round 1 findings verified and fixed; maintainer decisions on TTL fallbacks and
  the estimator's synthetic fields applied. Awaiting the next gate round.
- 2026-11-13: review round 2: implementation and domain CLEAN, security NOT CLEAN (M1 CONFIRMED,
  pinned as a known defect, not fixed). Two rounds without a clean one: waiting for the maintainer.
- 2026-11-14: maintainer decisions applied: M1 fixed; A-B-A, refresh-as-access (ULTRA-00321) and the
  silent dropped result documented; the small MINORs fixed. Awaiting round 3.
- 2026-11-14: review round 3: implementation and security CLEAN, domain NOT CLEAN (soft bounds by the
  maintainer's decision); real defects B1-B6 and the MINORs fixed; monotonic clock; estimator plan
  waiting for confirmation.
- 2026-11-14: review round 4 (delta): both passes CLEAN; the MINOR batch applied. The estimator plan
  is approved (constant-time lookups); it starts after the merge.
