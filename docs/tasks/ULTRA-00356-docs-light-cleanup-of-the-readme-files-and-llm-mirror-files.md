# ULTRA-00356: Docs: light cleanup of the README files and LLM mirror files

> **Status (2026-11-25): DESIGNED.** Scope chosen by the maintainer; work next.

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

## Acceptance

- No dead relative link in the files in scope; no module path that does not exist; versions in lockstep; no slogan
  headings; `pnpm astro build` green.

## History

- 2026-11-25: created on the maintainer's word, scope "light pass, all files".
