# BackNavigationTrap fires on in-page fragment navigation

**Status:** TODO
**Plan:** — follow-up of `.claude/tasks-archive/2026-09/20260928-router-click-interception.md` (review round 1, MAJOR)
**Security-critical:** no

## Spec

Since the path router leaves `href="#section"` to the browser, a fragment navigation pushes a history
entry and fires `popstate` with `state == null`. An active `BackNavigationTrap`
(`kraft/core/src/jsMain/kotlin/routing/BackNavigationTrap.kt:56`) treats that as "navigating away":
`Continue` silently deactivates the trap, `Stop` pushes a new entry.

Before the router change the same click pushed `/#section` and left the page for the root route
without the trap noticing, so this is not a regression — but it is wrong.

Hard part: the trap's own entry has the same URL as the page, so URL comparison alone cannot tell
"back out of the trap" from "fragment navigation". The hash at activation vs now may be enough.

- [ ] In-page anchor click with an active trap does not invoke the trap's block
- [ ] Back button out of the trap still does
- [ ] Browser test in `kraft/core-tests` (real fragment navigation is safe in Karma)

## Implementation notes

## Test evidence

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:**
