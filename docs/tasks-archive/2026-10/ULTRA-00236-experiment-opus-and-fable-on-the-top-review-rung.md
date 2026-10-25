# ULTRA-00236: Experiment: Opus and Fable on the top review rung

> **Status (2026-10-25): WON'T IMPLEMENT.** stopped by the maintainer, no Fable in reviews (credits); no samples in ultra

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

## Result (2026-10-25, stopped)

Stopped for good by the maintainer (2026-10-25, via Mission Control): "Let us not use fable anymore but the newest
Opus (5.5) on max instead... we are burning through credits." No Fable reviewer at any rung, also not as an extra
voice beside Opus; the top rung is `reviewer-top` (opus, max) alone.

What ultra found: nothing to tally. No top-rung round ran in ultra between 2026-10-23 and the stop (the only gates,
ULTRA-00233 and ULTRA-00240, were clean in round 1), so the table stays empty. The one data point ultra has is still
MC-00026's blind run (`docs/housekeeping/2026-10-23-fable-experiment-ultra-00222.md`).

## Acceptance

- [x] The tally is ready for the 2026-10-30 meeting (no top-rung round this week is also a result): empty, stopped 2026-10-25.

## History

- 2026-10-23: created.
- 2026-10-24: PAUSED.
- 2026-10-24: paused (maintainer, token budget); no samples collected.
- 2026-10-25: stopped for good (maintainer, credits); no samples in ultra.
- 2026-10-25: WON'T IMPLEMENT. stopped by the maintainer, no Fable in reviews (credits); no samples in ultra
