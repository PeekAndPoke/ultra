# ULTRA-00234: ListMutator.subList() throws UnsupportedOperationException

> **Status (2026-10-23): NOT STARTED.** 
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

When `subList` is implemented, the `slice` case flips and must be rewritten.

## Decide before implementing

- A live, write-through view (the contract) or a read-only snapshot. Mutator is not battle-tested; the maintainer decides.

## The work

- Implement `subList` as a live view that writes through to the parent mutator, per the `MutableList` contract.
- Tests: `chunked()`, `windowed()` and direct `subList` edits on a `ListMutator`.

## Acceptance

## History

- 2026-10-23: created.
- 2026-11-13: probed which stdlib functions reach `subList` (track C of ULTRA-00316); `chunked` does not, `slice` does.
