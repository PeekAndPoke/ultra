# ULTRA-00315: DOCS: ultra:depot and ultra:depot-s3 (own docs page, LLM mirror)

> **Status (2026-11-13): DESIGNED.**

## Why this exists

ULTRA-00311 moved the depot into `ultra:depot` and `ultra:depot-s3` and added data subjects, metadata, hashes,
encryption with shredding and signed repositories (archived 2026-11-13; the code passed its gates and is settled).
Today the depot is documented only as a section of the funktor cluster page
(`docs-site/src/pages/ultra/funktor/cluster.astro`), and no LLM mirror covers it.

## The work

- [ ] A docs section `/ultra/depot/` (overview, repositories and paths, subjects and metadata, `verify`,
      encrypting and signed repositories with the key store, erasure and what it does not cover: backups of the
      key store, plain metadata, signed files; the stored formats' names).
- [ ] `docs-site/src/data/llms/depot.md` and its line in `llms.txt`; the page and the mirror move together.
- [ ] Sidebar and the library listing (`/ultra/`); `site.ts` already has `dep.depot`, `dep.depotS3`,
      `githubSource.depot`.
- [ ] The funktor cluster page keeps the wiring (kontainer, `useKarango()` / `useMonko()`, `DepotApi`) and
      links to the depot's own page for the rest.
- [ ] A "what will bite me" skill for the depot: an inner repository must not be a kontainer service; an erased
      subject id is burnt; only `Intact` proves a file; metadata is plain; `DepotUri` keeps paths as they are.

## History

- 2026-11-13: created on archiving ULTRA-00311 (project rule: a DOCS follow-up when public API changed).
