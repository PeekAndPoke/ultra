# Docs: switchMap / switchMapNotNull

**Status:** TODO
**Plan:** follow-up of `.claude/tasks-archive/2026-08/20260824-streams-switchmap.md`
**Security-critical:** no

## Spec

Document the two new Streams operators. They are public API on a settled, battle-tested library, so
the docs pair must move together.

- [ ] `docs-site/src/pages/ultra/streams/combining.astro` — add a switchMap section (this page hosts
      `combinedWith`/`pairedWith`/`fold`/`history`; switching between inner streams belongs here
      rather than on `operators.astro`)
- [ ] `docs-site/src/data/llms/streams.md` — same section in the LLM mirror (hand-maintained; nothing
      syncs it), plus a row in the platform table near the end of the file
- [ ] `docs-site/src/pages/ultra/streams/extending.astro` — platform summary table
- [ ] `ultra/streams/README.MD` (~line 28) — the "Composable operators" bullet

## What the docs must say

Signatures:

```kotlin
fun <T, R> Stream<T>.switchMap(selector: (T) -> Stream<R>): Stream<R>
fun <T : Any, R> Stream<T?>.switchMapNotNull(selector: (T) -> Stream<R>): Stream<R?>
```

One example — the driving use case:

```kotlin
val fps: Stream<Int?> = playerStream.switchMapNotNull { it.diagnostics }
// non-null variant by composition:
val fpsOrZero: Stream<Int> = playerStream.switchMapNotNull { it.diagnostics }.fallbackTo(0)
```

Gotchas that belong on the page (all confirmed in the review gate):

- Inner streams are compared by **identity**. A selector building a stream per call
  (`switchMap { it.diagnostics.map { … } }`) resubscribes on every outer value — hoist derived
  streams into the owning class instead.
- Switching **releases the old inner before subscribing the new one**, so an upstream shared by the
  inner streams (e.g. one `ticker()` feeding every player's diagnostics) is torn down and restarted
  on each switch. `permanent()` on that upstream prevents it.
- `switchMapNotNull` **resets to null** while the outer is null — it does not hold the last inner
  value. This differs from `foldNotNull`/`historyOfNonNull`, which ignore nulls entirely.
- The selector runs on every value read while nothing is subscribed (same as `map`), and never at
  construction time.

## Test evidence

- [ ] `pnpm build` in `docs-site/` green
- [ ] LLM mirror endpoints render (`/llms/streams.md`, `llms-full.txt`)

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |
