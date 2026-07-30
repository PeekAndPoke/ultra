# Library know-how skills (kontainer, kraft, karango, slumber, streams, …)

**Status:** TODO — proposed 2026-07-28, not started
**Plan:** none (standalone)
**Security-critical:** no

## Why

Agent context about this repo's libraries currently lives nowhere durable. It was in machine-local
memory, which does not travel between machines — the reason this task exists at all. The proposal is
one skill per library, tree-structured: a main entry point with core ideas and know-how, plus detail
files for specific topics. Skills are also portable to other projects, which flat notes are not.

## THE constraint — do not create a third copy

There are already two things to keep in sync: the docs site (`docs-site/src/pages/ultra/*`) and its
LLM mirror (`docs-site/src/data/llms/*.md` + `llms.txt` / `llms-full.txt`). A skill that restates
"what Karango is and how to query with it" makes it **three**, and this repo's own rule is that stale
docs are worse than none.

**So the skills must be disjoint from the docs by construction:**

| Question | Answered by |
|---|---|
| "How do I use Karango?" | docs site + LLM mirror |
| "What will bite me? What does the maintainer know that isn't written down?" | **the skill** |

For API reference a skill should POINT at `docs-site/src/data/llms/<lib>.md`, never copy it. That
keeps the sync burden at two, and it means the skills drift with *code* changes only — a much smaller
surface than drifting with every docs edit.

**Acceptance test for any line in one of these skills:** if it would be equally at home on a docs
page, it does not belong in the skill.

## What actually belongs in them

Real examples already collected, all of which are invisible from a signature and absent from the docs:

- **Kontainer** — a singleton that transitively injects a `dynamic` becomes `SemiDynamic`, i.e. one
  instance per request. Shared-state services must take zero constructor dependencies. Corollary:
  "the constructor runs once at boot" is false for realms. This produced a wrong claim in a KDoc on
  2026-07-28.
- **Karango** — creates COLLECTIONS and indexes (`ensureRepositories` / `recreateIndexes`) but never a
  DATABASE, so a missing one surfaces as an opaque Arango 1228. Test DBs must be created by hand.
- **Mutator** — `ListMutator.subList()` throws `UnsupportedOperationException`
  (`mutator/core/src/commonMain/kotlin/ListMutator.kt:260`); stdlib `chunked()` calls it internally,
  so even indirect use crashes. Also: Mutator is NOT battle-tested despite its age.
- **Streams** — always publish a value, even `null`. A deliberate departure from Rx, and the reason
  `invoke(): T` can return synchronously. (Philosophy — may already be on the docs site; if so, link
  rather than restate.)
- **Kraft** — `TestBed.preact { }` runs real browser behaviour; don't mock what you can run.

## Spec

- [ ] Decide the library set and priority. Kontainer first — it is the most battle-tested, the most
      widely used internally, and has the sharpest trap.
- [ ] Agree a tree shape: `SKILL.md` (core ideas + know-how + index) + `<topic>.md` detail files.
      Use `.claude/skills/skill-builder/` for the conventions.
- [ ] Write skills so they are portable — no assumptions about this repo's paths beyond a clearly
      marked "in this repo" section.
- [ ] Each skill links its docs/LLM-mirror counterpart for API reference instead of duplicating it.
- [ ] Add a line to `CLAUDE.md` → Documentation about the docs / LLM-mirror / skill split, so the
      boundary is stated once and enforced.

## Open questions

- **Do the JS-addon and Funktor-module surfaces need this too**, or only the core libraries?
- **How is drift caught?** Nothing verifies a skill's claims against the code. The cheapest guard is
  to cite `path/File.kt:line` for every concrete claim (as above) so a reviewer can check it in one
  jump — worth making a rule in the skill template.
- Verified 2026-07-28 that flat notes DO rot: the previous ones claimed `funktor/funktor-demo/` (now
  `funktor-demo/`) and listed a `konva` Kraft addon that no longer exists while missing `datetime`,
  `pixijs` and `threejs`. Whatever shape this takes has to survive that.

## Test evidence

n/a — documentation work. The check is that a fresh session, given only the skill, can avoid the
traps listed above.
