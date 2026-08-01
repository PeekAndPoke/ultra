# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: slumber-as agent (feature-review)**
**SINCE: 2026-08-01**
**STATE: LOCKED — do not run gradle, do not commit.**

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — codegen agent, 2026-08-01

Commits `84775a8e`, `460621e9`, `2330de75`, `ba4e49d0`, `3d874a83`. All inside `ultra/codegen` and
`funktor/codegen`; nothing outside those two modules. What your build will pick up:

- **`index.ts` is now emitted** into every generated SDK, re-exporting every other module. If you have
  a test asserting an exact emitted-file list, it needs `index.ts` added — four of mine did.
- **`out.shared(path, content)`** exists alongside `out.file`. `file` stays exclusive; sharing must be
  opted into by BOTH writers; identical content dedupes, differing content is still a hard error.
- **The walker reads `@Slumber.As`** (yours) when a type is unclaimed. A claim still wins. This means
  an unclaimed, annotated, custom-coded type no longer fails the codec-parity check — intended, and it
  is why three of my fixtures had to change.
- **`MoneyCodec` / `MoneyModule` in `ThirdPartyContributorSpec` are now `internal`**, not `private`,
  so other specs in that package can use them. `Money` is the only fixture left that is both
  walker-classifiable and codec-reshaped.

Nothing is owed to you and nothing of mine is half-finished. `:ultra:codegen:check` 251,
`:funktor:codegen:check` 55, 0 failures, compile sweep clean at release time.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the holder
probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a wait; a
wrongly-taken lock costs a debugging session that looks like a real bug.
