# Docs: switchMap / switchMapNotNull

**Status:** DONE (archived 2026-08-24)
**Plan:** follow-up of `.claude/tasks-archive/2026-08/20260824-streams-switchmap.md`
**Security-critical:** no

## Spec

Document the two new Streams operators. They are public API on a settled, battle-tested library, so
the docs pair must move together.

- [x] `docs-site/src/pages/ultra/streams/combining.astro` — new `switchMap` section
- [x] `docs-site/src/data/llms/streams.md` — same material in the LLM mirror, plus the platform-table row
- [x] `docs-site/src/pages/ultra/streams/extending.astro` — platform summary table
- [x] `ultra/streams/README.MD` — the "Composable operators" bullet
- [x] `docs-site/src/pages/ultra/streams/operators.astro` — cross-link (added during review; see below)

`llms.txt` deliberately untouched: its Streams entry is a single mirror link with no per-page URL
bullets, and the "adding a page means adding its URL" rule is scoped to pages. No page was added,
only a section. `llms-full.txt` picks the section up automatically — `llmsTemplate.ts` concatenates
the per-library mirrors.

## Implementation notes

- Placement is `combining.astro`, not `operators.astro`. Everything on the operators page is a
  per-value transform; switchMap's defining behaviour is the subscription lifecycle across two
  streams, which is the territory `combinedWith` already occupies. The counter-argument is real —
  the name is map-shaped and Rx/Flow refugees will look under "Operators" first — so that is paid
  for with a cross-link rather than by moving subscription semantics onto a page that has none.
- The section carries five behaviours a caller hits immediately: reset-to-null (vs `foldNotNull`
  holding), identity comparison and the hoisting workaround, shared-upstream teardown on each
  switch, no de-duplication on switch, and the selector running per unsubscribed read but never at
  construction.
- Exception-swallowing during a switch is deliberately NOT documented here: it is `notifyHandlers`
  behaviour shared by every operator (`streams.kt:21-31`) and the mirror already has an "Error
  resilience" section. Filing it under switchMap would imply it is specific to this operator.

## Test evidence

- [x] `pnpm build` in `docs-site/` green — 113 pages, no errors
- [x] LLM mirror renders: `dist/llms/streams.md` and `dist/llms-full.txt` both contain the section;
      no unsubstituted `{{ultraVersion}}` placeholders
- [x] Platform-table row updated in both surfaces (`extending.astro` and the mirror)
- [x] The cross-link's anchor resolves: `<h2 id="switchmap">` exists in the built page, matching the
      site's existing explicit-id convention (`kontainer/advanced#debug-tools`)
- [x] Mirror section (37 lines) is shorter than the page section (60) — the CLAUDE.md rule
- [x] Swept `index.astro`/`getting-started.astro`/`streams-sidebar.ts` for stale operator
      enumerations — none; neither page lists operators exhaustively and no new page means no
      sidebar entry

## Review record (/feature-review, 2026-08-24)

Scoped to two reviewers. The third charter in the skill is security, which has no surface on a prose
change — no code ships from these files. Recorded rather than silently skipped.

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Technical accuracy (claims vs code) | PASS with 1 real error | all 10 enumerated claims CONFIRMED against `ops/switchMap.kt` + the 27 specs; 1 wrong example; 2 missing behaviours; 1 phantom operator in the README |
| 2. Docs craft & conventions | PASS with findings | Aside was Voice 1 in a Voice 2 box; 2 claims present only in the mirror (pairing drift); section oversized; 4 LOW |
| 3. Security | n/a — no attack surface in prose | — |

**Fixed:**

1. **The flagship example was wrong** — it never subscribed, and `switchMap` is lazy, so
   "playerA's is released" described something that never happened. It now subscribes. I had
   rewritten that same example once already for a different defect and reintroduced the flaw;
   the reviewer caught what my own re-read did not.
2. **Voice 2 rewrite.** The Aside stated mechanism instead of owning a trade-off. Mechanism moved
   to Voice 1 body prose; the Aside now admits the alternative (overlapping subscriptions) and why
   it was not taken.
3. **Pairing drift closed** — the `foldNotNull` contrast and the selector-per-read note existed only
   in the mirror and are now on the page too.
4. **Two missing behaviours added** to both surfaces: switching does not de-duplicate (use
   `.distinct()`), and laziness.
5. **Trimmed** a one-line `<Code>` block into prose; comment alignment normalised to the page's
   two-space convention; mid-sentence bold dropped from the mirror; markdown table realigned.
6. **Two pre-existing README errors fixed** on the line this diff already touched:
   `distinctUntilChanged` does not exist anywhere in the repo (the operators are `distinct()` /
   `distinctStrict()`), and "Works on JVM and JS" omitted Native — the module has `linuxX64` tests
   that were run green earlier today.

**Not actioned:** a suggestion to open the section with the `switchMapNotNull` example. The non-null
`switchMap` is the base operator and leading with the nullable variant inverts the explanation; the
underlying complaint (the opening example taught nothing observable) was fixed instead by making it
subscribe and print.
