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
| 2026-10-17 | Kronos advanceBy collapse, round 1 (`.claude/tasks-archive/2026-10/20261017-kronos-advanceby-collapse.md`) | `advanceBy { }` on an already-read node (`total.isInitialized()` arm, `ultra/datetime/src/commonMain/kotlin/Kronos.kt`) had no test; `ZERO` / double-count / `settled` mutants survived. The coordinator's own mutant list skipped that arm | test | OPEN — proposal: Standard 2 gains "a branch on cached or lazy state (`isInitialized()`, a memo hit) gets one mutant per arm". Not applied: a `.claude/skills/` edit takes the full review set |
| 2026-10-18 | kraft chartjs live updates, round 1 (`.claude/tasks-archive/2026-10/20261018-kraft-chartjs-live-updates.md`) | The merge wrote into the CALLER's mount-time config: Chart.js keeps the config object it is given (`initConfig`/`initData`) and writes into it, so merging "onto the existing objects" meant the caller's objects. Implementer and plan both assumed the library copies | checklist | OPEN — proposal: the domain charter in `/feature-review` gains "when code hands objects to a third-party library, who owns them afterwards — does the library keep and mutate them?". Not applied: a `.claude/skills/` edit takes the full review set |
