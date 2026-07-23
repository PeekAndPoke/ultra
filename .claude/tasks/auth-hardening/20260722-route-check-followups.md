# Route-check extension-point follow-ups (param-names, entity linter, branch isolation)

**Status:** TODO (created 2026-07-22) — spun out of `20260722-two-phase-auth-consistent-params.md`
**Type:** framework / hardening
**Depends on:** the `RouteBootCheck` / `RouteParamsGuard` extension points landed in part 3.

Part 3 introduced two pluggable extension points and moved the boot-resident route checks onto them:

- `RouteBootCheck` (funktor/rest) — run at app-start by `ValidateRoutesOnAppStarting`.
- `RouteParamsGuard` (funktor/rest) — run in phase 2 per request by the dispatch.

These three items were deliberately deferred so part 3 stayed reviewable.

## 1. Migrate `validateUriPattern` onto `RouteBootCheck`

`TypedRoute.validateUriPattern` (funktor/core `TypedRoute.kt`) checks that every non-optional route
param appears in the URI pattern — but it runs in the `TypedRoute` **init (construction)**, not at
boot. Move it to a `UriParamsBootCheck : RouteBootCheck` so ALL route validation converges on the
one runner (the user's "push all the other checks into the same structure").
NOTE: this is a behavior shift (construction-time failure → boot-time failure) — verify nothing
relies on the earlier failure point, and keep the message actionable.

## 2. Entity `OrgAware` linter

Marking a domain entity `OrgAware` (funktor/saas) is the developer's opt-in; the boot check only
forces the PARAM side once an org-owned entity is actually resolved. A dev who forgets to mark an
org-owned entity `OrgAware` gets no signal. Add a lint/boot rule that flags entity types that look
org-owned (e.g. have an `org`/`orgId` field, or a `Ref<Organisation>`) but do not implement
`OrgAware`. Likely a detekt rule and/or a startup scan over the vault repositories.

## 3. Branch-level (sub-org) isolation (user request, 2026-07-22)

Extend the saas isolation from organisations to **branches** (the sub-tenants embedded in an
`Organisation.branches`). The mechanics are near-identical to org isolation:

- `OrgBranchAware` on entities (the entity's owning branch) + `BranchScopedParam` on route params
  (the addressed branch), mirroring `OrgAware` / `OrgAwareParam`.
- A `BranchIsolationBootCheck : RouteBootCheck` (branch-owned entity ⟹ params must declare the
  branch) and a `BranchIsolationGuard : RouteParamsGuard` (caller-binding to the caller's accessible
  branch(es) via `UserPermissions.hasBranch` / `hasAnyBranch`, + branch-consistency via
  `hasSameIdAs` on the branch reference).
- Compose with org isolation: a branch belongs to an org, so a branch-scoped route is typically also
  org-scoped; decide whether `BranchScopedParam` implies `OrgAwareParam` or they stack.
- Same 404-hidden failure semantics, both DB backends in the e2e.

## 4. User-level isolation — same mechanism as org isolation (user idea, 2026-07-23)

Extend the `RouteParamsGuard`/`RouteBootCheck` mechanism from ORGS to per-USER-owned resources
(orders, payments, profile, …). Near-identical shape — this is the payoff of the pluggable design:
- `CallerOwned` on entities (the owning user id) + `CallerOwnedParam` on route params (or infer the
  owner directly from the resolved entity — no url param needed since the owner is the caller).
- A `CallerOwnershipGuard : RouteParamsGuard` binding `entity.ownerId == caller.userId` (super-user
  policy TBD), and a boot check forcing coverage where a `CallerOwned` entity is resolved.
- Compose with org isolation (an order belongs to a user AND an org).
- **Deferred: think it through after this workstream** (user's call). Likely its own task.

## 5. Round-2 security hardening notes (from the part-3 gate)

Documented inline in the code KDoc; tracked here for hardening + red-team follow-up:

- **Fail-open without the saas module (MEDIUM).** `OrgIsolationBootCheck` + `OrgIsolationGuard` are
  registered ONLY by `Funktor_Saas`. An app that marks entities `OrgAware` but omits the saas module
  gets no isolation AND no boot warning. Documented on `OrgAware` KDoc. Consider a REST-core boot
  assertion that fails start when a marker-typed param has no covering guard. Red-team: boot an app
  with an `OrgAware` route but no saas module; expect (today) silent no-protection.
- **Polymorphic/base-typed params (MEDIUM, low likelihood).** Boot-forcing detects org-ownership by
  the DECLARED `Stored<X>` type arg, so `Stored<Base>` (Base not `OrgAware`, concrete rows are) is
  not boot-forced. The guard now checks the RUNTIME value, so it catches this IF the params are
  `OrgAwareParam`; a base-typed param that also omits `OrgAwareParam` is unguarded. The entity-linter
  (item 2) is the backstop. Red-team: a base-typed entity route param.
- **Body-embedded entity refs (INFO).** Org-isolation covers only URL-resolved `Stored`/`Storable`
  params (converter `findById`); a `Stored<OrgAware>` deserialized from a request BODY is not
  guarded (and not converter-loaded, so no pre-auth oracle). Red-team: body-trust IDOR.

## Acceptance

- Each item is its own reviewed change (they are independent). Reference this doc from the part-3
  index when scheduling.
