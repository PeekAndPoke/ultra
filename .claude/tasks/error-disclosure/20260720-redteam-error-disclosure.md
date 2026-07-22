# Red-team: error disclosure, environment gating, route auth

**Status:** COLLECTED — do not execute during normal feature work
**Origin:** `.claude/tasks/20260720-error-response-disclosure-audit.md`
**Covers:** `20260720-env-classification-allowlist`, `20260720-error-messages-generic-in-production`,
`20260720-insights-gui-auth-gate`, `20260720-fixtures-env-gate`,
`20260720-route-auth-default-deny`, `20260720-auth-error-account-enumeration`

Scenarios to attempt in a dedicated penetration-test session, against a disposable environment.
Run these **after** the linked remediation tasks land, to confirm the fixes hold.

## A. Forcing internal detail into an error body

1. As an anonymous caller, provoke a `KarangoQueryException` on a public endpoint (sign-in does a
   user lookup). Try: oversized strings, keys with `/`, unicode edge cases, values that break AQL
   bind serialisation, and enough concurrency to trigger cursor/timeout errors. Confirm the response
   body contains no AQL, no collection names and no bind-variable names.
2. Same against org-scoped endpoints as an authenticated user of a *different* org.
3. Provoke non-DB exceptions and check their messages: force a `ConnectException` (point a
   dependency at a dead host), a Slumber awaker failure (malformed JSON body against a typed route),
   and a kontainer resolution error. Confirm none of host:port, file paths, service FQCNs or JSON
   field paths reach the client.
4. Confirm the correlation id in the response is opaque and cannot be used to fetch the full error.

## B. Environment misclassification

5. Deploy with `ktor.deployment.environment` set to each of: `staging`, `prd`, `production-eu`,
   `live-de`, `PROD ` (trailing space), `""`, and a literal `${ENV}`. For each, confirm error
   responses expose no stack traces, no failed-auth-rule descriptions, and that fixtures are not
   installed.
6. Confirm that startup **fails loudly** on an unrecognised environment (if that option was chosen)
   rather than silently picking a permissive default.
7. Attempt to influence the environment value from outside the deployment (env var injection,
   config file precedence, system properties) and see whether a lower-privilege path can flip the
   app into development mode.

## C. Insights GUI

8. With insights enabled, fetch `/_/insights/bar/{bucket}/{file}` and
   `/_/insights/details/{bucket}/{file}` anonymously, then as an authenticated non-superuser.
   Confirm both return 404.
9. Harvest `detailsUri` / `detailsUrl` from ordinary API responses as an anonymous caller and try
   them. Confirm they are absent for unauthorised callers and unusable if guessed.
10. Enumerate bucket/file names — check whether they are predictable (timestamps, counters) and
    whether another user's captured request can be reached by iterating.
11. Confirm insights cannot be enabled at all in a production environment.

## D. Route auth coverage

12. Enumerate every mounted route in the demo app and probe each anonymously; list any that respond
    without an explicit `public()` declaration.
13. Add a route with no `.authorize { }` to a scratch app and confirm startup fails rather than
    publishing it.
14. Probe for routes reachable via path variations that bypass the rule check (trailing slash,
    case, encoded separators, HTTP method not declared).

## E. Account enumeration and auth oracles

15. Sign up with a known-existing email vs a fresh one; diff status, body and **timing**. Repeat
    across several hundred attempts to expose statistical timing differences.
16. Sign in with an unknown user vs a known user with a wrong password; diff the same three signals.
17. Exercise password reset / recovery and any org-invite flow for the same oracle.
18. Check whether rate limiting exists on these routes, and whether it is per-IP, per-account, or
    absent.

## F. Regression guards

19. Re-run A1 and B5 against the *pre-fix* commit to confirm each scenario genuinely reproduced —
    a red-team scenario that never reproduced proves nothing about the fix.
