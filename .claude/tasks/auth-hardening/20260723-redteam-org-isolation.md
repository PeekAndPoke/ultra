# Red-team: org-isolation (part-3 two-phase auth + OrgAware guard)

**Status:** COLLECTED — not executed (2026-07-23). Run in a dedicated pen-test session.
**Type:** red-team (security-critical follow-up to `20260722-two-phase-auth-consistent-params.md`)
**Assume:** a hostile AUTHENTICATED user, member of org `acme`, session selected on `acme`, trying to
read/act on org `globex`.

Two review gates (round-2 verdict: idiomatic route airtight) found no break of the canonical path.
These are the residual/edge scenarios to actively attempt.

## Scenarios to attempt

1. **Classic cross-org IDOR** — `GET /orgs/{acme}/widgets/{globexWidgetId}`. Expect 404 (org-consistency:
   `entity.org hasSameIdAs param.org` fails). Confirm the 404 is byte-identical to a genuine miss.
2. **Foreign-org URL** — `GET /orgs/{globex}/widgets/{globexWidgetId}` from an acme-selected session
   (even if acme user is ALSO a globex member via `accessibleOrgs`). Expect 404 (caller-binding uses
   the SELECTED org, not `accessibleOrgs`).
3. **Existence oracle** — anonymous + authenticated-wrong-org probing of `{widgetId}` — confirm 401
   (anon) / 404 (wrong org) are identical for existing vs non-existing ids; confirm NO repository read
   for the phase-1-denied case (timing + a counting repo).
4. **Polymorphic/base-typed param** — a route param `Stored<Base>` where `Base` is not `OrgAware` but
   the concrete row is. Two sub-cases: (a) params ARE `OrgAwareParam` → guard's runtime-value check
   should still catch it; (b) params are NOT `OrgAwareParam` → boot check does NOT force it → attempt
   an unguarded cross-org read. This is the known gap the entity-linter follow-up backstops.
5. **Unmarked org-owned entity** — an entity that SHOULD be `OrgAware` but the dev forgot the
   interface → no boot force, no guard. Attempt cross-org read. (Linter follow-up territory.)
6. **Body-embedded ref** — a `Stored<OrgAware>` deserialized from the request BODY (not the URL) →
   out of guard scope. Attempt to smuggle a foreign entity via the body.
7. **Key-space confusion** — craft an `OrgAware.org` ref or `{org}` segment to make `entity.org._id`
   collide with a foreign `param.org._id` (bare `_key` vs `organisation/key`, missing collection
   prefix). Expect fail-closed, not a bypass — verify no false-allow.
8. **Guard-not-registered** — an app using `OrgAware` but omitting the saas module. Verify the org
   param can't even convert (no `OrgsRepo`) → route non-functional, not a silent leak.
9. **Super-user cross-realm** — an `isSuperUser` token minted by another realm hitting an org route
   (caller-binding short-circuits on `isSuperUser`); confirm org-consistency still blocks an
   inconsistent pair even for a super-user. (See `saas-orgs/20260719-cross-realm-authz-and-tests.md`.)
10. **Two-phase bypass** — try to trigger a pre-auth `findById` (DoS/oracle) on any route variant
    (Plain/WithParams/WithBody/WithBodyAndParams/Sse) by a floor-denied caller.

## Cross-references

- `20260722-two-phase-auth-consistent-params.md` — the feature.
- `20260722-route-check-followups.md` — the linter/hardening backstops (items 2, 4).
- `20260718-redteam-saas-orgs.md` — the broader org red-team; fold these in.
