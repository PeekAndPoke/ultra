# Open security questions — route ownership / isolation (follow-ups)

**Status:** OPEN QUESTIONS — collected 2026-07-23, not yet designed/scheduled.
**Type:** security / design
**Context:** spun out of part-3 (`20260722-two-phase-auth-consistent-params.md` +
`20260722-route-check-followups.md`). The auth-hardening folder is the security-tasks home.

## 1. "Owner must be in the URI" — generalize the org rule to a structural rule (user, 2026-07-23)

**Rule:** when a route resolves an OWNED entity from the URI (`OrgAware` today, `UserAware` later),
the URI MUST also contain the owner, AND the params must bind it. So:

- `GET /orders/{order}` where `Order : OrgAware` → **rejected at boot** (no owner in the path).
- `GET /user/{user}/orders/{order}` (or `/orgs/{org}/orders/{order}`) → **allowed**.

Formalizes the pattern `/{ownerType}/{owner}/{resource}/{id}` and UNIFIES org + user ownership under
one check instead of an org-specific interface rule.

- **Boot:** the URI pattern must contain the owner segment `{org}`/`{user}` AND the params implement
  the matching `OrgAwareParam` / `UserAwareParam` (which resolves that segment). Today this is
  *emergent* (an `OrgAwareParam.org: Stored<Organisation>` is a non-optional param, so
  `validateUriPattern` already forces `{org}` into the uri) — this task makes it an EXPLICIT,
  generalized boot check (a `RouteBootCheck`), greppable and uniform across owner types.
- **Request:** the existing `OrgIsolationGuard` (and a future `UserIsolationGuard`) already do
  caller-binding + ownership — this rule guarantees they always have the owner to bind to.

Value: one clear structural law ("owned resource ⟹ owner in path"), covering org + user + future
owners, checked at BOTH boot and request time. Pairs with the entity-`OrgAware`/`UserAware` linter
(`20260722-route-check-followups.md` item 2) and a shippable review skill.

## 2. Body-embedded id smuggling (user has a PROVEN approach — details pending)

Entity refs deserialized from a request BODY bypass the URL-param guard (org-isolation covers only
`findById`-loaded URL params; a body-carried `Stored<OrgAware>` is not checked — noted in
`20260723-redteam-org-isolation.md` scenario 6). The user has a proven approach to close this;
capture the design when shared. Placeholder — do not design speculatively.

## 3. Related open items (linked for the security view)

- **Cross-org operations over-protected** (move A→B, both ids in uri) — the single-org model has no
  vocabulary for it. Direction: a `CrossOrgParam` opt-out, operator-scoped. See
  `20260722-route-check-followups.md` item 5.
- **User-level isolation** (`UserAware`/`CallerOwnershipGuard`) — same mechanism as org isolation.
  See `20260722-route-check-followups.md` item 4.
- **Cross-realm authorization boundary** — `../saas-orgs/20260719-cross-realm-authz-and-tests.md`.
- **Red-team scenarios** — `20260723-redteam-org-isolation.md`,
  `../saas-orgs/20260718-redteam-saas-orgs.md`.
