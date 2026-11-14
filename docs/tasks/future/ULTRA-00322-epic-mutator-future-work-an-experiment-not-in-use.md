# ULTRA-00322: Epic: Mutator, future work (an experiment, not in use)

> **Status (2026-11-14): NOT SCHEDULED.** An epic: its progress is its children's (`ticket.py list --epic ULTRA-00322`). The maintainer (2026-11-14): "Mutator is not in use anywhere and might never really be, it is an experiment. [...] All open point should go into a 'future mutator epic'".

## Why this exists

Mutator is an experiment and has no user. The review of ULTRA-00234 (ListMutator) closed the serious defects (writes
landing in the wrong row) and left open points that are documented, not fixed. They are collected here so that
nothing is lost and nothing of it competes with the areas that are in use.

## What belongs here

Every open mutator point: defects that are known and pinned by a test, design questions, missing tooling. Nothing here
is scheduled. When Mutator gets a real user, start with ULTRA-00323 (the design for tracking collection elements):
most of the other children change shape with it.

New tickets in this area: `ticket.py new "Title" --part-of ULTRA-00322`.

## History

- 2026-11-14: created on the maintainer's word; ULTRA-00318 and ULTRA-00320 moved here from ULTRA-00255.
