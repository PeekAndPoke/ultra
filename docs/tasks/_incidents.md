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
| 2026-10-31 | Another project's session (spross, per Mission Control's mtimes) wrote spross's IntelliJ allow list over ultra's `.claude/hooks/idea-actions.allow`, adding `SaveAll` and `Synchronize`, which ultra never allowed | Every project's agent runs as user claude and can write to ultra's checkout; the hook file is plain text | Nothing so far: caught uncommitted in `git status`; the hook loads at session start, so it may already apply to new ultra sessions | Left uncommitted; asked the maintainer; Mission Control asked Spross-Main-Dev | A session writes only in its own repo's `.claude/` (for the meeting) |
