# ULTRA-00359: Karango and Monko: vault as api; Monko remove(entity) fires OnAfterDelete through Repository

> **Status (2026-11-25): DONE.** Gate PASS in round 1 (no findings); merged into master.

## Why this exists

Found by the ULTRA-00356 truth audit, both confirmed by reviewers:
1. `karango/core/build.gradle.kts:42` and `monko/core/build.gradle.kts:43` declare `implementation(project(":ultra:vault"))`
   (and kontainer, reflection, datetime, log at implementation too), although `Repository`, `Stored` and `Cursor` are in
   their public API. A consumer with only `karango:core` or `monko:core` cannot compile the getting-started examples;
   the docs currently tell users to add `io.peekandpoke.ultra:vault` themselves.
2. `MonkoRepository` does not override `Repository.remove<X : T>(entity: Stored<X>)`; it adds
   `@JvmName("removeStored") suspend fun remove(stored: Stored<T>)` (MonkoRepository.kt:181-186). A call through a
   `Repository<T>` reference goes to the interface default `remove(entity._id)` (ultra/vault Repository.kt:324), which
   deletes but runs no OnAfterDelete hook. Karango overrides it (EntityRepository.kt:304-308).

## Decisions (maintainer, 2026-11-25)

- `api(project(":ultra:vault"))` in karango/core and monko/core. Vault already exposes kontainer, reflection, datetime,
  log, slumber and common as api, so the other implementation entries may stay.
- MonkoRepository overrides `Repository.remove(entity)` like Karango, so the hook fires on every path.

## The work

- Monko: a test first, RED today: `remove(entity)` called through a `Repository<T>` reference fires OnAfterDelete
  exactly once; the direct `MonkoRepository` call still fires it once (not twice). Then the override; drop or keep the
  `removeStored` overload as fits (no double hook).
- Build: vault as api in both modules. Check that no consumer in the repo breaks (compile sweep of funktor and demo).
- Docs: drop the "add vault" lines and the "Monko: through Repository<T> no hook" notes (vault getting-started and
  repository pages, karango and monko getting-started, the LLM mirrors vault.md, karango.md, monko.md, karango/README.MD).
- Mutation check of the override; monko and funktor storage specs green (both DB backends where storage is touched).

## Implementation notes

- `monko/core/src/main/kotlin/MonkoRepository.kt:181-187`: `override suspend fun <X : T> remove(entity: Stored<X>)`
  replaces the `@JvmName("removeStored")` overload (dropped, not kept: both would match a `Stored<T>` argument, and
  one method is the only way to rule out a double hook). It deletes by `entity._key` and fires the after-delete hooks
  through the driver hook scope, as the overload did. Nothing in the repo called `removeStored`. JVM binary note: the
  public JVM method `removeStored` is gone; Kotlin callers resolve to the override unchanged.
- `karango/core/build.gradle.kts:44`, `monko/core/build.gradle.kts:45`: `api(project(":ultra:vault"))`. The generated
  POMs now carry `vault-jvm` at compile scope; the vault-jvm POM carries logging, kontainer, reflection, slumber,
  cache, common and datetime at compile scope, so Kontainer, Kronos, logging and `kType()` come with the backends.
  The other implementation entries stay, per the decision.
- Docs: dropped the "add vault" lines (karango and monko getting-started, `karango/README.MD`, LLM mirrors
  `karango.md` and `monko.md`), rewrote the vault getting-started "Dependency" paragraph and the matching line in
  `vault.md` (Karango, Monko and Funktor expose it), and removed the "Monko: through Repository<T> no hook" notes
  (`vault/repository.astro` table, `vault.md` hook table footnote and execution-order line). Repo-wide grep for
  `does not expose`, `runtime only`, `ultra:vault:`, `dep.vault`, `removeStored` and Monko hook notes over
  docs-site/src, READMEs, module sources and `.claude/`: no hits left.

## Test evidence

- New spec `monko/core/src/test/kotlin/io/peekandpoke/monko/MonkoRepositoryRemoveHookSpec.kt`, against the
  worktree's MongoDB (`ultra_wt_vault_api`): remove(entity) through a `Repository<T>` reference fires OnAfterDelete
  once; the direct `MonkoRepository` call fires it once; `remove(idOrKey)` deletes and fires none. Each checks the
  delete count and that the document is gone.
- RED before the fix: 3 tests, 1 failure, "Collection should have size 1 but has size 0" at the hook assertion of the
  `Repository<T>` case (the document was deleted, count 1, hook not run).
- Mutants, each RED, file restored from a cp backup, cmp identical: M1 no hook call in the override (2 failures, hook
  size), M2 hook fired twice (2 failures, hook size 2), M3 delete with a wrong key (2 failures, delete count),
  M4 override removed so the interface default runs (2 failures, hook size).
- Green, counts from the test-results XML: monko:core 268/0, karango:core 1652/0, funktor auth 322/0, saas 50/0,
  logging 14/0, messaging 67/0, cluster 162/0 (failures/errors 0 everywhere; these funktor suites run their Karango
  and Monko storage specs, both backends).
- Compile sweep after the build change, all executed and clean (no `e:`): funktor all, cluster, auth, messaging,
  logging, saas, core (main and test), funktor-demo:server (main and test), karango:ksp and monko:ksp (main and
  test), root project.
- Docs site: `pnpm install --frozen-lockfile` and `pnpm astro build` complete, 117 pages.

## Review ledger

Round 1, standard gate, one reviewer covering the three charters (implementation and code style, domain, security),
rung 1 reviewer-opus-high (Opus high while the Fable credits are out). Verdict: PASS, NO FINDINGS.

CLEAN: override signature matches Repository.remove; one remove(entity) path, so no double hook; remove(idOrKey) and
removeAll run no hooks; hooks go through the driver's VaultHookScope as on save (request isolation unchanged); no
Kotlin or KSP-generated caller resolves to a different document; funktor registers no OnAfterDelete on Monko; `_key`
and `_id` select the same document under ULTRA-00138 (keys never contain a slash); the docs are true and nothing
left tells users to add vault. Noted, not findings: `removeStored` is a JVM binary break for already-compiled direct
callers (CHANGELOG); hooks fire even when nothing was deleted, on both drivers (older); `dep.vault` in site.ts is now
unused.

## Acceptance

- A consumer with only `karango:core` or `monko:core` can name Vault types; Monko fires OnAfterDelete on every
  `remove(entity)` path, once; docs say so.

## History

- 2026-11-25: created on the maintainer's decision.
- 2026-11-25: IN PROGRESS. Implemented on wt/vault-api (Monko override with RED-first e2e and four mutants, vault as
  api in both modules, docs); review gate next.
- 2026-11-25: gate PASS round 1, no findings; merged into master.
