# funktor/saas module + Organisation storage (Phase O1)

**Status:** IN REVIEW — implementation complete + green (2026-07-18); pending `/feature-review` gate
**Plan:** `.claude/tasks/20260717-auth-orgs-foundation.md` → Phase O1
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
- [x] Fixtures (`RepoFixtureLoader` per backend) + kontainer registration (also gives API-test isolation)
- [x] Wire `funktor:saas` into `funktor/all` (build dep + `funktor()` `saas` param + `Funktor` module + `FunktorParams`)
- [x] `OrgsApiFeature` CRUD (list/get/create/update, `isSuperUser`), auto-mounted via `ApiFeature`;
      commonMain `OrgsApiClient` typed endpoints; `asApiModel` mapper (id = vault `_key`)
- [x] `OrgsStorage.ensureBySlug()` idempotent upsert (race-safe) + idempotency tests both backends
- [x] `EnsureOrganisationOnAppStarting` hook + `FunktorSaasBuilder.ensureOrganisation(slug, name)`;
      wired into the funktor/all test blueprint (`ensureOrganisation("system-default", ...)`)
      — boots cleanly (verified via AuthApiSpec); explicit "org exists" assertion lands with the API list test

## Implementation notes

- No `Slug` value type in the repo — house convention is plain `String` + unique index (confirmed
  via pattern scan). `OrgStatus` shared enum in commonMain.
- `Repository<T>` already provides `insert`/`save`/`findById`/`findAll`/`removeAll`; only `findBySlug`
  is custom. `findAll()` is overridden by both `EntityRepository` and `MonkoRepository`.
- Test container recreates indexes so the unique-slug constraint is actually enforced in tests.
- **DECIDED (review-confirmed):** `OrgModel.id` = vault `_key` (not `_id` = "coll/key"). `_key` is
  backend-portable and collection-independent; both backends' `findById` accept key-or-id. This is
  the stable reference O2's `OrgMembership.orgId` will use.

## Test evidence

- [x] `:funktor:saas:jvmTest` green — `OrgsStorageKarangoSpec` (6/6) + `OrgsStorageMonkoSpec` (6/6)
      against live ArangoDB + MongoDB; slug-uniqueness + ensureBySlug idempotency both backends
- [x] `:funktor:all:jvmTest` green (full suite, no regressions) — `OrgsApiSpec` (7/7): anon 401 on
      list+create; super-user list shows ensured `system-default` (ensure-org hook verified e2e);
      create-with-branches; get by id; get unknown → 404; update name+status
- Note: API path (`findById` by `_key`) exercised on Karango only (funktor/all test app is Karango);
  Monko `findById` covered by storage spec via full `_id`

## Review record (/feature-review, 2026-07-18, 3× Opus/high)

| Reviewer | Verdict | Confirmed findings (after adversarial verification) |
|---|---|---|
| 1. Implementation & code style | PASS | Style spotless. Fixed: duplicate-slug 500→409; `list` simplified to `.map`; added `findById(_key)` test. Monko `_key` path proven equivalent + now tested. |
| 2. Domain expert | PASS w/ deferrals | HIGH (branch-id integrity): mitigated now (non-blank + in-org uniqueness validation); server-minting + update-immutability deferred to O2 (where memberships reference branch ids). Slug normalization added. |
| 3. Security | PASS | Authz gated + fail-closed on all 4 endpoints (verified); IDOR/injection on `{id}` verified safe (bind params + key-strip). Fixed: slug normalize + non-blank; added negative-auth tests (get/update anon, non-super). |

**Fixes applied (all green — `:funktor:saas:jvmTest` + full `:funktor:all:jvmTest`):**
- Org slug normalized (trim+lowercase) + non-blank at create API and `ensureBySlug`/`ensureOrganisation`.
- Duplicate slug on create → `409 Conflict` (was raw 500 leaking DB internals).
- Branch validation: non-blank id/slug + in-org unique ids → `400`.
- `list` handler simplified; storage `findById(_key)` locked on both backends.
- Tests: `OrgsApiSpec` 7→12 (non-super 401, dup→409, blank→400, anon get/update 401).

**Deferred (recorded, not open blockers):**
- Server-mint + update-immutability for branch ids → **O2 prerequisite** (see plan).
- Archived default-org self-heal (single-tenant lockout) → **O3** login active-org filter.
- Pre-auth body materialization / body-size cap → framework-wide (`funktor/rest`), not O1-specific.

**Red-team follow-up:** `20260718-redteam-saas-orgs.md` (created).

**Gate verdict: PASS** — no open CRITICAL/HIGH exploitable now; e2e tests present (both backends) & green.
