# DOCS: Database.ensureIndexes / recreateIndexes now return the index report

**Status:** TODO
**Plan:** follow-up from `.claude/tasks-archive/2026-07/20260728-index-ops-return-info.md`
**Security-critical:** no

## Spec

`Database.ensureIndexes()` and `Database.recreateIndexes()` changed from `Unit` to
`List<VaultModels.IndexesInfo>` — the same value `Database.validateIndexes()` returns. The docs still
present ensure and validate as mutually exclusive alternatives, so the new capability is
undiscoverable and a reader who wants both the mutation and the report will call both methods,
paying for the validation pass twice.

Pages to update (docs page and its LLM mirror must move together):

- [ ] `docs-site/src/pages/ultra/karango/kontainer-integration.astro:119-122` — the
      `database.ensureIndexes()` / `// Or validate without creating` / `database.validateIndexes()`
      snippet, and line 150
- [ ] `docs-site/src/data/llms/karango.md:900-903` and `:933` — same snippet in the mirror
- [ ] `docs-site/src/pages/ultra/vault/repository.astro:193-202` — repository-level index section;
      check it does not imply the `Database`-level methods return `Unit`
- [ ] `docs-site/src/pages/ultra/monko/indexes-and-hooks.astro:96-102` +
      `docs-site/src/data/llms/monko.md:545-551` — these are `repo.`-level, which did NOT change.
      Verify only that nothing there contradicts the new `Database`-level behaviour.

## Content notes

Two things worth stating once, in the reference rather than in prose:

- `ensureIndexes()` is **not purely additive** — both drivers drop and re-create an index whose
  definition changed (`karango/core/src/main/kotlin/vault/KarangoIndexBuilder.kt:155`,
  `monko/core/src/main/kotlin/MonkoIndexBuilder.kt:127-131`). Operators assume "ensure" is safe on a
  live database; it briefly removes an index during a rebuild.
- `excessIndexes` in an `ensureIndexes()` result is informational — ensure never removes an
  undefined index. Only `recreateIndexes()` clears those.

Keep it compact per the docs rules: this is one signature change plus two gotchas, not a new page.

## Test evidence

n/a — docs only. Verify the docs site builds.
