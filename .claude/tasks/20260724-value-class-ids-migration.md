# Migration: wrap ids in `@JvmInline value class`es (RealmId pilot → the rest)

**Status:** PLANNED 2026-07-24 — awaiting go-ahead. `RealmId` is the pilot (Step 1) to surface
unforeseen complications before committing to the rest.
**Rationale / candidate survey:** `20260724-value-class-ids-audit.md`.
**Enabled by:** `20260724-slumber-value-class-support.md` (DONE — value classes serialize like kotlinx).
**User decision:** use SPECIFIC id classes (`RealmId`, `UserId`, `OrgId`, …), not a generic `Id<T>`.

## ⚠️ Prerequisite surfaced by the RealmId pilot — KSP value-class support (both backends)
Every Tier-1 id is a STORED **and QUERIED** entity field, so it needs the Karango + Monko KSP to
generate type-safe query-path accessors for value-class properties. Concretely for RealmId:
`AuthRecord.realm` is indexed (`field { realm }`) and filtered (`FILTER(r.realm EQ realm)` /
`r.realm eq realm`) in `Karango/MonkoAuthRecordsRepo` via the KSP-generated `realm` accessor. Changing
`realm: String` → `RealmId` on the entity will not compile / query until KSP handles it.

**→ HARD DEPENDENCY: `20260724-karango-ksp-value-class.md` + `20260724-monko-ksp-value-class.md` must
land FIRST, before the STORED-field part of ANY id migration.**

**DECIDED (user 2026-07-24): KSP-first.** Do BOTH KSP tasks (in lockstep — storage is both-backend),
each with full round-trip tests, THEN migrate ids end-to-end (params + entities). The KSP work is
clear in scope, purely mechanical, concretely defined and testable — a good first unit.

## Placement (user 2026-07-24: co-locate with the entity's home WHERE POSSIBLE)
Rule: put each id in the SAME module as the entity it identifies; when a lower module also needs it
(dependency direction forbids co-location), fall back to the lowest module all users can see.
- `RealmId` → **`funktor/auth` commonMain** — co-located with the realm/`AuthRecord` concept (auth owns
  realms), in commonMain so BOTH the API client (commonMain) and `AuthRecord`/`AuthSystem` (jvmMain)
  can use it. Clean co-location. ✓
- `UserId` → `funktor/auth` commonMain — auth owns the realm-qualified user id.
- `OrgId` → CANNOT co-locate with `Organisation` (funktor/saas): `UserPermissions.org` /
  `OrgMembership.orgId` are in the LOWER `ultra/security`, which can't see funktor/saas. → lives in
  `ultra/security` (the lowest common module). Document the exception.
- `Email` / `Slug` → co-locate with the owning entity (`Slug` with `Organisation` in funktor/saas;
  `Email` with the user models). Confirm each at its step.

## Step 1 — `RealmId` (PILOT)
`@JvmInline value class RealmId(val value: String)` with `init { require(value.isNotBlank()) }` (a
realm key is a non-empty registered identifier; no canonicalization). NO stdlib concerns.

Migration surface (from the survey):
- **Non-stored (safe now):** `AuthSystem.getRealm/signIn/signUp/selectOrg/…(realm)`,
  `AuthRealm`, `SessionStore.listForUser/revokeAllForUser(realm, …)`, `AuthApiClient(realm)`,
  `AuthApiFeature.RealmParam`, demo `REALM` consts (`B2bRealm.REALM` etc. → `RealmId("b2b")`).
- **Stored + queried (needs KSP):** `AuthRecord.realm` (all 6 subtypes) + the indexes/filters in
  `Karango/MonkoAuthRecordsRepo` + the KSP-generated `realm` accessor. Also `AuthRecordStorage`
  signatures (`findByToken(realm, type, token)`, `findAllByOwner(realm, type, owner)` — the swap-hazard
  functions this whole effort targets).

Test evidence:
- [ ] Existing auth e2e green on BOTH backends (`B2bAuthFlowTest`, `B2b2cAuthFlowTest`, session/reset
      flows) after the migration — the behavior must be identical.
- [ ] Both-DB round trip for `AuthRecord.realm` as `RealmId` (store → `findByToken`/`findAllByOwner`
      filter by realm → read back) once KSP lands.
- [ ] `RealmId` unit: `init` rejects blank; equals/hashCode by value.

Review: 3-agent gate (impl+style / domain-auth / security) → loop to zero → commit.

## Step 2 — `UserId`
`userId`/`ownerId: String` → `UserId` (realm-qualified `_id`, e.g. `b2b_users/x`). Surface: `AuthRecord.
ownerId` (stored+queried → KSP), `SessionStore`, `OrgMembersStorage` (`OrgMember.userId` — stored+
queried → KSP), `AuthSystem`. Vault `Stored<T>._id` stays a generic `String`; `UserId` lives at the
auth/domain layer with conversion at the vault boundary (or a KSP helper). `init { require non-blank }`.

## Step 3 — `OrgId`
`orgId: String` → `OrgId`. Surface: `UserPermissions.org`, `OrgMembership.orgId`, `AuthSelectOrgRequest.
orgId`, `AuthSystem.selectOrg`. **`init { require(!value.contains("/")) }`** — enforces the bare-`_key`
contract and REJECTS a collection-qualified `_id`, turning the documented `_key`-vs-`_id` 404 footgun
into a guarded type. NOTE: `OrgMember.org` is a `Ref<Organisation>` (an `_id`), NOT an `OrgId` — leave
it; `OrgId` is the SELECTED-org key on the session/permissions side.

## Step 4 — `Email`
`email: String` → `Email`. `init { require(value == value.trim().lowercase()) }` + `of(raw) =
Email(raw.trim().lowercase())`. `of()` normalizes; a non-canonical value is REJECTED on construction
(incl. deserialize). CONSEQUENCE: `Email` is a DOMAIN/stored type — the API takes raw `String` and
normalizes at the handler (as `20260724-email-canonicalization.md` already does), NOT direct raw-body
binding. Surface: user models/entities across realms.

## Step 5 — `Slug`
`Organisation.slug` / `Branch.slug` → `Slug`. Same pattern as Email: `init` validates
`Slugs.normalize(value) == value`; `of(raw) = Slug(Slugs.normalize(raw))`.

## Later / optional
- `BranchId` (`Branch.id`, `branchIds: Set<String>`) — org-scoped; adopt when we touch that area.
- `UserType` (`USER_TYPE` consts) — closed set; value class OR keep consts; low payoff.

## Not
- `Role` (`roles: Set<String>`) — open-ended app-defined; leave as `String` (structural ones have
  `OrgRole` consts).

## Cross-cutting caveat (applies to every step)
Type-safety is guaranteed by the wrapper; CANONICALIZATION is NOT enforced on decode by construction
alone — slumber/kotlinx call the ctor, bypassing `of()`. Put hard invariants in `init {}` (runs on the
ctor / on deserialize → REJECTS bad values); do transforming normalization in `of()` at the boundary.
(Security red-team item recorded in the slumber task + email-canonicalization task.)

## Standing rule
To be codified by the user AFTER the RealmId pilot validates the approach (per the audit doc).
