# Karango: exclude soft-deleted rows from unique indexes

**Status:** TODO
**Plan:** follow-up from `.claude/tasks/20260728-vault-scan-findings.md` → S2
**Security-critical:** no

Sibling task: `.claude/tasks/20260729-monko-softdelete-unique-indexes.md`. **This one is the harder
of the two** — Monko's builder already supports partial filters; Karango's does not, and the index
analyzer cannot currently see the difference.

## Problem

A soft-deleted row still occupies its unique index, so uniqueness checks treat tombstones as live.

`KarangoOrgMembersRepo.kt:41-48` declares `persistentIndex { org; userId; unique(true) }`.
`OrgMembersStorage.remove(m)` only sets `softDelete`, so the document stays and keeps its index entry.
Re-adding a previously-removed member fails with a raw Arango 1210 out of `repo.insert` rather than a
domain outcome. `OrgMembersStorageBaseSpec.kt:127` pins the broken behaviour for both backends.

## Why this is not the same fix as Monko

**ArangoDB has no partial-index feature equivalent to MongoDB's `partialFilterExpression`.** It has
*sparse* indexes, which skip documents where the indexed attribute is missing or null — that is the
only lever.

**Determine this first, before designing anything.** Check the ArangoDB version actually in use and
its current index documentation rather than trusting the paragraph above — if a newer server does
support a filtered/conditional index, this whole task collapses to the Monko shape.

If sparse is the only lever, the pattern is a **discriminator field that is null exactly when the row
is soft-deleted**, with a sparse unique index over it:

- entity carries e.g. `uniqueOrgUser: String?` = `"$org/$userId"` while live, `null` once deleted
- unique + sparse index on that single field
- the write path must keep the field in sync with `softDelete` — ideally in one place, not per repo

Options to weigh: a computed property maintained by an `OnBeforeSave` hook; ArangoDB server-side
`computedValues` on the collection (check availability for the version in use); or accepting the
limitation and enforcing uniqueness in application code under the existing per-org lock.

## Two code areas to change

### 1. `KarangoIndexBuilder` — no way to declare sparse today

`PersistentIndexBuilder` (`karango/core/src/main/kotlin/vault/KarangoIndexBuilder.kt:212-244`) exposes
only `options(block: PersistentIndexOptions.() -> Unit)`, passing the driver's option object straight
through. Whether `sparse` is reachable that way needs checking; either way there is no first-class
`sparse()` on the builder as there is on Monko's `UniqueIndexBuilder`.

Compare `MonkoIndexBuilder.UniqueIndexBuilder` (`monko/core/src/main/kotlin/MonkoIndexBuilder.kt:182-222`)
for the shape to aim at — and note its KDoc already warns that MongoDB's `sparse` only excludes
*missing* fields, not null ones. Arango's sparse excludes both missing and null. Do not assume the two
behave identically.

### 2. The index analyzer is blind to this — fix it in the same pass

`KarangoIndexBuilder.matches()` (`:39-42`) compares **name and field paths only**:

```kotlin
this.getEffectiveName() == index.name && this.getFieldPaths() == index.fields.toList()
```

It ignores `unique`, `sparse`, TTL `expireAfter` — everything else. Consequences that make this task
unsafe without fixing it:

- `validateIndexes()` reports an index as **healthy** when the deployed one is non-sparse and the
  definition says sparse. So the very change this task makes would be invisible to validation.
- `ensureIndexes()` decides "keep vs re-create" through `createHelper` (`:150-162`), which compares
  **fields only**. A name-matching index whose sparse/unique flags differ is KEPT, so an existing
  deployment would never actually get the new index.

That second point means the fix does not roll out to existing databases at all until `matches()` and
`createHelper` compare the full index shape.

Monko's analyzer has a milder version of the same defect and is tracked separately in the scan
findings (`MonkoIndexBuilder.matches()` compares the name only, while `differsFrom()` compares the
real shape).

## Spec

- [ ] Confirm what ArangoDB (version in use) actually supports for conditional/partial indexes
- [ ] `matches()` and `createHelper` compare the full index shape, not just name + fields
- [ ] `validateIndexes()` reports a sparse/unique mismatch as unhealthy
- [ ] `KarangoIndexBuilder` can declare a sparse unique index first-class
- [ ] `KarangoOrgMembersRepo` excludes soft-deleted rows from its unique index
- [ ] Invert `OrgMembersStorageBaseSpec.kt:127` — re-adding a soft-deleted member must SUCCEED
- [ ] Audit `KarangoOrgsRepo.kt:37` and any other `unique(true)` for the same exposure

## Careful with

- **Existing deployments.** Once `matches()` gets stricter, previously "healthy" indexes start
  reporting as missing/excess and `ensureIndexes()` will drop and rebuild them. On a large collection
  that is a real outage window. Decide whether this needs a staged rollout.
- **Karango's ensure is already silent about failure** — it logs `[ERROR]` and returns normally after
  three attempts (`EntityRepository.kt:193-207`), so a failed rebuild does not fail a deploy.
- Arango refuses to build a unique index that existing data violates; a database holding a live row
  plus a soft-deleted duplicate needs a migration path.

## Test evidence

- [ ] Unit: `matches()` distinguishes sparse from non-sparse, and unique from non-unique
- [ ] End-to-end against a real ArangoDB: add → remove → re-add succeeds; duplicate of a LIVE row
      still fails
- [ ] End-to-end: changing an index definition's sparse flag causes `ensureIndexes()` to actually
      rebuild it on a pre-existing collection — this is the regression that proves point 2
- [ ] Mutation-check both the analyzer change and the index change
- [ ] `./gradlew :karango:core:test :funktor:saas:jvmTest :funktor-demo:server:test`
