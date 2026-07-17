# funktor/saas module + Organisation storage (Phase O1)

**Status:** IN PROGRESS — storage core done + green (2026-07-17); API feature / ensured-org / fixtures pending
**Plan:** `.claude/plans/20260717-auth-orgs-foundation.md` → Phase O1
**Security-critical:** yes (tenant entity + isolation — red-team follow-up required)

## Spec

New `funktor/saas` module: the `Organisation` (chain) → embedded `Branch` (site) tenant entity,
persisted to both ArangoDB (karango) and MongoDB (monko), with a unique slug.

- [x] Register `:funktor:saas` in `settings.gradle`; `build.gradle.kts` (mirrors messaging, minus email deps)
- [x] commonMain models: `OrgModel`, `BranchModel`, `OrgStatus`
- [x] `Organisation` `@Vault` entity (embedded `List<Branch>`), `Timestamped`
- [x] `OrgsStorage` interface (`Null` + `Vault(Repo)`); `Repo : Repository<Organisation>` adds `findBySlug`
- [x] `KarangoOrgsRepo` (unique persistent index on slug) + `MonkoOrgsRepo` (uniqueIndex on slug)
- [x] `Funktor_Saas` kontainer module + `FunktorSaasBuilder.useKarango()/useMonko()`
- [x] Dual-backend storage spec (`OrgsStorageBaseSpec` + Karango/Monko subclasses)
- [ ] Fixtures (`RepoFixtureLoader` per backend) + kontainer registration
- [ ] `OrgsApiFeature` CRUD (`isSuperUser`), wire into `funktor/all`
- [ ] `EnsuredOrganisation` config + `OnAppStarting` upsert hook (single/default-org apps) + idempotency test

## Implementation notes

- No `Slug` value type in the repo — house convention is plain `String` + unique index (confirmed
  via pattern scan). `OrgStatus` shared enum in commonMain.
- `Repository<T>` already provides `insert`/`save`/`findById`/`findAll`/`removeAll`; only `findBySlug`
  is custom. `findAll()` is overridden by both `EntityRepository` and `MonkoRepository`.
- Test container recreates indexes so the unique-slug constraint is actually enforced in tests.
- Open decision deferred to O2/O3: `OrgModel.id` source (`_id` vs `_key`) — matters for how
  `OrgMembership.orgId` references orgs. Not yet wired.

## Test evidence

- [x] `:funktor:saas:jvmTest` green — `OrgsStorageKarangoSpec` (4/4) + `OrgsStorageMonkoSpec` (4/4)
      against live ArangoDB + MongoDB; includes slug-uniqueness (unique index enforced both backends)
- [ ] ensured-org idempotency (pending that feature)

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

**Red-team follow-up:** to be created when O1 completes (cross-org access via forged/guessed org id,
slug collision across tenants, branch-id spoofing).
