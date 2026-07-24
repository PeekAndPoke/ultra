# Docs follow-up: org membership model (OrgMember collection + getMemberships seam)

**Status:** TODO — COLLECTOR for a later documentation pass (capture what changed + anchors; do not
write prose here). Created 2026-07-23 per the "framework change → doc task" standing rule.
**Type:** documentation
**Triggering change:** the saas org-membership feature (`.claude/tasks/saas-orgs/20260723-org-membership-management.md`).

## What changed (framework surface — document when the feature stabilizes)

- **`OrgMember` collection** (`funktor/saas`): the first-class org↔user membership entity, unique
  `(org, userId)`, `OrgAware`; `OrgMembersStorage` (Null + Vault + Karango/Monko) via
  `funktorSaas { useKarango()/useMonko() }`. `userId` = the realm-qualified user `_id`.
- **`AuthRealm.getMemberships()` is now the membership seam.** Default is `emptySet()` (org-less
  realms need no override); org realms override it to source memberships from the collection via
  `OrgMembersStorage.sessionMembershipsOf(user._id)`. The session model (`OrgMembership` value
  object → `buildOrgPermissions` → JWT) is unchanged.
- **`HasOrgMemberships` was REMOVED.** Memberships are no longer embedded on the user record; the
  `AuthUser` interface stays data-only. Migration note for consumers: implement `getMemberships` on
  the realm (backed by the `OrgMember` collection or any source) instead of embedding a `memberships`
  field.
- **`OrgRole`** (`ultra/security`): reserved structural roles (`OWNER`/`ADMIN`) + role-set predicates
  (`Set<String>.isOrgOwner/isOrgAdmin/canManageOrgMembers`) + invariant helpers
  (`ownerIdsOf`/`wouldRemoveLastOwner`). Gate authorization via the AuthRule DSL (super-user bypass),
  not the raw predicate.
- **Reusable soft-delete filter** (framework addition): `karango/core/.../aql/softdelete.kt` and
  `monko/core/.../lang/dsl/softdelete.kt` add `notDeleted(entity.softDelete)` for `SoftDeletable`
  entities — vault soft-delete is a model-level convention with NO repo auto-filtering, so reads
  must exclude deleted rows explicitly. `OrgMember` uses it; `remove` soft-deletes (audit + recover).
- **Member-management API pattern** (demo b2b, `B2bMembersApi`): the reference shape for tenant
  member management — URL-org `OrgAwareParam` + `OrgAware` member param (isolation for free),
  owner-only ownership (admins manage members, owners manage ownership), atomic last-owner via a
  per-org lock, `(org,userId)`-immutable role edits, soft-delete removal. To be extracted to
  `funktor/saas` when the operator surface needs it.

## Anchors
- `funktor/saas/.../domain/OrgMember.kt`, `.../storage/OrgMembersStorage.kt`, `.../OrgMemberships.kt`
- `funktor/auth/.../AuthRealm.kt` (`getMemberships`), `funktor/auth/.../model/AuthUser.kt`
- `ultra/security/.../user/OrgRole.kt`, `OrgMembership.kt`

## Do NOT
- Do not write prose here — this is the collector; the doc pass writes the pages.
- Docs live in `docs-site/src/pages/ultra/*` + the LLM mirror templates `docs-site/src/data/llms/*.md`
  (never edit `docs-site/public/`).
