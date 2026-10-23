# ULTRA-00234: ListMutator.subList() throws UnsupportedOperationException

> **Status (2026-10-23): NOT STARTED.** 

## Why this exists

`ListMutator.subList()` throws (`mutator/core/src/commonMain/kotlin/ListMutator.kt:260`). Stdlib functions such as
`chunked()` call it internally, so even indirect use crashes. Collection mutators are supposed to implement their
`MutableList`/`Set`/`Map` contracts fully; this is the one known gap. Moved here from CLAUDE.md "Project facts"
(Rules Retro, 2026-10-23); `mutator/core/CLAUDE.md` keeps a one-line pointer.

## Decide before implementing

- A live, write-through view (the contract) or a read-only snapshot. Mutator is not battle-tested; the maintainer decides.

## The work

- Implement `subList` as a live view that writes through to the parent mutator, per the `MutableList` contract.
- Tests: `chunked()`, `windowed()` and direct `subList` edits on a `ListMutator`.

## Acceptance

## History

- 2026-10-23: created.
