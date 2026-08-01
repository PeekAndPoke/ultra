# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: none**
**SINCE: 2026-08-01 (released by the slumber-as agent)**
**STATE: FREE — take the lock before building.**

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — codegen agent, read this before your first build

`@Slumber.As` landed. `.claude/tasks/20260731-slumber-as-declared-wire-shape.md`; commits `4902d6bf`,
`3237c91a`, `3bff0916`, `92221f35`, `4c4daf69`. What your build will pick up:

- **The `Slumber` annotation nest moved** from `io.peekandpoke.ultra.slumber` to
  `io.peekandpoke.ultra.common.slumber`. **Your import sites are already migrated** — `TypeWalker.kt`
  and `walker_fixtures.kt` were updated in `4902d6bf`. If your uncommitted edits touch those files,
  expect a merge nuisance, not a breakage.
- **The six `Mp*` datetime types now carry `@Slumber.As`**, `@Retention(RUNTIME)`, readable
  reflectively. This is what your follow-on work was waiting for: `MpDateTimeTsContributor`'s claims and
  much of `runtime/datetime.ts` become derivable, and `MpDateTimeFieldParitySpec` is superseded by
  `ultra/slumber`'s `SlumberAsRoundTripSpec`, which checks the same property generically for every
  annotated type. Read §3, §4 and §10 of the task file before starting — §10 records that `Redacted<T>`
  is asymmetric, and that the annotation describes the SLUMBER direction only.
- **`SerializationTuple` became the public `MpDateTimeRawData`** in `ultra:datetime` — same fields, same
  kotlinx behaviour, renamed and made public because it was already the shape class.
- **karango and monko KSP now generate from the declared shape.** `MpInstant$$karango.kt` emits
  `.ts` / `.timezone` / `.human` instead of `.value`, which named a wire key that does not exist. If you
  see stale accessors, delete `build/generated/ksp` in the affected module.
- **The hand-written `.ts` helpers are gone** from `karango/core/.../aql/base_slumber.kt` and
  `monko/core/.../lang/base_slumber.kt`. Call sites now import `io.peekandpoke.ultra.datetime.ts`.

## One thing waiting on you

`funktor/codegen/src/test/kotlin/FunktorCodegenWiringSpec.kt:101` — `Unresolved reference 'codeGen'`,
from your uncommitted edits. It was the only error in the last full sweep. It blocks only
`:funktor:codegen:compileTestKotlin`, so it did not affect the work above — but it does mean **nobody
can use a full-tree sweep as a gate until it compiles.**

## Why this file exists

Kotlin's incremental state under `build/kotlin/` is **not safe against two interleaved gradle builds**.
On 2026-07-30 that produced a task reporting UP-TO-DATE while its outputs were stale, surfacing as an
`AbstractMethodError` wrapped in an `AssertionFailedError` — it read like a logic bug and cost real
debugging time. Recorded in `CLAUDE.md` under "Verification traps".

So: **whoever holds this lock is the only agent that runs gradle or commits.** If `STATE: LOCKED` and
you are not the `HOLDER`: do not run gradle, do not commit, do not stage. Reading, grepping and
planning are all fine.

## The lock covers BUILDING, not EDITING — and that bit

Observed 2026-08-01 while the lock was held: the non-holder kept editing source (`ultra/codegen`,
`funktor/codegen`, two new untracked files), which is not forbidden and is reasonable use of a locked
interval. But it left those modules **not compiling**, so the holder's full-tree sweep failed on errors
that were not its own. That sweep is CLAUDE.md's gate for cross-module changes, and it stops being one.

Worse, it misleads in the other direction: a module that fails to compile blocks everything downstream
of it, so a sweep can report "no errors in your modules" simply because those modules never got
compiled. That happened — `funktor:auth` looked clean only because `funktor:messaging` failed first,
and four call sites would have been missed by trusting it.

**Rule, both agents:**

- Editing while not holding the lock is fine. **Leaving a shared module non-compiling is not** — finish
  a file or revert it before going idle.
- The holder must **scope its verification** to its own modules while the tree is contested, and must
  check that those modules actually COMPILED rather than being skipped behind a failure. `--continue`
  does not save you: a failed dependency still blocks its dependents.
- A full-tree sweep is only evidence when the tree is quiet. Say which it was.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the holder
probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a wait; a
wrongly-taken lock costs a debugging session that looks like a real bug.
