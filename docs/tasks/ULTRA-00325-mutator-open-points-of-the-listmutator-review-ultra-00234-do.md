# ULTRA-00325: Mutator: open points of the ListMutator review (ULTRA-00234), documented and not fixed

> **Status (2026-11-14): PAUSED.** Not scheduled; a collection of small open points, recorded on the maintainer's word (2026-11-14: "document all open points and create follow up tickets").
>
> **Part of:** ULTRA-00322 (Epic: Mutator, future work (an experiment, not in use))

## Why this exists

The review of ULTRA-00234 ended clean (round 3, no CRITICAL, no MAJOR). These points stayed open on purpose. Each is
documented in the KDoc or the changelog; none writes to a wrong row. The ledger in ULTRA-00234 has the reviewers'
scenarios.

## The open points

1. **`remove(element)`, `indexOf`, `contains` take the first equal row.** With one instance in several rows and two
   held mutators, `m.remove(last)` removes the first row: `[e, b, e]`; `first = m[0]`; `last = m[2]`; `m.remove(last)`
   leaves `last` alive and `first` dead. A tracked child could resolve to its own row.
2. **A dropped write is kept by the mutator** and arrives with a later write that passes the guard (reverse twice;
   another child going back to its baseline). A handle could be marked dead after its first drop.
3. **The identity guard is not portable for strings, numbers and booleans:** JS compares them by value, the JVM by
   box (100 lands, 1000 drops). Both outcomes are safe. The guard stays identity (maintainer, 2026-11-14).
4. **Lists whose elements are mutable collections** (hand-built, not generated): the child's live collection becomes
   the parent's row, so the guard cannot see an in-place change and one child can overwrite another's addition. Same
   root as ULTRA-00320.
5. **The raw list:** `get()` hands out the live list, `set(list)` keeps the given list by reference, observers and
   `reset()` hand it out too. A same-size change on it is not noticed.
6. **`Collections.rotate` on 100 or more rows** throws `UnsupportedOperationException`, because `subList` is a
   read-only snapshot.
7. **Not pinned by specs:** `fill`, `asReversed`, the small `reverse` (traced as correct by the domain reviewer).
8. **`mutator/core/CLAUDE.md`** still names `subList()` as a known defect; the corrected lines need the maintainer's OK.
9. **Docs lines that are false since ULTRA-00234** (no DOCS follow-up ticket was made; Mutator is not scheduled):
   `docs-site/src/pages/ultra/mutator/collections.astro:49-50, 74-86` (the example at 74-86 shows a row after
   `clear()`), `docs-site/src/data/llms/mutator.md:89`; to add: `subList` is a read-only snapshot, the dropped-write
   rule, `Collections.rotate`. The full list is in ULTRA-00234's "DOCS follow-up" section.

## Acceptance

- Each point is fixed, moved into its own ticket, or closed as "stays documented", after ULTRA-00323.

## History

- 2026-11-14: created on the maintainer's word.
