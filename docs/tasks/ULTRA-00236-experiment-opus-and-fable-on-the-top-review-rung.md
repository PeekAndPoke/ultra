# ULTRA-00236: Experiment: Opus and Fable on the top review rung

> **Status (2026-10-24): PAUSED.** Suspended by the maintainer for the token budget (2026-10-24, via Mission Control):
> "No more Fable reviewers, instead we will use Opus reviewers on max effort", clarified: "Opus max on the highest
> review tier. The two lower ones stay as they are." `reviewer-top` is now Opus at max; the CLAUDE.md line is removed.
> No top-rung round ran in ultra, so there are no samples (reported to MC-00026).

## Why this exists

The maintainer (2026-10-23, via Mission Control): "No repeat runs, let us get more samples in practice. For the max
tier review rounds we always run 1 Opus and 1 Fable for the next week. This should give us some insights. In
general to me this means, that we need to always run "enough" reviewers to hedge our bets. What enough means is up
for debate next friday." Follows the blind run of MC-00026 (`docs/housekeeping/2026-10-23-fable-experiment-ultra-00222.md`).

## Decide before implementing

## The work

- Every top-rung round runs, per charter, `reviewer-top` (Fable) and `reviewer-xhigh` spawned with effort `max`
  (Opus 5.5): the same brief, blind to each other.
- The round's findings record, per finding, who found it (both, Opus only, Fable only), and each reviewer's tokens.
- One row per round below; the meeting tallies them.

## Tally

| date | ticket, round | charter | both | Opus only | Fable only | Opus tokens | Fable tokens |
|---|---|---|---|---|---|---|---|

## Acceptance

- [ ] The tally is ready for the 2026-10-30 meeting (no top-rung round this week is also a result).

## History

- 2026-10-23: created.
- 2026-10-24: PAUSED.
- 2026-10-24: paused (maintainer, token budget); no samples collected.
