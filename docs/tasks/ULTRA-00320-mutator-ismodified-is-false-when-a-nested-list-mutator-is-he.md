# ULTRA-00320: Mutator: isModified() is false when a nested list mutator is held across the parent's commit()

> **Status (2026-11-14): DESIGNED.**
>
> **Part of:** ULTRA-00255 (Ultra libraries epic)

**Plan:** ULTRA-00316 (close the open epics), track C. Found by the security review of ULTRA-00234, round 2 (claim H).

## Why this exists

A caller holds the mutator of a nested list, the parent commits, and the caller writes through the held mutator
afterwards. The write lands, but the parent's `isModified()` stays false, so code that saves "only when modified"
skips the save.

Confirmed on JVM and JS; pinned as a known defect in `ListMutatorKnownDefectsSpec` (claim H, commit `2b746d0c` on
`wt/mutator`). The cause sits outside ULTRA-00234: the generated code (`mutator/ksp/src/main/kotlin/MutatorCodeBlocks.kt:134`)
and `ObjectMutator`'s snapshot by identity.

**Same root, traced by the round-3 implementation reviewer of ULTRA-00234, not yet confirmed by a test:** the
generated getter builds a new list mutator on every access (`MutatorCodeBlocks.kt:132-134`), so row tracking does not
reach across two accesses. `held = book.addresses[0]`; `book.addresses.removeAt(0)`; `held.street = "X"`: the write
goes into the first mutator's own copy and the removed row comes back. Verify first, then fix with claim H.

## Decide before implementing

- What `commit()` means for child mutators handed out before it: do they stay live (and report into the parent), or
  are they detached, and is a write through a detached one refused or silently local?
- Whether the same holds for nested object, set and map mutators; check before fixing the list case alone.

## The work

- Tests first: the pinned claim H turned into a real expectation; the same scenario for object, set and map children.
- Fix in the generator and/or `ObjectMutator`, as the decision above says.
- Mutation check: the fix removed turns the new tests red.

## Acceptance

- The parent reports a write made through a child mutator held across `commit()`, or the write is refused, as decided.
- `ListMutatorKnownDefectsSpec` no longer lists claim H.
- JVM and JS green.

## History

- 2026-11-14: created on the maintainer's word ("create followup ticket").
