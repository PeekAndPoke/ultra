# The fleet's defect-density ledger

The record of how the fleet actually did, one row per committed step. `SKILL.md` "Every brief opens
with the bar" is the hypothesis these numbers test. Adopted from klang 2026-09-28 and started empty;
klang's rows (`/opt/dev/peekandpoke/klang/.claude/skills/agent-fleet/defect-density-ledger.md`) are a
different codebase and are not comparable.

**Who fills it:** the coordinator, when a step commits, in the same pass that writes the commit
message. **The commit message carries** "N review rounds, C critical, M major" so a row can be rebuilt
from `git log`.

**The measure is defect density:** CRITICAL plus MAJOR findings across all rounds, the same defect
found by two reviewers counted once, per 1000 changed PRODUCTION lines (insertions plus deletions in
the step's commits, excluding test sources, `docs-site/`, `*.md` and `.claude/`; `git show --numstat
<commit>` and sum). Rounds to clean beside it, test lines as context. Confounds: step size, and the
review process itself maturing over the same period.

| step | opening line | prod lines | test lines | rounds to clean | CRIT+MAJOR | per kLoC | note |
|------|--------------|------------|------------|-----------------|------------|----------|------|
