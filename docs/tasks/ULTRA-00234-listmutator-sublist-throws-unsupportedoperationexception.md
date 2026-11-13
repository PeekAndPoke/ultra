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
- [x] Round 1: repeated changes through one child mutator all land; a change goes to the element's own index when
      the same instance is in the list twice; the list iterator's `remove()`/`set()` act on the element last returned.

## Implementation notes

- `ListMutatorImpl.subList` (`mutator/core/src/commonMain/kotlin/ListMutator.kt`) checks the bounds, then wraps
  the child mutators `get(it)` hands out, in a fresh list (hence a snapshot), in `ReadOnlySnapshotList`. That is a
  small private class in the same file that implements `MutableList` and refuses every structural call
  unconditionally, even when the call would change nothing (e.g. `clear()` on an empty snapshot), as the JVM's
  unmodifiable lists do. A platform wrapper was avoided on purpose: its behaviour differs between JVM and JS.
- The snapshot's own `subList` returns another `ReadOnlySnapshotList`. `contains`, `containsAll`, `indexOf` and
  `lastIndexOf` compare by value, as the list does; `toString` shows the values; `equals` and `hashCode` delegate to
  the element mutators.
- `ReadOnlySnapshotList.It` writes out the `ListIterator` members instead of using `ListIterator<E> by inner`.
  The delegation compiles in Kotlin, but kapt's Java stub then lists `java.util.ListIterator` twice
  ("repeated interface") and `kaptKotlinJvm` fails.
- Round 1: `get(index)`, `It.next()`, `It.previous()` and so `subList` share one private helper, `trackedChild`. It
  tracks the instance it last wrote (so a second change through the same mutator lands, the way `SetMutator`'s
  `currentTrackedValue` already worked) and the index it last wrote. A change goes to that index when the tracked
  instance is still there, else to the first position holding that instance (by identity), and is dropped when the
  list no longer holds it. The old same-instance-twice quirk is left only for an element that moved.
- Round 1: the list iterator keeps `lastReturned`, as `ArrayList`'s iterator does. `remove()` and `set()` act on that
  element; before any `next()`/`previous()`, after `remove()` and after `add()` they throw `IllegalStateException`.
  The old `set()` before `next()` silently overwrote index `pos - 1`.
- Round 1: `Mutator.Base.modifyValue` used to set the value to the baseline instance itself when the new value equals
  the baseline. For list, set and map mutators, which change the value in place, the next in-place change then also
  changed the baseline: `isModified()` stayed false and `reset()` restored nothing. It now takes
  `snapshotInitial(_initial)`, a copy for collections and the same instance for immutable values (unchanged there).
- KDoc: the `subList` contract is redeclared on `interface ListMutator`, so quick-doc shows it instead of the stdlib's
  "returns a view".
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

### Round 1 fixes

New specs `ListMutatorChildWriteBackSpec` (9), `ListMutatorIteratorSpec` (8) and `MutatorBaselineSpec` (3), plus two
cases in `ListMutatorSubListSpec` (value comparisons, `toString`); the "restructured" case there was rewritten so the
element really moves (c from index 2 to 1, index 2 then holds another element).

| | jvmTest | jsBrowserTest |
|---|---|---|
| tests first, old code | 191, 19 failures | 192, 18 failures (`reverse()` on 20 passes on JS: its stdlib `reverse()` swaps by index) |
| fixed | 191, 0 failures | 192, 0 failures (191 + Kotest's bootstrap entry) |

Mutation checks, each on both platforms and restored:

| Mutant | Turned RED (JVM and JS unless noted) |
|---|---|
| tracker reverted (no `tracked = new`) | the four "two changes ... both land" tests |
| no index hint (always `indexOfFirst`) | the three "goes to its own index when the same instance is in the list twice" tests |
| no identity lookup (always the remembered index) | both "finds its element by identity after the element moved", "no longer holds is dropped", `ListMutatorSpec` "iterator should not crash when list is structurally modified" |
| iterator `set()` back to `pos - 1` | "set() after previous()"; on the JVM also "reverse() on 20 elements" |
| no state check in iterator `remove()` | "remove() before next()", "remove() twice", "remove() right after add()" |
| `modifyValue` back to `else _initial` | the three `MutatorBaselineSpec` tests |
| snapshot `indexOf` by mutator object | "contains(), containsAll(), indexOf() and lastIndexOf() compare by value" |
| snapshot `toString` of the mutators | "the snapshot's toString() shows the values" |

## Review record — the LEDGER

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `ListMutator.kt` `get(index)`, `It.next`/`previous`, snapshot | A second change through one element mutator is lost: the write-back looks up the captured first instance | CONFIRMED, fix | RED first: 4 "two changes" tests on JVM and JS. Fix: one helper `trackedChild` tracking the latest instance |
| 1 | MINOR | same helper | Same instance twice: a change through index 1 lands on index 0 | CONFIRMED, fix | RED first: 3 "own index" tests on JVM and JS. Fix: index hint, identity fallback |
| 1 | MINOR | `ListMutator.kt` `It.remove`/`set` | After `previous()` they hit the element before; JVM `reverse()` corrupts lists of 18+ | CONFIRMED, fix | RED first: JVM 7, JS 6 of `ListMutatorIteratorSpec` (`reverse()` red on the JVM only). Also found: `set()` before `next()` silently overwrote index `pos - 1`. Fix: `lastReturned`, `IllegalStateException` |
| 1 | MINOR | `Mutator.kt:28` | `modifyValue` aliases value and baseline | CONFIRMED, fix | RED first: list, set and map: `isModified()` false after an in-place change. Fix: `snapshotInitial(_initial)` |
| 1 | MINOR | snapshot `contains`/`indexOf`/... | Compare by mutator object, not by value | CONFIRMED, fix | RED first: `indexOf(m[2])` was -1 |
| 1 | MINOR | `ListMutator` KDoc | Quick-doc shows the stdlib's "returns a view" | CONFIRMED, fix | `subList` redeclared on `interface ListMutator` with the contract |
| 1 | MINOR | `ListMutatorSubListSpec` | "restructured" test passes without the identity lookup | CONFIRMED, fix | c stayed at index 2; rewritten so it moves, now RED under the no-identity mutant. Probe tests renamed; explicit return types (`: Unit`, `: Boolean`, `: Int`) on expression-bodied functions in `ListMutator.kt` |
| 1 | MINOR | snapshot `toString` | Prints mutator objects | CONFIRMED, fix | RED first; prints the values now |
| 1 | MINOR | ticket | Changelog lines for not-a-view and the fixes; DOCS follow-up lines | done | see Changelog text and DOCS follow-up |

Fix commit: `15edd63e` (round 1, all rows). Not done (coordinator): a public snapshot type with deprecated mutators.

### Round 2

- Implementation & code style: **CLEAN**.
- Security / data integrity: **NOT CLEAN** (one MAJOR claim and further claims, below).
- Domain expert: running.

Two rounds without a clean one: verified with tests only, nothing fixed; the maintainer decides how to go on. Every
claim was written as a test asserting the EXPECTED behaviour and run on the JVM and JS; all were RED on both, with the
actual results below. They are pinned as they behave today in `ListMutatorKnownDefectsSpec` (`sharedTest`; each test
name says "KNOWN DEFECT" and gives the expected behaviour), so the suite stays green. `E` is one shared instance.

| Round | Sev | `path:line` | Claim | Verdict | Evidence (JVM and JS alike) |
|---|---|---|---|---|---|
| 2 | MAJOR | `ListMutator.kt:275` identity fallback | A: `[E, E]`; `held = m[0]`; `m[0].street = "S"`; `held.city = "Y"` writes row 1 (expected: dropped) | CONFIRMED | `[ES, EY]` |
| 2 | MAJOR | same | B: `[E, E, E]`; `row3 = m[2]`; `removeAt(0)`; `row3.street = "X"` (expected `[E, EX]`) | CONFIRMED | `[EX, E]` |
| 2 | MAJOR | same | C: `[E, b, E]`; `c = m[2]`; `add(0, z)`; `c.street = "X"` (expected index 3) | CONFIRMED | index 1 written: `[z, EX, b, E]` |
| 2 | MAJOR | same (hint hit) | D: `[a, b]`; `c0 = m[0]`; `c0.set(b)`; `removeAt(0)`; `c0.street = "X"` writes c1's row (expected: dropped) | CONFIRMED | `[bX]` |
| 2 | MAJOR | same | E: strings `["", "", "z"]`; `e = m[1]`; `add(0, "new")`; `e.set("typed")` (expected `["new", "", "typed", "z"]`) | CONFIRMED | `["new", "typed", "", "z"]` |
| 2 | MINOR | `ListMutator.kt:275` `getOrNull` | F: nullable `["x", null]`; `e = m[1]`; `removeAt(0)`; `e.set("y")` | CONFIRMED | `IndexOutOfBoundsException` (index 1, size 1): `getOrNull(1) === null` matches the tracked `null` |
| 2 | MINOR | `trackedChild`, `tracked` updated after `setAt` | G: a parent observer writing through the same held child during the notification | CONFIRMED | nested write dropped: `[Address(S, C, Z)]`, expected city `Y` |
| 2 | MINOR | outside the change set: generated `MutatorCodeBlocks.kt:134`, `ObjectMutator` identity `snapshotInitial` | H: `pm = book.addresses`; `pm.add(b)`; `book.commit()`; `pm.add(z)` | CONFIRMED | `isModified()` false, `getInitialValue()` holds `z`: the book's baseline shares the held list mutator's live list |
| 2 | MINOR | `ListMutator.kt` `It` | I: `next()` past the end / `previous()` at 0 throw `IndexOutOfBoundsException`, not `NoSuchElementException`; `listIterator(-1)` and `listIterator(size + 1)` succeed | CONFIRMED | all four as claimed |
| 2 | MINOR | `ListMutator.kt:275` | J: identity versus equality is not pinned | CONFIRMED | mutant `indexOfFirst { it == tracked }` and mutant `==` in both comparisons: all 201 JVM / 202 JS tests stay green |

On JS `===` on strings compares values, so for E the "identity" lookup is value equality there; the result is the
same on both platforms.

**Fix shape for A to E, as the reviewer proposes** (on a hint miss accept the identity match only if it is unique,
else drop; D needs a removal counter or a documented limit): about 10 lines in `trackedChild`. It does not fix A or D.
In A, after row 0 was replaced, `E` is unique in row 1, so the write still goes there. In D the hint HITS (`b` sits at
the held index after the removal), so the uniqueness rule never runs. B, C and E become silent drops instead of
landing on the held row. A removal counter that distrusts the hint after any removal fixes D but also drops B.

**A better shape: a row token per element.** `ListMutatorImpl` keeps a parallel list of opaque tokens, one per row.
A child captures its row's token and the instance it last wrote. On a write, it finds its row by token (hint first,
then `indexOf` on the tokens) and writes only if that row still holds the instance the child last saw (`===`); else
it drops.

- That gives every expected result: A drops (row 0 holds `ES`), B and C land on the held row, D drops (its token went
  with the removed row), E lands, and F lands (no `getOrNull` ambiguity).
- G is fixed by setting `tracked = new` before `setAt` notifies.
- Cost: one small object per row, O(n), and no references to the children, so nothing to leak.
- Every structural operation must keep the tokens in step: `add`, `add(index)`, both `addAll`, `remove`, `removeAt`,
  `removeAll`, `retainAll`, `clear`, iterator `add`/`remove`, and whole-value replacement (`set`, `modifyValue`,
  `reset`, which renew all tokens).
- Risk: `get()` hands out the live `MutableList`, so a caller changing it in place desyncs the tokens. Detect a size
  mismatch and drop the writes then, or document it.
- Size: roughly 80 to 120 lines in `ListMutator.kt`, plus tests.

Registering the children with the parent, so structural changes shift their indexes, would also work. But the parent
would hold a strong reference to every child it ever handed out: `forEach` on every render grows it without bound,
unless weak references are used, and those were removed from `ultra/common`'s `Observable` in 2026-07. Not
recommended. H is a separate defect, in the generated code and `ObjectMutator`'s identity snapshot, not `ListMutator`.

## Changelog text

- Mutator: `ListMutator.subList()` no longer throws. It returns a read-only snapshot of the child mutators in the
  range; changing an element through it updates the parent list and notifies its observers, while adding, removing
  or replacing elements in the snapshot throws `UnsupportedOperationException`. Unlike `List.subList` it is not a
  view: later changes to the list do not show in it, and `subList(a, b).clear()` throws; remove a range on the list
  itself. `slice(IntRange)` on a `ListMutator` works as a consequence.
- Mutator fix: a second change through the same element mutator of a `ListMutator` (from `list[i]`, `forEach`, an
  iterator or `subList`) was lost without a notification; every change now lands.
- Mutator fix: when the same instance is in a `ListMutator` twice, a change through `list[1]` (or an iterator or
  `subList` element) went to index 0; it now goes to the element's own index.
- Mutator fix: the `ListMutator` list iterator's `remove()` and `set()` after `previous()` hit the element before the
  one `previous()` returned, so `reverse()` on the JVM silently corrupted lists of 18 or more elements. They now act
  on the element last returned, and throw `IllegalStateException` without a preceding `next()`/`previous()`.
- Mutator fix: after `set()` back to a value equal to the baseline, a list, set or map mutator's next in-place change
  also changed the baseline (`isModified()` stayed false, `reset()` restored nothing).

## DOCS follow-up

Lines that describe `ListMutator` as a full `MutableList` and need a sentence on `subList` (read-only snapshot, not a
view) once this settles; not edited here:

- `docs-site/src/pages/ultra/mutator/collections.astro:49-50`
- `docs-site/src/data/llms/mutator.md:89`

## History

- 2026-10-23: created.
- 2026-11-13: probed which stdlib functions reach `subList` (track C of ULTRA-00316); `chunked` does not, `slice` does.
- 2026-11-13: decided as a read-only snapshot (maintainer); implemented on `wt/mutator`.
- 2026-11-13: review round 1 NOT CLEAN (1 MAJOR, 8 MINOR), all confirmed and fixed on `wt/mutator` (`15edd63e`).
- 2026-11-13: review round 2: implementation CLEAN, security NOT CLEAN; claims A to J verified by tests (all
  confirmed), pinned as known defects in `ListMutatorKnownDefectsSpec`, not fixed (maintainer decides).
