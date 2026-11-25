# ULTRA-00359: Karango and Monko: vault as api; Monko remove(entity) fires OnAfterDelete through Repository

> **Status (2026-11-25): DESIGNED.** Maintainer: "Make vault api", "Fix: override"; implementation next.

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

## Acceptance

- A consumer with only `karango:core` or `monko:core` can name Vault types; Monko fires OnAfterDelete on every
  `remove(entity)` path, once; docs say so.

## History

- 2026-11-25: created on the maintainer's decision.
