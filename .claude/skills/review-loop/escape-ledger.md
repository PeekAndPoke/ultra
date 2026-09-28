# The escape ledger

One row per escaped defect: every CRITICAL or MAJOR a review round found, what let it through, and the
ONE thing that changed so the class cannot escape the same way again. `SKILL.md` "Standard 3" states the
method and the five classes; this file holds the record. Adopted from klang 2026-09-28 and started
empty — klang's own rows (`/opt/dev/peekandpoke/klang/.claude/skills/review-loop/escape-ledger.md`) are
worth a read for the general classes, but they are audio-specific evidence and stay there.

**Who appends:** the coordinator, when a step commits. **What a row needs:** the date, the step and
round, what escaped, the class (brief, checklist, test, design, tooling, or "recurrence" naming the row
it repeats), and what it changed. A row whose last column is a restatement rather than a changed
artefact is not closed.

**Why recurrence is the signal.** The sample is too small to estimate a rate, so the loop improves at
n = 1: a class that escapes twice means the rule's TEXT failed, and it gets rewritten rather than
repeated. Reading this file for recurrences before writing a brief is the point of keeping it.

| date | step | the escape | class | what it changed |
|------|------|------------|-------|-----------------|
