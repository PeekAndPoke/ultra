# <Feature name>

**Status:** TODO | IN PROGRESS | IN REVIEW | DONE (archived YYYY-MM-DD)
**Plan:** `.claude/tasks/<plan>.md` → <phase / item>
**Security-critical:** yes | no  (yes → red-team follow-up task required, see below)

## Spec

What to build. Acceptance criteria as a checklist:

- [ ] ...

## Implementation notes

Decisions made while building — keep short, link code as `path/File.kt:line`.

## Test evidence

- [ ] Unit/behavior tests
- [ ] End-to-end tests (backend: `AppSpec`/`AppUnderTest`; both DB backends via `MatrixTest2d`
      when storage is involved)
- [ ] Full test command(s) run + green: `...`

## Review record — the LEDGER (filled by /feature-review)

**Written BEFORE the next round starts, not after the last one.** The loop's phase 2 asks a reviewer to
name what is factually wrong in a rejection reason — a reviewer who cannot see the reason cannot do
that, and its re-raise then counts as a withdrawal. An empty Reason column silences correct findings.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | CRITICAL | | | fix / reject / maintainer-decision / out-of-scope | checkable fact or named rule |

Severity is **CRITICAL / MAJOR / MINOR** — not HIGH or MEDIUM; the loop keys on these exact words.
A rejection reason must be checkable, and you may not reject a CRITICAL/MAJOR against code you wrote.

Also record: findings of yours that did NOT survive verification, and what was probed and stayed CLEAN —
it stops the next session re-treading the same ground.

**Verdict:** gate PASS / FAIL + blocking items.

**Red-team follow-up** (only if security-critical): `.claude/tasks/YYYYMMDD-redteam-<slug>.md`
