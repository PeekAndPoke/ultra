# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: slumber-as agent**
**SINCE: 2026-08-01**
**STATE: LOCKED — do not run gradle, do not commit.**

---

## Why this file exists

Two agents share `/opt/dev/peekandpoke/ultra` on branch `auth-increments`. File-level separation is
not enough: Kotlin's incremental state under `build/kotlin/` is **not safe against two interleaved
gradle builds**. On 2026-07-30 that produced a task reporting UP-TO-DATE while its outputs were
stale, and it surfaced as an `AbstractMethodError` wrapped in an `AssertionFailedError` — i.e. it
read like a logic bug and cost real debugging time. It is recorded in `CLAUDE.md` under
"Verification traps".

The trigger then was an ABI change to `ultra/log` while another agent built. The work now in flight
is an ABI change to `ultra/common` and `ultra/slumber`, which `ultra/codegen` declares as
`api(project(":ultra:slumber"))` — the same shape, wider blast radius, and both workstreams run KSP.

So: **whoever holds this lock is the only agent that runs gradle or commits.**

## Protocol

- **Read this file before every build and before every commit.** Not once per session — the holder
  changes underneath you.
- If `STATE: LOCKED` and you are not the `HOLDER`: do not run gradle, do not commit, do not stage.
  Reading files, grepping and planning are all fine.
- The holder rewrites this file to `STATE: FREE` when it stops, and lists what changed so the next
  agent knows what its build will pick up.
- The next agent takes the lock by rewriting `HOLDER`, `SINCE` and `STATE` **and committing that
  change first**, before any other work.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the
holder probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a
wait; a wrongly-taken lock costs a debugging session that looks like a real bug.

## Current work under the lock

`.claude/tasks/20260731-slumber-as-declared-wire-shape.md` — `@Slumber.As`, the karango/monko KSP
processors, and removing the hand-written `.ts` path helpers.

Expected to touch:

- `ultra/common/src/commonMain/kotlin/slumber/` — the annotation nest moves here from `ultra:slumber`
- `ultra/slumber/src/commonMain/kotlin/Slumber.kt` — removed by that move; import sites updated
- `ultra/datetime/src/commonMain/kotlin/**` — the six Mp types gain `@Slumber.As`
- `karango/ksp`, `monko/ksp` — read the annotation, generate path accessors
- `karango/core/src/main/kotlin/aql/base_slumber.kt`, `monko/core/src/main/kotlin/lang/base_slumber.kt`
  — the hand-written `.ts` helpers come out
- `ultra/vault`, `ultra/security` — import sites for the moved annotation

**Note for the codegen agent specifically:** the annotation is `@Retention(RUNTIME)` precisely so
`ultra/codegen` can read it reflectively. Once this lands, `MpDateTimeTsContributor`'s claims and much
of `runtime/datetime.ts` become derivable, and `MpDateTimeFieldParitySpec` is superseded by a single
generic round-trip check next to the annotation. That is **your** follow-on work, not this task's —
see §3 and §4 of the task file.
