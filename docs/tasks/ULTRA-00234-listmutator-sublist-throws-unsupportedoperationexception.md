# ULTRA-00234: ListMutator.subList() throws UnsupportedOperationException

> **Status (2026-11-14): IN REVIEW.** implemented on `wt/mutator` (track C of ULTRA-00316) as a read-only snapshot;
> round 2's claims fixed with row tokens (maintainer: "create followup ticket and fix the found issues, tighten the
> contract"); the review gate is next
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
      (found by row token, as `get(index)` does) and notifies the parent's observers.
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
  *Superseded in round 2 by row tokens, below.*
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
- Round 2, row tokens: `ListMutatorImpl` keeps a private list of `RowToken`s, one per row, in step with every
  structural change made through it: `add`, `add(index)`, both `addAll`, `remove` (now by index), `removeAt`,
  `removeAll`/`retainAll` (one loop, `removeRowsWhere`), `clear`, and through those the iterator's `add`/`remove`.
  `set(index)` and iterator `set` keep the row's token: they change the value, not the row. Replacing the whole value
  (`set(list)`, `modifyValue`) and `reset()` renew every token. `trackedChild` captures its row's token and the
  instance it last saw. It finds the row by token (the remembered index first, if that index still holds the token,
  else `indexOf` on the tokens). It writes only if the row still holds that very instance (`===`), and drops the
  write otherwise. It updates `seen` before `setAt` notifies, so a write from inside an observer lands (G).
- Round 2, decisions: a write whose row is gone, or which something else wrote since, is dropped silently, per the
  maintainer's rule "when a sensible fallback does the job, do not throw"; the `ListMutator` KDoc says so. Reorders
  (`sort`, `reverse`, `shuffle`) go through `set`, so they change values in place rather than moving rows: a held
  mutator's write after a reorder lands only if its row still holds its element, else it is dropped. The old
  identity lookup followed the element instead, but could also land on the wrong row with duplicates. Identity (not
  equality) in the guard is deliberate and pinned: a row rewritten with an equal value by someone else counts as
  written by someone else. A list changed in place through `get()` bypasses the tokens. When the row count no longer
  matches, all tokens are renewed and held mutators drop their writes; a same-size in-place change is not detected.
- Round 2, iterator contract (I): `next()`/`previous()` past the ends throw `NoSuchElementException`;
  `listIterator(index)` throws `IndexOutOfBoundsException` outside `0..size`, as `ArrayList` does.
- Round 2: `ListMutatorKnownDefectsSpec` keeps only H, now naming its follow-up ULTRA-00320 (generated code and
  `ObjectMutator`'s identity snapshot; out of this ticket).
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
- Domain expert: CLEAN (fable-medium). Nine MINOR, among them: the duplicate-instance write (claims A to E) rated
  MINOR and "accept and document", against the security reviewer's MAJOR: the maintainer decides; the changelog
  text overclaims ("every change now lands") and lacks the "no compile signal" list; iterator contract gaps (I);
  `tracked` updated after the notification (G); the baseline alias in generated code (H) as its own ticket; the
  snapshot's `equals`/`hashCode` stay as they are, only the KDoc wording at `ListMutator.kt:295-296` is wrong;
  false lines in `mutator/core/CLAUDE.md:4-5` and `collections.astro:49-50, 74-86`; stdlib calls (sort, shuffle,
  fill, removeAll, retainAll, asReversed, small reverse) traced as correct but not pinned by specs.

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

### Round 2 outcome (2026-11-14)

Two rounds without a clean one; the maintainer decided: "create followup ticket and fix the found issues, tighten the
contract". H moved to ULTRA-00320. A to G and I are fixed and J is pinned, in `ListMutatorRowTrackingSpec` (27 tests:
A to G, the identity pin, `clear`, whole-value `set`, `reset`, `sortBy`, `reverse` of 3 and of 20, `shuffle` over 20
seeds, `removeAll`, `retainAll`, `remove(element)`, `addAll(index)`, `set(index)`, iterator `remove`/`add`/`set`,
`subList` elements, in-place change through `get()`) and `ListMutatorIteratorSpec` (I).

| Claim | Outcome |
|---|---|
| A, B, C, D, E, F, G | fixed by row tokens; RED before on JVM and JS, green after |
| H | out of this batch: ULTRA-00320; still pinned as a known defect |
| I | fixed: `NoSuchElementException`, `listIterator(index)` bounds |
| J | pinned: the row guard is `===`; the `==` mutant turns the identity test red |

Evidence: `console/wt-gradle.sh :mutator:core:jvmTest :mutator:core:jsBrowserTest -x kotlinStoreYarnLock`, XML counts:

| | jvmTest | jsBrowserTest |
|---|---|---|
| tests first, old code | 222, 24 failures | 223, 24 failures |
| row tokens | 222, 0 failures | 223, 0 failures (222 + Kotest's bootstrap entry) |

Mutation checks, each on both platforms (same tests red on JVM and JS) and restored:

| Mutant | Turned RED |
|---|---|
| token lookup replaced by the index hint | 19 tests: B, C, D, E, F, `clear`, whole-value `set`, `reset`, `removeAll`, `retainAll`, `remove(element)`, `addAll(index)`, iterator `remove`/`add`, `subList` shifted row, in-place change through `get()`, both "row moved" tests, `ListMutatorSpec` "structurally modified during iteration" |
| token list not updated on `removeAt` | 8 tests: B, F, iterator `remove`, `remove(element)`, `subList` shifted row, both "row moved" tests, `ListMutatorSpec` "structurally modified during iteration" |
| `===` becomes `==` in the row guard (J) | the identity test "a row holding an equal but different instance ..." |
| `seen` updated after `setAt` notifies | G |
| whole-value replacement keeps the old tokens | "replacing the whole value: ... set() is dropped" |

### Round 3 (2026-11-14): level 3, reviewer-fable-xhigh, blind then reconciled

- Implementation & code style: **CLEAN** (0 CRITICAL, 0 MAJOR, 5 MINOR).
- Domain expert: **CLEAN** (0 CRITICAL, 0 MAJOR, 6 MINOR).
- Security / data integrity: **CLEAN** (0 CRITICAL, 0 MAJOR, 6 MINOR).

All three confirmed A to G, I and J fixed. The reviewers were read-only, so every scenario below was first written
as a test and run on the JVM and JS.

| # | MINOR (raised by) | Verdict | Evidence | Outcome |
|---|---|---|---|---|
| 1 | `addAll` with the list itself or a view of it reads `elements.size` after the insert (all three) | CONFIRMED | `addAll(m)`, `addAll(1, m)`, `addAll(m.asReversed())` RED on JVM and JS: the token count no longer matched the rows, so all tokens were renewed and held writes dropped | fixed: values extracted once, `values.size` tokens |
| 2 | the `reset()` test passes without the `reset()` override (the row count changed) | CONFIRMED | replaced by a same-size case; mutant "no `renewTokens` in `reset()`" turns it RED | test fixed |
| 3 | position mutants on `rows.removeAt(index)`, `rows.add(index, ...)`, `rows.addAll(index, ...)`, `rows.removeAt(i)` survive (security) | CONFIRMED | four "rows on both sides of a change in the middle" tests; each mutant turns its test RED (table below) | tests added |
| 4 | reorder tests only show drops; the shuffle test does not check the shuffle; D has a bare `m[1]` | CONFIRMED | deterministic `reverse()` and `sortBy()` cases where the row keeps its element and the write lands; shuffle asserted against `list.toMutableList().apply { shuffle(Random(seed)) }`; the bare `m[1]` only built a discarded child, removed | tests added |
| 5 | JS: a row holding `NaN` cannot be written (`NaN === NaN` is false) | CONFIRMED on JS only (JVM green: boxes keep their identity) | `Double.NaN` and `Float.NaN` tests RED on JS | fixed: `sameInstance` treats two NaN of the same type as the same |
| 6 | KDoc and changelog wording: "dropped" keeps the value; identity versus interning; move idiom; false `null` clause; first equal row; `get()` for reading; per-instance tracking; acceptance wording; `Collections.rotate` | CONFIRMED, except one detail | pinned: a dropped write carried by a later write (reverse twice; another mutator restoring the row's instance), `remove(element)` takes the first equal row (`[b, eY]`), JS equal numbers identical. NOT SURVIVED: "JVM: 100 lands, 1000 drops". On this test JVM even `listOf(100)[0] === (100 as Any)` is false, so boxed identity is not stable; documented only, no JVM pin. `Collections.rotate` on 100 rows confirmed to throw `UnsupportedOperationException` (JVM pin) | documented and pinned |
| 7 | the iterator's own `remove()`/`set()` are index-addressed | CONFIRMED on JVM and JS: `[b, x]`, `[b]`, `[a, b]` as traced | pinned as known behaviour next to H, named ULTRA-00324 | no fix (maintainer) |
| 8 | ULTRA-00320 scenarios | CONFIRMED on JVM and JS by a throwaway probe (not committed): a held element of `book.addresses` brings a removed row back; the nested list variant leaves `outer.isModified()` false with initial value `[[a, b, z]]` | | for ULTRA-00320 |

Views on the three open decisions of round 2:

- Identity versus equality in the row guard: implementation and security for identity, domain for equality.
  Maintainer: identity, final.
- Following an element across a reorder: not built; design ticket ULTRA-00323.
- In-place changes through `get()`: documented ("`get()` is for reading; the list passed to `set(list)` is kept by
  reference").

What stayed CLEAN (all three): tokens and rows in step on every structural path except `addAll(self)` (fixed); the
stdlib paths on the JVM and JS; an exception half-way through an operation; reentrancy; memory (one token per row,
no references to children); tokens cannot collide.

Mutation checks of round 3, each on JVM and JS and restored:

| Mutant | Turned RED |
|---|---|
| `rows.removeAt(index)` → `removeAt(0)` | "removeAt(middle)" |
| `rows.add(index, ...)` → `add(0, ...)` | "add(middle)" |
| `rows.addAll(index, ...)` → `addAll(0, ...)` | "addAll(middle)" and the three `addAll(self)` tests |
| `rows.removeAt(i)` → `removeAt(0)` in `removeRowsWhere` | "removeAll(middle)" |
| no `renewTokens` in `reset()` | the same-size `reset()` test |
| `addAll` creates `elements.size` tokens again | the three `addAll(self)` tests |
| `sameInstance` back to bare `===` | both NaN tests, on JS only (JVM: boxes keep identity) |

Counts (`console/wt-gradle.sh :mutator:core:jvmTest :mutator:core:jsBrowserTest -x kotlinStoreYarnLock`, XML): jvmTest
241, 0 failures; jsBrowserTest 241, 0 failures. JS is not JVM + 1 here: the JVM runs the two-test
`ListMutatorJvmCollectionsSpec`, JS runs the one-test `ListMutatorIdentityJsSpec` and Kotest's bootstrap entry.
`ListMutatorRowTrackingSpec` has 41 tests and `ListMutatorKnownDefectsSpec` 4, on both platforms.

## Changelog text

- Mutator: `ListMutator.subList()` no longer throws. It returns a read-only snapshot of the child mutators in the
  range; changing an element through it updates the parent list and notifies its observers, while adding, removing
  or replacing elements in the snapshot throws `UnsupportedOperationException`. Unlike `List.subList` it is not a
  view: later changes to the list do not show in it, and `subList(a, b).clear()` throws; remove a range on the list
  itself. `slice(IntRange)` on a `ListMutator` works as a consequence.
- Mutator fix: an element mutator of a `ListMutator` (from `list[i]`, `forEach`, an iterator or `subList`) now
  writes to its own row, wherever later adds and removes move it. Before, it lost its second change, and with the same
  instance in several rows it could write to another row. A write from inside a list observer through the same
  element mutator now lands, and so does a write to a row holding `NaN`.
- Mutator: a write through an element mutator is not applied when its row is gone (removed, cleared, the whole list
  replaced or reset) or holds another instance than the mutator last saw (another element mutator, `set`, a reorder
  such as `sort`, `reverse` or `shuffle`, or a move such as `add(j, removeAt(i))`). It is dropped silently, but the
  element mutator keeps its value, so a later write that does land carries it.
- Mutator fix: the `ListMutator` list iterator's `remove()` and `set()` after `previous()` hit the element before the
  one `previous()` returned, so `reverse()` on the JVM silently corrupted lists of 18 or more elements. They now act
  on the element last returned, and throw `IllegalStateException` without a preceding `next()`/`previous()`.
- Mutator fix: after `set()` back to a value equal to the baseline, a list, set or map mutator's next in-place change
  also changed the baseline (`isModified()` stayed false, `reset()` restored nothing).
- Mutator: the `ListMutator` list iterator's `next()`/`previous()` past the ends throw `NoSuchElementException` (was
  `IndexOutOfBoundsException`), and `listIterator(index)` checks `0..size` (was unchecked).

Behaviour changes without a compile signal (same signatures, different behaviour):

- `subList` and `slice(IntRange)` return a read-only snapshot instead of throwing.
- Iterator exceptions: `NoSuchElementException` past the ends, `IllegalStateException` for `remove()`/`set()`
  without a preceding `next()`/`previous()`, `IndexOutOfBoundsException` from `listIterator(index)` out of range.
- Held element mutators: after a reorder (`sort`, `reverse`, `shuffle`), a move such as `m.add(j, m.removeAt(i))`
  (drag and drop), a whole-value `set` or `reset`, a held mutator's later write is dropped where it used to follow
  the element by identity.
- `remove(element)`, `removeAll`, `retainAll` now remove through the row bookkeeping; results are unchanged.

Limits:

- The write check is identity. For strings, numbers and booleans, an equal value written by someone else may or may
  not count as the same instance, depending on platform and interning (on JS equal numbers and strings are identical;
  on the JVM boxed numbers usually are not).
- `remove(element)`, `indexOf` and `contains` compare by value and take the first equal row; for a specific row use
  `removeAt` or an iterator.
- The value from `get()` is for reading, and the list passed to `set(list)` is kept by reference: change the list
  through the mutator. Changing it in place bypasses the row tracking (a change of the row count is detected and drops
  held writes; a same-size change is not).
- Row tracking belongs to one list-mutator instance. A generated list property builds a new list mutator on each
  access, so element mutators from earlier accesses are not tracked by later ones (ULTRA-00320, together with known
  defect H).
- The list iterator's own `remove()` and `set()` address rows by index: after a structural change made around a live
  iterator they act on whatever row is at that index now (ULTRA-00324, pinned in `ListMutatorKnownDefectsSpec`).
- Held element mutators do not follow their element across a reorder (design ticket ULTRA-00323).

## Open points, not fixed

Decided by the maintainer (2026-11-14): Mutator is an experiment with no user, and this batch is the last one for it.
The row guard stays identity (`===`), final. The open points live on master, under the epic ULTRA-00322:

- ULTRA-00320 (paused): generated list properties build a new list mutator on each access, so a held list mutator's
  in-place change alters the baseline after `commit()` (H), a held element brings a removed row back, and a nested
  list mutator alters the outer baseline. Probed in round 3, item 8.
- ULTRA-00323 (design): held element mutators do not follow their element across a reorder.
- ULTRA-00324: the list iterator's own `remove()`/`set()` address rows by index (round 3, item 7; no fix, pinned).
- ULTRA-00325: the other open points of the review rounds.

## DOCS follow-up

False or incomplete lines, to fix once this settles; not edited here:

- `docs-site/src/pages/ultra/mutator/collections.astro:49-50`: "implements `MutableList<Mutator<T>>`, so you can use
  all familiar list operations". `subList` is a read-only snapshot, not a view, and held element mutators drop their
  writes after a reorder or once their row is gone.
- `docs-site/src/pages/ultra/mutator/collections.astro:74-86`: the example clears the list and then shows
  `mutator()  // [Address("Street 3", "City 3", "Zip 3")]`; after `clear()` the list is empty.
- `docs-site/src/data/llms/mutator.md:89`: "MutableList operations": the same caveat as above.
- Worth one line there: on the JVM, `Collections.rotate` on a `ListMutator` of 100 or more rows throws
  `UnsupportedOperationException`. For large non-random-access lists it reverses `subList` ranges, and the snapshot is
  read-only.

## History

- 2026-10-23: created.
- 2026-11-13: probed which stdlib functions reach `subList` (track C of ULTRA-00316); `chunked` does not, `slice` does.
- 2026-11-13: decided as a read-only snapshot (maintainer); implemented on `wt/mutator`.
- 2026-11-13: review round 1 NOT CLEAN (1 MAJOR, 8 MINOR), all confirmed and fixed on `wt/mutator` (`15edd63e`).
- 2026-11-13: review round 2: implementation CLEAN, security NOT CLEAN; claims A to J verified by tests (all
  confirmed), pinned as known defects in `ListMutatorKnownDefectsSpec`, not fixed (maintainer decides).
- 2026-11-14: maintainer: fix and tighten the contract; H to ULTRA-00320. A to G fixed with row tokens, I fixed, J
  pinned, on `wt/mutator`.
- 2026-11-14: review round 3 (level 3) CLEAN on all three charters; the MINOR batch applied and probed. Maintainer:
  last batch for the mutator; open points in ULTRA-00320, ULTRA-00323, ULTRA-00324, ULTRA-00325 (epic ULTRA-00322).
