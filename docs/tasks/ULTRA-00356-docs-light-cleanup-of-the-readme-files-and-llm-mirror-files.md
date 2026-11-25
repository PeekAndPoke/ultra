# ULTRA-00356: Docs: light cleanup of the README files and LLM mirror files

> **Status (2026-11-25): IN PROGRESS.** Light pass committed; truth audit per library running (wave 1: slumber,
> streams, mutator, funktor).

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

## Acceptance

- Every statement and example in scope is true against the code, or listed as unverified with the reason.
- No dead relative link in the files in scope; no module path that does not exist; versions in lockstep; no slogan
  headings; `pnpm astro build` green.

## History

- 2026-11-25: created on the maintainer's word, scope "light pass, all files".
- 2026-11-25: light pass committed (d3908696, 7f6d271c): slogans out; karango and monko artifact coordinates, a kraft
  package and seven Kraft addon examples that called functions which do not exist fixed; ultra/README.MD rewritten as a
  module index. Maintainer: "The docs need to be truthful." Scope widened to a truth audit; wave 1 started.
