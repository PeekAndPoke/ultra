# ULTRA-00356: Docs: light cleanup of the README files and LLM mirror files

> **Status (2026-11-25): DONE.** Truth audit of every library except datetime (waits for ULTRA-00343, then ULTRA-00339)
> and kontainer (ULTRA-00338); every library reviewed until clean; `pnpm astro build` green (118 pages).

**Part of:** ULTRA-00260 (Epic: Docs backlog)

## Why this exists

Found during ULTRA-00338: `ultra/README.MD` links are dead (written relative to the repo root), it names modules that
moved or were renamed (`ultra/logging`, `ultra/mutator`) and misses most current ones; README headings like
"Done Right"; the kontainer mirror claimed "no reflection tricks". Maintainer (2026-11-25): "Yes fix the dead links.
And clean up the readme files and llms files", scope "Light pass, all files".

## Scope (maintainer, 2026-11-25: light pass, all files)

Every README file in the repo (except generated, build and `docs/tasks` ones) and every LLM mirror file in
`docs-site/src/data/llms/`. Fix:
- dead links and anchors; wrong module paths and names;
- version snippets out of lockstep with `gradle.properties` `VERSION_NAME` and `docs-site/src/data/site.ts`;
- tone: superlatives, bragging and marketing slogans, per `.claude/skills/docs-site/tone-of-voice.md`;
- claims false at a glance (a module, class or function that does not exist; a stated count that is wrong).
- `ultra/README.MD` becomes an accurate index of the modules under `ultra/`.

Not in scope: a statement-by-statement truth audit against the code. That stays with the DOCS tickets under
ULTRA-00260 (ULTRA-00339 datetime, ULTRA-00326 cache, ...). The kontainer files (`ultra/kontainer/README.MD`,
`docs-site/src/data/llms/kontainer.md`) are done with ULTRA-00338 on its own branch.

## Scope widened (maintainer, 2026-11-25): "The docs need to be truthful."

After the light pass the maintainer asked for truth: every statement and example on the docs pages, the LLM mirrors
and the READMEs is checked against the code, like ULTRA-00338 did for Kontainer. Opus auditor per library (brief in the
coordinator's scratchpad: find the code for each claim, fix pages and mirror together, list what cannot be settled),
then one review per library diff. Waves, with the budget checked between them:
- Wave 1: slumber, streams, mutator, funktor (folds in the false spots of the funktor DOCS tickets).
- Wave 2: karango, monko, vault, i18n with maths, cache (ULTRA-00326).
- Wave 3: kraft (pages and mirror split), the remaining READMEs (root, ultra/common, log, meta, security, docs-site,
  release-notes, funktor-demo).
- Datetime waits for ULTRA-00343 (code still changing), then ULTRA-00339.
- Kontainer: done with ULTRA-00338.

## Review ledger (rung 1, reviewer-opus-high, one domain-expert reviewer per library)

| Library | Round 1 | Round 2 | MINORs |
|---|---|---|---|
| streams | clean | | 1 out-of-diff sentence fixed |
| slumber | clean | | 2 fixed |
| mutator | clean | | 3 fixed |
| i18n, maths | clean | | 2 fixed; 3 maths code defects confirmed (to the maintainer) |
| cache | clean | | 2 fixed |
| vault | 3 MAJOR (hook timing in Funktor apps, timeMs 0.0 without profiling, three non-compiling selectors) | clean | 2 fixed |
| monko | 1 MAJOR (ensureIndexes scope) | clean, 2 MINOR fixed | 3 fixed |
| funktor | 1 MAJOR (`Stored` has no `total`) | clean, 1 MINOR fixed | 6 fixed |
| karango | 1 MAJOR (install snippet lacks vault, examples do not compile) | clean, 1 MINOR fixed | |
| kraft parts 1 to 3 | clean each | | 6 fixed; lazy addons are a code defect, fixed in ULTRA-00357 |
| READMEs | clean | | 2 fixed |

Found by the coordinator after the reviews: the streams artifact is `stream` (site.ts, mirror, README); a brace in
slumber/datetime.astro broke the site build.

Code defects found (for the maintainer, not fixed here): Monko `remove(entity)` through `Repository<T>` runs no
hook; karango and monko core keep vault at implementation scope; `Ease.In.elastic` NaN, `Ease.In.bounce` reversed,
`createExponential` ignores `lambda`; AuthRuleBuilder messages still advise `public()` inside `authorize`; the
sdkgen-app and b2b-app dev servers share port 36591. Each is documented as it is today.

## Acceptance

- Every statement and example in scope is true against the code, or listed as unverified with the reason.
- No dead relative link in the files in scope; no module path that does not exist; versions in lockstep; no slogan
  headings; `pnpm astro build` green.

## History

- 2026-11-25: created on the maintainer's word, scope "light pass, all files".
- 2026-11-25: light pass committed (d3908696, 7f6d271c): slogans out; karango and monko artifact coordinates, a kraft
  package and seven Kraft addon examples that called functions which do not exist fixed; ultra/README.MD rewritten as a
  module index. Maintainer: "The docs need to be truthful." Scope widened to a truth audit; wave 1 started.
- 2026-11-25: truth audit done for streams, slumber, mutator, i18n, maths, cache, vault, monko, funktor, karango,
  kraft and the remaining READMEs; ledger above. Site builds (118 pages). Ready to merge.
