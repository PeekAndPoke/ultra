# ULTRA-00274: Streams: a no-op handler that captures nothing is one shared instance, so two such subscriptions share one entry

> **Status (2026-10-30): DONE.** Gate PASS 2026-10-30: the UI batch's review, clean in round 9 (record in ULTRA-00266). Found by the review of the batch; a behaviour change (one handler given twice is two subscriptions), in the CHANGELOG.

**Security-critical:** no

## Why this exists

A Kotlin lambda that captures nothing is a single instance (JS: one top-level function; JVM: invokedynamic with no
captures), and every stream keeps its handlers in a set (`StreamSourceImpl.kt:18`, `StreamWrapperBase.kt:15`). Two
subscriptions with the same no-op lambda therefore hold one entry: the second `add` does nothing, and the first
unsubscribe removes the entry both rely on, so a source computed only while listened to (fold, smooth, ticker) stops
under the other. `sampled` had this and was fixed in the UI batch (its own handler per operator). The same trap
predates it in `Stream.subscribeToStream(sub = {})` (`Stream.kt:19`) and `permanent()`.

## The work

1. Give each of those subscriptions a handler of its own (a closure over its subscription), or count subscriptions
   instead of keeping them in a set.
2. A spec per case: two such subscriptions on a fold source, the first leaving, the other still sees new values.

## History

- 2026-10-29: created from the round-3 review of the UI batch.
- 2026-10-30: IN PROGRESS.
- 2026-10-30: fixed; spec added and mutation-checked.
- 2026-10-30: DONE.
