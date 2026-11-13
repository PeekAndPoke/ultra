# ULTRA-00234: ListMutator.subList() throws UnsupportedOperationException

> **Status (2026-11-13): IN REVIEW.** implemented on `wt/mutator` (track C of ULTRA-00316) as a read-only snapshot;
> the review gate is next
>
> **Part of:** ULTRA-00255 (Ultra libraries epic)

## Why this exists

`ListMutator.subList()` throws (`mutator/core/src/commonMain/kotlin/ListMutator.kt:260`). Stdlib functions such as
`chunked()` call it internally, so even indirect use crashes. Collection mutators are supposed to implement their
`MutableList`/`Set`/`Map` contracts fully; this is the one known gap. Moved here from CLAUDE.md "Project facts"
(Rules Retro, 2026-10-23); `mutator/core/CLAUDE.md` keeps a one-line pointer.

## Probe (2026-11-13)

The `chunked()` claim above is wrong. `ListMutator` is not `RandomAccess`, so the stdlib's `chunked` and `windowed`
take the iterator path. `ListMutatorSubListProbeSpec` (`mutator/core/src/sharedTest/kotlin/`), on both the JVM and JS:

- `chunked(2)`, `windowed(2)` and `windowed(2, 2, partialWindows = true)` work.
- `subList(0, 2)` throws `UnsupportedOperationException` (pinned in `ListMutatorSpec`).
- `slice(0..1)` throws: the stdlib's `List.slice(IntRange)` calls `subList`.

When `subList` is implemented, the `slice` case flips and must be rewritten. *(Done: it now pins that `slice` works.)*

## Decide before implementing

- A live, write-through view (the contract) or a read-only snapshot. Mutator is not battle-tested; the maintainer decides.
- **Decided (maintainer, 2026-11-13): "readonly snapshot ok".**

## The work

- ~~Implement `subList` as a live view that writes through to the parent mutator, per the `MutableList` contract.~~
  Superseded by the decision: a read-only snapshot.
- Tests: `chunked()`, `windowed()` and direct `subList` edits on a `ListMutator`.

## Acceptance

- [x] `subList(from, to)` returns the child mutators of the range; a change through one writes back to the parent
      (found by identity, as `get(index)` does) and notifies the parent's observers.
- [x] The snapshot refuses `add`, `remove`, `set`, `clear`, `addAll`, `removeAll`, `retainAll` and its iterators'
      `add`/`remove`/`set` with `UnsupportedOperationException`, the same on the JVM and JS.
- [x] Not a view: later structural changes to the parent leave the snapshot's size and order as they were.
- [x] Bounds as `List.subList`: `IndexOutOfBoundsException` / `IllegalArgumentException`; an empty range is empty.
- [x] `slice(IntRange)` works.

## Implementation notes

- `ListMutatorImpl.subList` (`mutator/core/src/commonMain/kotlin/ListMutator.kt`) checks the bounds, then wraps
  `(fromIndex until toIndex).map { get(it) }` (a fresh list, hence a snapshot) in `ReadOnlySnapshotList`. That is a
  small private class in the same file that implements `MutableList` and refuses every structural call
  unconditionally, even when the call would change nothing (e.g. `clear()` on an empty snapshot), as the JVM's
  unmodifiable lists do. A platform wrapper was avoided on purpose: its behaviour differs between JVM and JS.
- The snapshot's own `subList` returns another `ReadOnlySnapshotList`. `equals`, `hashCode` and `toString`
  delegate to the elements.
- `ReadOnlySnapshotList.It` writes out the `ListIterator` members instead of using `ListIterator<E> by inner`.
  The delegation compiles in Kotlin, but kapt's Java stub then lists `java.util.ListIterator` twice
  ("repeated interface") and `kaptKotlinJvm` fails.
- The KDoc names the existing quirk: if the same instance is in the list twice, a change goes to its first position.
- `add(index, element)` notifies its observers: the new test `ListMutatorNotificationsSpec` "add(index, element)
  should notify" was green on the unchanged code. No defect there; it closes the coverage gap found in ULTRA-00144.
- `mutator/core/CLAUDE.md` on master already says `chunked()` and `windowed()` work and names `subList` as a known
  defect. The coordinator removes that line after the merge.

## Test evidence

Specs in `mutator/core/src/sharedTest/kotlin/` (run on the JVM and JS): the new `ListMutatorSubListSpec` (10 tests),
`ListMutatorSubListProbeSpec` ("slice(IntRange) works through subList()"), `ListMutatorSpec` (the `subList` case now
pins the snapshot) and `ListMutatorNotificationsSpec` ("add(index, element) should notify").

`console/wt-gradle.sh :mutator:core:jvmTest :mutator:core:jsBrowserTest -x kotlinStoreYarnLock`, counts from
`.build-claude/mutator/core/test-results/*/TEST-*.xml`:

| | jvmTest | jsBrowserTest |
|---|---|---|
| tests first, old code | 169, 12 failures | 170, 12 failures (all from `subList` throwing) |
| implemented | 169, 0 failures | 170, 0 failures (169 + Kotest's bootstrap entry) |

Mutation checks, each run on both platforms and restored:

| Mutant | Killed by (JVM and JS) |
|---|---|
| (a) live child mutators without the write-back (`get()[it].childToMutator()`) | "writes back to the parent and notifies", "finds its element after the parent was restructured" |
| (b) `add` on the snapshot allowed | "refuses every structural change" (JS also "own subList()": on the JVM a one-element `toList()` is an immutable singleton, so the mutant still throws there) |
| (c) a live view over the parent's indexes | "the snapshot is not a view" |
| (d) `notifyObservers()` dropped from `add(index, element)` | "add(index, element) should notify" |

## Changelog text

- Mutator: `ListMutator.subList()` no longer throws. It returns a read-only snapshot of the child mutators in the
  range; changing an element through it updates the parent list and notifies its observers, while adding, removing
  or replacing elements in the snapshot throws `UnsupportedOperationException`. `slice(IntRange)` on a
  `ListMutator` works as a consequence.

## History

- 2026-10-23: created.
- 2026-11-13: probed which stdlib functions reach `subList` (track C of ULTRA-00316); `chunked` does not, `slice` does.
- 2026-11-13: decided as a read-only snapshot (maintainer); implemented on `wt/mutator`.
