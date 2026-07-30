# Monko: exclude soft-deleted rows from unique indexes

**Status:** TODO
**Plan:** follow-up from `.claude/tasks/20260728-vault-scan-findings.md` → S2
**Security-critical:** no

## Problem

A soft-deleted row still occupies its unique index, so uniqueness checks treat tombstones as live.

`MonkoOrgMembersRepo.kt:40-43` declares `uniqueIndex { org; userId }` with no partial filter.
`OrgMembersStorage.remove(m)` only sets `softDelete`, so the document stays in the collection and
keeps its index entry. Re-adding a previously-removed member therefore fails with a raw
`MongoWriteException` (E11000) out of `repo.insert` — a 500 — rather than the domain outcome the rest
of the surface returns.

`OrgMembersStorageBaseSpec.kt:127` currently *pins* the broken behaviour (`shouldThrowAny { members.add(...) }`
after a remove), so that expectation has to be inverted as part of this task.

`B2bMembersApi` avoids the trap only by routing every add through `findByOrgAndUserIncludingDeleted`
(`B2bMembersApi.kt:186`). `add()` is public API — a second caller (operator surface, fixtures, an
invite job) that skips that dance breaks.

## The builder already supports this — this task is about USING it

**No `MonkoIndexBuilder` change is needed.** `UniqueIndexBuilder.partial(filter)` already exists
(`monko/core/src/main/kotlin/MonkoIndexBuilder.kt:205-215`) and `buildOptions()` already applies it as
`partialFilterExpression` (`:218-221`). Nothing in the repo uses it yet — grep for `partialFilter`
returns only the builder itself.

Ensure/validate already handle it too: `differsFrom()` compares `partialFilterExpression`
(`MonkoIndexBuilder.kt:105-107`), so a definition change is detected and the index is dropped and
re-created.

## Spec

- [ ] `MonkoOrgMembersRepo` unique index gains a partial filter excluding soft-deleted documents
- [ ] Decide and document the filter shape — the marker is `SoftDelete.deletedAt`, so something like
      `Document("softDelete", Document("\$type", "null"))` or an `\$exists: false` form. Verify against
      what `StoredSlumberer` actually writes for an absent `SoftDelete`; do not guess the field name
      or its serialized shape
- [ ] Invert `OrgMembersStorageBaseSpec.kt:127` — re-adding a soft-deleted member must SUCCEED
- [ ] Audit every other `uniqueIndex { }` in the repo for the same exposure. `KarangoOrgsRepo`
      has a unique index too — check whether `Org` is `SoftDeletable`
- [ ] Verify the index is actually re-created on an existing database, not just on a fresh one:
      changing the definition must make `ensureIndexes()` drop and rebuild it

## Careful with

**MongoDB will not build a partial unique index if existing data violates it.** A database that
already holds a live row plus a soft-deleted duplicate will fail index creation, and
`MonkoRepository.ensureIndexes()` only logs a warning (`MonkoRepository.kt:70-72`) — so the index
silently ends up missing rather than the deploy failing. Decide whether that needs a migration step
or a louder failure.

## Test evidence

- [ ] Unit: builder emits the expected `partialFilterExpression`
- [ ] End-to-end against a real MongoDB: add → remove → re-add the same member succeeds, and adding a
      duplicate of a LIVE member still fails
- [ ] Mutation-check: drop the partial filter → the re-add test must fail
- [ ] `./gradlew :monko:core:test :funktor:saas:jvmTest :funktor-demo:server:test`
