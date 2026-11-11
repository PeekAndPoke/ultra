# ULTRA-00307: Streams: a value published during a notification; a handler removed during one

> **Status (2026-11-11): IN PROGRESS.** Found by the streams operator audit of 2026-11-11 (the maintainer: "revisit
> all stream operators for defects. Write tickets for all defects and fix them right away."). Both MAJOR.

**Security-critical:** no

## Why this exists

`notifyHandlers` (`ultra/streams/src/commonMain/kotlin/streams.kt:28`) loops over a snapshot of the handlers with the
value it was called with.

**F1: a newer value is overtaken by the older one.** A handler that sets the stream again (a clamp) starts a nested
notification with the new value; the outer loop then goes on with the old one, so the handlers after it end on the
old value:

```kotlin
val s = StreamSource(0)
s.subscribeToStream { if (it < 0) s(0) }   // subscribed first
val d = s.map { it }
s(-5)
// s() == 0, d() == -5 while subscribed; a distinct() downstream keeps -5
```

Every publisher has the pattern: `StreamSourceImpl.invoke(next)`, `StreamWrapperBase.publish`, `StreamCombinator`,
`cutoff`, `switchMap`.

**F2: a handler removed during a notification is still called** with that value. User handlers run after their
`Unsubscribe` returned, and operators that tear down on the last unsubscribe get a value afterwards:
- `sampled` stops ticking for good: its `every` handler from the snapshot sets `interval` after `stopListening()`
  reset it, so the next listening sees "the same interval" and asks for no tick.
- `switchMap` returns a stale `invoke()`: its inner handler from the snapshot sets `hasValue = true` after `stop()`.

`cutoff` already guards against F2 locally (`sourceCount`).

## The fix

- F2: `notifyHandlers` skips a handler that is no longer in the live set (`ownEntry` makes every entry unique, so
  membership is exact).
- F1: each publisher counts its publishes; `notifyHandlers` stops a notification once a newer one of the same publisher
  started. The nested notification has already reached every live handler with the newer value. Earlier handlers saw
  both values in order; later ones see only the newer. That is "latest wins" for synchronous re-entrant writes, which
  is a change of delivery: a `history` downstream records only the newer value of such a write.

## Acceptance

- Specs (commonTest) for F1 on a source, a map, a combinator; F2 on a user handler, `sampled`, `switchMap`; each red
  before the fix.
- `:ultra:streams:allTests` green; the Kraft and Klang users of streams compile and their tests are green.

## History

- 2026-11-11: created from the operator audit (findings F1, F2).
- 2026-11-11: implemented (`Notifier`, `notifyHandlers` skips removed handlers). `NotificationSpec`: 6 cases, all red on the old code; mutations M3 (membership check removed) and M4 (superseded check removed) red. Kraft core 216/216 green. Review gate next.
