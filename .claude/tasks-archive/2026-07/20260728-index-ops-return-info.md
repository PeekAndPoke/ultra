# Database.ensureIndexes / recreateIndexes return the index state

**Status:** DONE (archived 2026-07-28)
**Plan:** none — spotted while writing KDoc for `ultra/vault/src/jvmMain/kotlin/Database.kt`
**Security-critical:** no

## Spec

`Database.validateIndexes()` returns `List<VaultModels.IndexesInfo>`, but `ensureIndexes()` and
`recreateIndexes()` return `Unit` — even though both already call `repo.validateIndexes()` and throw
the result away. A caller that ensures indexes cannot find out whether the indexes are now correct
without a second full round-trip to the database.

- [x] `Database.ensureIndexes(): List<VaultModels.IndexesInfo>` — `Database.kt:57`
- [x] `Database.recreateIndexes(): List<VaultModels.IndexesInfo>` — `Database.kt:66`
- [x] Both ensure/recreate every repository first, then `return validateIndexes()` once
- [x] Per-repo logging (Karango's `[OK]`/`[MISSING]`/`[EXCESS]`) is preserved — same number of
      `repo.validateIndexes()` calls as before, just moved into a final pass
- [x] Callers still compile

## Implementation notes

Shape: loop the mutation over all repos, then a single `return validateIndexes()` at the end. This
moves validation from interleaved (ensure A, validate A, ensure B, validate B) to a final pass, so
the returned state reflects the database *after* all repositories are done. Same log lines, same
count of validation calls.

**Callers that break.** `VaultIndexesEnsureCommand.kt:13` and `VaultIndexesRecreateCommand.kt:13`
use `override fun run() = runBlocking { database.ensureIndexes() }`. An expression body infers its
return type from the call, so a non-`Unit` return makes them stop overriding `CliktCommand.run(): Unit`
— a compile error, not a silent change. Fix: block body, as `VaultIndexesValidateCommand.kt:13`
already has.

Lambda callers are fine — `AppSpec.kt:71` and `funktor/saas/src/jvmTest/kotlin/index_jvmTest.kt:31`
coerce to `Unit`.

**Out of scope:** `Repository.ensureIndexes()` / `recreateIndexes()` keep returning `Unit`. Changing
those touches the Karango and Monko overrides and every repository implementation; the `Database`
level is where callers actually want the report.

**Follow-up worth considering:** the three vault CLI commands print nothing at all and always exit 0,
even when index creation failed — Karango logs `[ERROR]` and returns normally after 3 attempts
(`karango/core/src/main/kotlin/vault/EntityRepository.kt:193-207`), Monko only logs a warning
(`monko/core/src/main/kotlin/MonkoRepository.kt:70-72`). Now that ensure and re-create return the same
info as validate, all three commands could print missing/excess per repo and exit non-zero. Note the
predicate to reuse is `RepositoryInfo.hasErrors` (`ultra/vault/src/commonMain/kotlin/VaultModels.kt:14`)
— `IndexesInfo` itself has no `hasErrors`. Not done here — that is a UX change, not plumbing.

**Known gap in what the report can tell you (pre-existing, not introduced here):** Monko decides index
health by NAME only (`MonkoIndexBuilder.matches()`, `monko/core/src/main/kotlin/MonkoIndexBuilder.kt:88-91`)
while `differsFrom()` compares keys, uniqueness, sparse and partial filter (lines 97-110). A stale
Mongo index can therefore be reported as healthy. Karango compares name *and* fields, but ignores
unique/sparse/TTL. This diff does not change that — but it promotes the value from a discarded
side-effect to an advertised return, so the report should not be treated as authoritative until
`matches()` reuses `differsFrom()`.

## Test evidence

- [x] Unit/behavior tests — 4 added to `ultra/vault/src/jvmTest/kotlin/DatabaseSpec.kt`, covering the
      returned info, the ensure-all-then-validate-all ordering, and a still-missing index
- [x] Mutation-checked, both mutations caught (1 failure each, restored from a `cp` backup after):
      - `ensureIndexes` back to interleaved `map { ensure; validate }` → ordering test fails
      - `recreateIndexes` → `return emptyList()` → test fails
- [x] Full test command run + green: `./gradlew :ultra:vault:jvmTest` — `DatabaseSpec` 21/21,
      confirmed in `build/test-results/jvmTest/TEST-*.xml`, not from the gradle summary
- [x] Compile check across every caller: `:karango:core :monko:core :funktor:testing :funktor:cluster`

The test fixture needed two *distinct* repository subclasses (`RecordingRepoA`/`RecordingRepoB`), not
two instances of one class — `SimpleLookup` keys repositories by runtime class
(`ultra/common/src/commonMain/kotlin/lookup.kt:44`), so same-class instances collapse and the second
repo would silently vanish from the test.

No e2e test: this is return-value plumbing in vault core, with no storage behaviour of its own. The
real index behaviour it reports on is already covered by the Karango/Monko repository specs.

## Review record (filled by /feature-review)

Run 2026-07-28, three opus reviewers over the 4-file diff. Every finding was re-verified against the
code by the coordinator before being accepted; the verified-clean list below is recorded so the next
session does not re-tread it.

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | PASS | 1 MEDIUM (return shape — deferred to maintainer), 1 LOW (docs follow-up missing → created) |
| 2. Domain expert | PASS | 1 LOW confirmed and fixed (KDoc understated ensure); 1 MEDIUM downgraded to a documented caveat; 1 MEDIUM out of scope (pre-existing Monko defect) |
| 3. Security | PASS | 0 introduced by this diff; 1 LOW pre-existing (CLI always exits 0) |

**Fixes applied:**

1. `Database.kt:56-62` — KDoc on `ensureIndexes()` said "Creates missing indexes", which reads as
   purely additive. Both drivers drop and re-create an index whose definition changed
   (`karango/core/src/main/kotlin/vault/KarangoIndexBuilder.kt:155`,
   `monko/core/src/main/kotlin/MonkoIndexBuilder.kt:127-131`). Reworded, and noted that
   `excessIndexes` is informational for ensure because only `recreateIndexes()` clears those.
2. This task file's follow-up note claimed the CLI could exit non-zero on `hasErrors`. Wrong —
   `hasErrors` is on `VaultModels.RepositoryInfo` (`VaultModels.kt:14`), not on `IndexesInfo`.
   Corrected, so the follow-up does not start from a false premise.
3. Created `.claude/tasks/20260728-docs-index-ops-return-info.md` — public API changed and the docs
   still present ensure and validate as alternatives (`docs-site/src/pages/ultra/karango/kontainer-integration.astro:119`
   and its mirror `docs-site/src/data/llms/karango.md:900`).

**Open, for the maintainer to decide (NOT fixed):** two reviewers independently flagged that
`List<IndexesInfo>` carries no repository identity, so a caller must positionally zip it against
`getRepositories()` — an ordering contract that holds (`SimpleLookup` is insertion-ordered,
`ultra/common/src/commonMain/kotlin/lookup.kt:43`) but is neither documented nor tested. The strongest
evidence is that the domain's one real reporting consumer, `VaultApi.listRepositories`
(`funktor/cluster/src/jvmMain/kotlin/vault/api/VaultApi.kt:22-29`), does not use
`Database.validateIndexes()` at all — it iterates repos itself precisely to keep name and connection.
Left as-is: the brief was explicitly "return the same as `validateIndexes()`", and changing the shape
would mean changing `validateIndexes()` too. Revisit if the CLI follow-up gets built.

**Verified clean** (probed, no issue): ordering change is safe — no driver keeps state between ensure
and validate, both build a fresh builder and read live server state; `repositories.all()` is
insertion-ordered and instance-cached, so the ensure pass and validate pass see the same repos in the
same order; `repo.validateIndexes()` is still called exactly once per repo, so log volume is
unchanged; exception semantics improved (a throwing validate no longer skips later repos' ensure); all
5 call sites checked; no new HTTP exposure — `IndexesInfo` reaches an endpoint only via `VaultApi`,
which this diff does not touch and which sits behind `authFloor = { isSuperUser() }` (`VaultApi.kt:11`);
`recreateIndexes()` remains CLI-only; test fixtures are file-private and relax nothing.

**Noted, not blocking:** changing the return type from `Unit` is binary-incompatible for anything
already compiled against the published vault artifact (`NoSuchMethodError`). Source-compatible for
every in-repo caller. Fine per the project's no-migration-support stance, but vault is a released
library — worth a line in the release notes.