# ultra/common scan — findings backlog

**Status:** IN PROGRESS — the significant items are fixed, the list below is what is left
**Plan:** none — output of a 7-agent scan of `ultra/common`, 2026-07-29
**Security-critical:** no

All 49 main-source files were documented (comment-only) and the defects below were found along the
way. Everything marked DONE was mutation-checked and is green on JVM, JS and native.

## DONE — fixed 2026-07-29

| Finding | Fix |
|---|---|
| `GetAndSet.of(...)` declared `Observable` but never called `emit` — subscribing worked, nothing ever fired | emit on set |
| `Observer.observe` passed the *observable* as its own observer (bare `this` in a member extension is the extension receiver) | interface deleted, see below |
| `emit` iterated the live set, so a callback that subscribed or unsubscribed threw `ConcurrentModificationException` — only with ≥2 subscribers, since `hasNext()` does not check modCount | iterate a snapshot |
| JVM `toFixed` used the machine's default locale, so a German-locale server rendered `1,50` where its own JS client rendered `1.50` | pinned to `Locale.ROOT` |
| Native `RunSync` was a global, non-re-entrant spin lock — any nested call spun forever | re-entrant via a `@ThreadLocal` depth counter |
| JS `WeakSet.size` returned `NaN` (`undefined + Int`) and `toSet()` threw — a JS `WeakSet` is neither sizeable nor iterable | both removed from the API |
| `MutableTypedAttributes` locked writes but not reads | reads take the same lock |
| `ComparableTo` existed twice, in `ultra/common` and `ultra/datetime` | unified on common's |

**The weak-observer machinery was deleted rather than fixed.** `Subscription` held the observer
weakly *and* the callback strongly, and any callback touching the observer keeps it reachable through
the emitter — so the advertised auto-cleanup could never fire for a realistic callback. The one
production caller (`Mutator.kt:121`) observed itself, making the weak reference inert anyway.
`Observer` had zero implementors. Subscriptions now live until `Unsubscribe` is called or the
observable is collected, which is what Kraft's `unSubscribers` pattern already does properly.

**Why `ComparableTo` was duplicated, for the record:** `ultra/datetime` declared `ultra:common` in
its **`commonTest`** block, not `commonMain`, so its main sources could not see common at all. Moved
to `commonMain` as `api` — required, because `ultra/maths` and `kraft/addons/datetime` depend on
datetime *without* depending on common.

## Open — worth a decision

- **`WeakSet` element matching is not uniform.** JVM and native match with `equals`, JS by reference
  identity. For the one real caller — `ObjectSizeEstimator.seen`, doing cycle detection — *identity*
  is what is actually wanted, so the JVM/native behaviour is arguably the wrong one: a graph holding
  two equal-but-distinct values is counted once there and twice on JS. Making JVM identity-based
  needs a wrapper (the JDK has no `WeakIdentityHashMap`). Documented, not fixed.
- **`ultra/datetime/recurse.kt` is a byte-identical copy of `common/recursion.kt`.** Same root cause
  as `ComparableTo`, and now removable since commonMain can see common. Left for the datetime pass.
- **`List.remove(element)` / `List.removeAt(idx)` are shadowed by the `MutableList` members.**
  Identical source text either mutates the receiver or builds and discards a copy, depending only on
  the declared type. Widening a field from `MutableList` to `List` silently changes behaviour with no
  warning. Renaming them would be the fix; both have many callers.
- **`EmailRegex` overflows the stack on long input** — `"a.".repeat(2000) + "a@example.com"` throws
  `StackOverflowError`, an `Error` that neither `catch (Exception)` nor `EmailAddress.parseOrNull`'s
  handler catches. Not currently reachable (`EmailAddress` caps at 254 chars first), but it is a trap
  for the next direct caller of the public helper.
- **`Placeholders.validate()` cannot see a malformed placeholder.** `"Hi {{FOO}, welcome"` validates
  as fine and ships to the user verbatim. Substitution itself is single-pass and injection-safe —
  that was probed and held.

## Open — smaller, no decision needed, just work

- `ellipsis` appends the suffix *on top of* `maxLength` (`"ab".ellipsis(1)` is 4 chars), truncates by
  UTF-16 code unit so it can split a surrogate pair, and throws on a negative length.
- `safeEnumsOf` returns a `List` but routes through a `Set`, so duplicates are silently collapsed.
- `toggle` is not order-idempotent — off-then-on moves the element to the end of iteration order.
- `toUri` is string concatenation: it appends query params *after* a `#fragment`, and collapses
  duplicate keys through `toMap()` so `listOf("tag" to "a", "tag" to "b")` loses the first.
- `recurse` is O(n²) (linear `!in` scan per step) and `flattenTreeToSet` is recursive with no depth
  bound.
- `safeEnumOf` uses a thrown-and-caught exception as its miss path, on request-supplied input.
- `TypedAttributes`' primary constructor is public, takes an unvalidated map and does not copy it, so
  `size` and `entries` can disagree and a type mismatch surfaces at the reader.

## Test evidence

- `ultra/common`: jvmTest 325, jsBrowserTest 159, linuxX64Test 153 — 0 failures
- Consumers verified after the `ComparableTo` change: datetime 5234, slumber 1219, funktor/core 772,
  karango 1649, monko 249, kraft/core-tests 394, cache 290, vault 285, model 170, maths 343 — all 0
- `WeakSetSpec` moved to `commonTest`, so it now runs on all three platforms instead of JVM only —
  which is exactly how the JS defects would have been caught
- New: `UtilsSpec`, `NumbersJvmSpec`, plus additions to `ObservableSpec`, `GetAndSetSpec`,
  `MutableListsSpec`, `ListsSpec` covering functions that had **no** tests (`hasSubscriptions` and
  `shift` had neither tests nor callers; `unsubscribeAll`, `pop`, `prepend`, `modifyIf` were used but
  untested)
