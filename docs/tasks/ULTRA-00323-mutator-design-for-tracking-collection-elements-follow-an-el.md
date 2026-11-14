# ULTRA-00323: Mutator: design for tracking collection elements (follow an element across reorders and across accesses)

> **Status (2026-11-14): PAUSED.** A design ticket; not scheduled. The maintainer (2026-11-14): "we need a better design to track collection elements. Create a design ticket as well."
>
> **Part of:** ULTRA-00322 (Epic: Mutator, future work (an experiment, not in use))

## Why this exists

ULTRA-00234 gave `ListMutator` row tokens: a held element mutator finds its row by a token and writes only if the row
still holds the instance it last saw. That is safe (no write lands in a wrong row), but it is a stopgap with limits
that a user of the library would not expect:

- **A held mutator does not follow its element.** After `sort`, `reverse`, `shuffle` or the move idiom
  `m.add(j, m.removeAt(i))` (drag and drop) its write is dropped, unless the element stayed in its row. Reorders reach
  the mutator as single `set` calls from the stdlib, which it cannot tell from "someone wrote another value".
- **Tracking is per list-mutator instance**, and a generated property builds a new one on each access:
  `held = book.addresses[0]`; `book.addresses.removeAt(0)`; `held.street = "X"` brings the removed row back
  (ULTRA-00320 has the scenario).
- **"Dropped" is not final:** the held mutator keeps its change, and a later write that passes the guard carries it.
- **Identity guard:** kept on the maintainer's decision (2026-11-14). For strings and numbers, whether a row rewritten
  with an equal value counts as someone else's differs between JVM and JS.
- **Changes on the raw list** from `get()` bypass the tokens; only a changed row count is noticed.
- Set and map mutators have no tracking of this kind at all.

## Decide before implementing

- What a child mutator is: a live handle on an element (it follows the element, like an immer draft) or a handle on a
  row. Everything else follows from this.
- Where tracking lives: in the parent that owns the property (one tracker per owner, surviving accesses and
  `commit()`), not in each list-mutator instance. This is also the root of ULTRA-00320.
- How a reorder is expressed so that values and trackers move together: mutator-level `move`/`reorder`/`sort`
  operations, or move detection inside `set`.
- Whether a write through a dead handle stays a silent drop, and whether the handle can say that it is dead.
- Memory: a registry of children must not keep removed rows or abandoned handles alive.

## The work

A design note with the answers above and worked examples (duplicates of one instance, nested lists, reorder, drag and
drop, a handle held across `commit()`), reviewed before any code. Then tests first.

## Acceptance

- The design is agreed with the maintainer.
- ULTRA-00320, ULTRA-00324 and ULTRA-00325 are re-read against it: each is covered, changed or closed.

## History

- 2026-11-14: created on the maintainer's word.
