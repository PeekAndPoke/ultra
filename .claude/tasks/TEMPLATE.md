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

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up** (only if security-critical): `.claude/tasks/YYYYMMDD-redteam-<slug>.md`
