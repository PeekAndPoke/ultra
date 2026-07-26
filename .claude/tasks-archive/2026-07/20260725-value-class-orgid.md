# `OrgId` value class + session org id → coll/key (value-class-ids Step 3)

**Status:** DONE 2026-07-25 (gate PASS — all three reviewers; confirmed findings fixed; tests green)
**Plan:** `.claude/tasks/20260724-value-class-ids-migration.md` → Step 3
**Security-critical:** YES — the org-isolation caller-binding boundary. Three producers and one
consumer must flip ATOMICALLY; miss the consumer and every org-scoped request 404s.

## User decision driving this (2026-07-25)

The project-wide rule: **Vault `_id` = `collection/key`, `_key` = bare key; anything naming another
document names it by the full `_id`.** The session org id was the ONE bare-`_key` holdout, and the
user chose to standardize it rather than keep the exception — *"better for future reviews to
standardize across the board, so we do not need to wonder why sometimes only key and sometimes id."*

**Scope boundary, also the user's decision:** ID-HOLDING FIELDS go coll/key; **URL segments keep the
bare `_key`**, projected via `OrgId.key`. Rationale: a URL segment is not an id field — it is a lookup
token whose collection comes from the route's parameter TYPE (and funktor's outgoing param converter
renders EVERY entity as `_key`, so this is a framework-wide convention, not an org quirk). Putting
coll/key in the path would mean `%2F` inside a path segment — routinely normalized or rejected by
nginx/ALB/CDN — plus a UI showing "organisation/acme". Rejected on those grounds.

## Spec

- [x] `OrgId` in `ultra/security` commonMain, `init` REQUIRES `collection/key` (exactly one `/`,
      non-empty both sides), `+ key` / `collection` projections, `of()`, `parseOrNull()`
- [x] `UserPermissions.org: OrgId?` + `accessibleOrgs: Set<OrgId>` (+ `hasOrganisation`,
      `hasAnyOrganisation`, `canAccessOrg`) — `branches` NOT affected (embedded sub-doc ids)
- [x] `OrgMembership.orgId: OrgId`, `SelectedOrg.orgId: OrgId`, `buildOrgPermissions`
- [x] `AuthOrgRef.id: OrgId`, `AuthSelectOrgRequest.orgId: OrgId`
- [x] `AuthSystem.selectOrg` / `AuthRealm.selectOrg` / `resolveSelectedOrg` / `refreshToken(currentOrgId)`
- [x] `AuthRule.forOrganisation` / `forAnyOrganisation` + the builders
- [x] **`OrgIsolationGuard`: `hasOrganisation(orgParam.org._key)` → `._id`** ← THE 404 site
- [x] **`OrgMemberships.sessionMembershipsOf`: `orgId = member.org._key` → `._id`** ← producer 1
- [x] **`saas_org_hooks`: `AuthOrgRef(id = stored._key)` → `._id`** ← producer 2 (+ the two joins)
- [x] JWT `org` / `accessibleOrgs` claims (JVM builder+extractor, and the JS `AuthState` decoder)
- [x] b2b/b2b2c app pages: use `OrgId.key` where the value feeds a URL
- [x] `OrgModel.id` stays a bare `_key` String — it addresses the org IN A URL (ops-app routes +
      `OrgsApi.OrgParam`). Documented in its KDoc so the asymmetry is explicit, not a surprise.
- [x] Contract docs rewritten: `UserPermissions.org`, `OrgIsolationGuard`'s "NOTE on key spaces"
      (whose entire premise disappears — both sides become `_id`), `OrgMemberships`, `OrgIsolation`

## Not a DB migration, but IS a wire/contract migration

`UserPermissions` / `OrgMembership` / `SelectedOrg` are `@Serializable` VALUE OBJECTS, not `@Vault`
entities — nothing stores the bare org key on disk. The persisted org reference is already coll/key
(`OrgMember.org: Ref<Organisation>`). So: no data rewrite. BUT in-flight JWTs carry the OLD bare key
in their `org`/`accessibleOrgs` claims and will fail caller-binding until re-minted → **existing
sessions must re-login.** Acceptable: this repo carries no back-compat requirement.

## Test evidence

- [x] `OrgIdSpec` — rejects a bare `_key` / multi-segment / blank / control chars; `key` and
      `collection` projections; equality by value; slumber + kotlinx round trip
- [x] `OrgIsolationSpec` + `OrgIsolationE2eSpec` updated to the `_id` basis and **verified still red
      when the guard is wrong**: mutating `OrgIsolationGuard` back to `._key` is caught by 7 tests,
      and it fails LOUDLY (`OrgId("acme")` throws) rather than silently 404ing.
      CORRECTION to an earlier claim: `B2bMembersApiTest` DOES catch a producer/consumer mismatch too
      — it mints a REAL token through `signInRoute` (so `sessionMembershipsOf` →
      `buildVettedOrgPermissions` → `encodePermissions` all run) and then asserts 200 on a route whose
      verdict comes solely from `OrgIsolationGuard`. Do not weaken that test believing it proves
      nothing.
- [x] `B2bAuthFlowTest` select-org negative case must use `initech._id` or it passes for the wrong reason
- [x] Full both-backend e2e green

## Review record (filled by /feature-review 2026-07-25)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | PASS after fixes | 5 MEDIUM, 5 LOW. Found NO scripted-edit damage: no misplaced/duplicate imports, no regex over-reach, no untyped `shouldBe` comparing `OrgId` to a `String`. Confirmed the exactly-one-`/` invariant is coherent with vault's own parsing (`ensureKey` = `split('/')[1]`, `Storable.collection` = `split("/").first()` — the two DISAGREE for a multi-segment id, so vault has no such semantics to preserve). |
| 2. Domain expert | PASS | "Atomic flip is COMPLETE… a value class with no `String` overload makes a partial flip unrepresentable." Both cross-space joins now mismatch-proof; nothing on disk moved; `branches`/`branchIds` correctly untouched; `OrgId` over `Ref<Organisation>` justified (a `Ref` is JVM-only, suspending, and cannot live in a JWT claim). |
| 3. Security | PASS — isolation PRESERVED | "The flip is COMPLETE. No half-flip state is fail-open." Guard unbypassable (the `{org}` segment is a typed `Stored<Organisation>`, so `_id` is re-derived from the repo, never from the URL). Found the two joins got STRICTER: under the old bare-`_key` basis two orgs with the same key in DIFFERENT collections compared equal, which could merge two tenants' roles — now unreachable. |

### Fixes applied

1. **`OrgIsolationGuard` no longer constructs `OrgId` raw at the tenant boundary** (MEDIUM, all three
   reviewers). `OrgId(orgParam.org._id)` could THROW inside the security check, contradicting the
   guard's own contract ("any failure → `DenyAsNotFound`, hidden") — a 500 instead of a 404, plus a
   probe distinguishing "malformed org" from "not your org". Now
   `OrgId.parseOrNull(...) ?: return DenyAsNotFound`.
   *Reviewer 3 argued for keeping the throw as a regression tripwire, with a log line.* Resolved
   against that: the tripwire belongs in CI (mutation-verified — 7 tests catch a revert to `._key`),
   and a `Log` ctor dependency would make this guard per-kontainer and destroy its `entityRefFields`
   cache (the kontainer SemiDynamic-singleton trap). Reasoning recorded in the code comment; the
   "no operator signal for a corrupted `_id`" residue is red-team item §6.
2. **`AuthRealm.refreshToken` now refuses to mint an org-less session on an `OrgPolicy.Required` realm**
   (MEDIUM, reviewers 1+3, INTRODUCED). A legacy token's unparseable `org` claim degraded to `null`,
   which took the org-less branch and minted a valid 1h token where every org-scoped route 404s —
   refreshable indefinitely, never self-healing, and silent. Previously a non-null `org` String always
   reached `resolveSelectedOrg`, which threw `noOrganisationAccess` → forced re-login. `parseOrNull`
   had turned a hard reject into a quiet degradation; now it throws again. (Privilege DOWNGRADE, never
   an isolation break — but it violated the realm's own invariant.)
3. **The legacy bare-`_key` claim is now tested at the extract boundary** (MEDIUM, reviewer 1) — the
   case EVERY in-flight pre-migration JWT hits, previously covered only by a `parseOrNull` unit test.
   `ExtractUserSpec` now pins: `org` → null, unparseable `accessibleOrgs` entries dropped,
   `hasOrganisation`/`canAccessOrg` → false. Mirrors what the `UserId` step did for identity.
4. **An end-to-end 400 test through the real `/select-org` route** (LOW, reviewer 3) — raw body
   `{"orgId":"acme"}`, the exact shape a pre-migration client or attacker sends. Verified it genuinely
   ran (kotest ignores `--tests`, so checked the result XML).
5. **The unknown-org negative test named a collection that does not exist** (LOW, reviewer 1) — the orgs
   collection is `saas_organisations`, so `OrgId("organisation/does-not-exist")` could never reach the
   "org row absent" branch and planted a wrong literal to copy. Now derives the collection from a real org.
6. **Both plan docs asserted the INVERSE invariant** (MEDIUM, reviewers 1+2) — the migration plan AND the
   audit doc still said `init { require(!value.contains("/")) }` / "the CONTRACT is the bare `_key`".
   A stale plan on a security boundary is how an inversion gets re-inverted. Both now carry explicit
   superseded-warnings.
7. **`OrgModel.id` KDoc** (MEDIUM, reviewers 1+2) — the spec's one doc deliverable, and the single place
   a reader would still wonder "why a key here and an id there". States the reason and warns against
   "standardizing" it.
8. **One shared forbidden-character predicate** (LOW, reviewers 1+2) — `OrgId` had a hand-rolled copy
   missing ` `/` `, which `UserId` bans with an explicit rationale. Hoisted to
   `user/id_chars.kt` so `Email`/`Slug` in Steps 4–5 cannot ship a weaker rule.
9. Docs-site snippets showing `forOrganisation("x")` (which no longer compiles) fixed in the llms
   template and the astro page; two unused `OrgId` imports removed.

### Deferred / flagged (not fixed here)

- **DEAD SURFACE — REMOVED 2026-07-25 on the user's call.** `AuthRule.forOrganisation` /
  `forAnyOrganisation` and both builder pairs (`AuthRuleBuilder`, `FloorAuthRuleBuilder`) had ZERO
  production callers — only `AuthPhaseSpec`. Deleted, along with the `AuthPhaseSpec` assertion and the
  docs-site snippet lines. `UserPermissions.hasOrganisation` STAYS — it is what `OrgIsolationGuard`
  uses for caller-binding.

- **NEWLY unreachable as a consequence — FLAGGED, not removed:**
  `UserPermissions.hasAnyOrganisation` (both overloads' single caller was `AuthRule.forAnyOrganisation`)
  and `UserPermissions.canAccessOrg` (already had no production caller before this change). Both are
  still covered by `UserPermissionsSpec`, so they are "tested but unreachable". They are a different
  surface from the auth rules — public predicates on a widely-used data class — so removing them is the
  user's call, not a side effect of this task.
  Also still open from Step 2: `ownerIdsOf` (`ultra/security/.../user/OrgRole.kt`).
- Reviewer 1's reuse suggestion (`val Stored<Organisation>.orgId` to collapse ~10 `OrgId(x._id)` sites)
  — skipped: most sites are tests, and the guard now uses `parseOrNull`, so the extension would cover
  less than it appears. Revisit if it spreads.
- Kotlin prohibits a `vararg` of a value class, so `hasAnyOrganisation(vararg)` is gone. No production
  caller existed; documented in place.

**Red-team follow-up**: `.claude/tasks/20260725-redteam-value-class-orgid.md` (8 scenarios, COLLECTED
not executed).
