# Incidents

Capture the incident when it happens: one line in this file. Fix the code and the tests at once; a rule
waits for the weekly meeting, which also prunes. Damage that can't be undone (lost work or data, an
exposed secret, a push or a publish) gets a stopgap at once, preferably a check or a deny-list request,
else one line marked PROVISIONAL until <meeting date>; the meeting keeps or drops it, and unconfirmed it
lapses.

After each meeting the decided lines move to a dated record. A review escape (a CRITICAL or MAJOR a round
found) is one class of incident; this file replaces the review-loop escape ledger (MC-00028).

| date | what happened | what let it through | what it cost | immediate fix | proposed rule |
|------|---------------|---------------------|--------------|---------------|---------------|
| 2026-10-31 | Someone, not yet known (no spross session wrote it, per Spross-Main-Dev's transcripts; most likely the maintainer by hand, 48 s after spross's copy changed), wrote spross's IntelliJ allow list over ultra's `.claude/hooks/idea-actions.allow`, adding `SaveAll` and `Synchronize`, which ultra never allowed | Every project's agent runs as user claude and can write to ultra's checkout; the hook file is plain text | Nothing so far: caught uncommitted in `git status`; but the hook reads the file on every call (`idea-action-gate.py:22`), so `SaveAll` and `Synchronize` already run without a prompt in ultra, this session included | Left uncommitted; asked the maintainer; Mission Control asked Spross-Main-Dev | A session writes only in its own repo's `.claude/` (for the meeting) |
| 2026-11-14 | `FastCache`'s maintenance loop could die for good on its first pass: the loop is launched in an `init` block, and a failure counter added in the round-4 MINOR batch of ULTRA-00159 (`e0b1ceda`) was declared below it, so a pass on another thread hit a null field and the `NullPointerException` ended the loop silently (no expiry, no eviction, no refresh) | The MINOR batch after the clean round 4 was merged without another review, and the track ran its suite on an idle machine, where the constructing thread is rarely preempted | About an hour on master with the defect (`8545866a`, not pushed, not released); found by the coordinator's run on master under load from two other tracks: 3 of 200 tests failed, one hung for 10 minutes | Fixed at once (`022a9c02`): the `init` block is last in the class and a deterministic test runs the first pass inside the constructor; every cache test is bounded at one minute; 20 loaded runs green | A MINOR batch that touches production code gets a delta review before the merge, and a suite with threads is run once under load before it counts as green (for the meeting) |
