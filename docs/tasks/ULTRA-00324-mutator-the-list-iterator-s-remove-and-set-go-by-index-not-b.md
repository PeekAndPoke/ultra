# ULTRA-00324: Mutator: the list iterator's remove() and set() go by index, not by row

> **Status (2026-11-14): PAUSED.** Not scheduled; recorded on the maintainer's word (2026-11-14: "record future tickets").
>
> **Part of:** ULTRA-00322 (Epic: Mutator, future work (an experiment, not in use))

## Why this exists

Element mutators of a `ListMutator` find their row by token since ULTRA-00234. The iterator's own `remove()` and
`set()` still go by its cursor index (`mutator/core/src/commonMain/kotlin/ListMutator.kt`, class `It`). When the list
changes between `next()`/`previous()` and `remove()`/`set()`, they hit another row, silently. An `ArrayList` throws
`ConcurrentModificationException` here. The behaviour is older than ULTRA-00234.

Traced by the round-3 security reviewer of ULTRA-00234; track C confirms it by test and pins it in
`ListMutatorKnownDefectsSpec`:

- `[a, b, c]`; `it.next()`; `it.next()` (returned b); `m.removeAt(0)`; `it.set(x)` gives `[b, x]` (c overwritten).
- The same, then `it.remove()` gives `[b]` (c removed).
- `[a, b]`; `it.next()` (a); `m.add(0, z)`; `it.remove()` gives `[a, b]` (z removed); `it.next()` returns a again.

## Decide before implementing

- (a) keep the last returned row's token, resolve `remove()`/`set()` through it, drop when the row is gone, re-anchor
  the cursor (the coordinator's recommendation: the same rule as for element mutators);
- (b) fail fast with a modification counter, like `ArrayList`;
- (c) document it.

Read ULTRA-00323 (design for tracking collection elements) first: it may settle this.

## Acceptance

- The three scenarios give the decided result on JVM and JS; the pins leave `ListMutatorKnownDefectsSpec`.

## History

- 2026-11-14: created on the maintainer's word.
