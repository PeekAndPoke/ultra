# Red team — insights REST API and collector data

**Status:** COLLECTED — not executed. Sweep in a dedicated penetration-test session.
**Feature:** `.claude/tasks-archive/2026-07/20260730-insights-rest-api.md`

Scenarios to attempt against the superuser-gated insights API and the data it exposes. Per the project
rules these are collected during feature work and executed separately, never as part of it.

## Auth gate

1. Reach `GET /insights` and `GET /insights/{bucket}/{file}` **anonymously** — expect denial, and check
   the denial does not differ between "no such record" and "not allowed" in a way that confirms a record
   exists.
2. Reach them as an **authenticated non-superuser**. Then as a superuser **minted by a different realm** —
   `AuthRule.isSuperUser()` is permission-based and realm-agnostic by design (its own KDoc warns:
   "an `isSuperUser=true` token minted by ANY realm passes an isSuperUser gate"). Confirm whether insights
   should be realm-scoped as well.
3. Expired / revoked / malformed JWT. Note that funktor's auth chain is **fail-soft** — an invalid
   credential falls through to `AnonymousCaller` rather than 401
   (`funktor/rest/src/jvmMain/kotlin/auth/call.kt` KDoc). Confirm the gate denies rather than treating a
   broken token as some intermediate state.
4. API-key caller instead of JWT — same three cases.

## Credential capture in stored records

The pre-fix state (see the feature task) stored `Cookie` and `Set-Cookie` verbatim and truncated only
`Authorization`. After the redaction fix:

5. Send credentials in a header **not** on the denylist (`X-My-App-Token`, `X-Amz-Security-Token`,
   `Authentication` — note the near-miss spelling) and confirm what lands in the stored record.
6. Send `AUTHORIZATION` / `Authorization` / mixed case — confirm the match is case-insensitive.
7. Send a credential as a **query parameter** (`?token=…`) — `queryParams` is captured with no redaction
   at all, and the fix as designed only covers headers. This is a known gap, deliberately listed here.
8. Send a credential in a **request body** — check whether any collector captures bodies.
9. Confirm the redaction marker cannot be confused with a real value that happens to equal the marker.
10. Trigger the old crash path: `Authorization: Bearer abc` (< 20 chars) — confirm it no longer throws,
    and that a throwing collector cannot break the response it is observing.

## Stored-record handling

11. `InsightsDataLoader` resolved collector classes with `Class.forName(it.cls)` **before** the subtype
    check, so a crafted record could name any class on the classpath and run its static initializer.
    After the registry fix: write a record naming an arbitrary class and confirm nothing loads.
12. Write a record with a collector key that does not exist — confirm the open envelope degrades rather
    than failing the whole response.
13. Depot path handling: `..`, URL-encoded `%2e%2e`, absolute paths, symlinks inside the depot dir.
    **Note:** `..` and absolute paths were checked on 2026-07-30 and are handled
    (`FileSystemRepository.validateName`, `File(root, path)` re-rooting) — symlinks were NOT checked.
14. Very large or malformed stored records — a truncated JSON file, a record with a huge collector list.

## Self-observation and disclosure

15. Confirm calling the insights API does not produce an insights record of itself (recursion), and that
    the API's own `Authorization` header does not end up in a record a later call can read.
16. Check what the list endpoint discloses to a superuser of one tenant about another tenant's requests —
    insights is global, not org-scoped.

## Added by the review gate (2026-07-31)

18. **Query strings are stored unredacted** (`RequestCollector.kt:41,43`) — both in `queryParams` and
    inside `uri`, which the summary rebuilds into `url`. Try `?token=`, `?code=`, `?api_key=`,
    `?X-Amz-Signature=`. Scenario 7 above predicted this; the review confirmed it end to end.
19. **`referer` is not redacted** — it carries the previous URL, which is where magic-link and OAuth
    query strings live. A different door to the same leak as 18.
20. **`cookie2` / `set-cookie2`** — `cookie` is protected by exact match only and the sensitive-name
    regex has no `cookie` alternative, so cookie-name variants fail open.
21. **Defeat `isExcluded` two ways** (`InsightsFull.kt:44`): percent-encode a path character
    (`/_/funktor/%69nsights/records` routes but is not excluded, because ktor's router decodes while
    `request.uri` does not), and put the base in a query string
    (`/api/orders?next=/_/funktor/insights` suppresses that request's own record). The second is
    audit evasion available to any caller.
22. **Depot symlinks** (`FileSystemRepository.kt:76,115,139`) — `root` is `.absoluteFile`, not
    `.canonicalFile`, and reads do no containment re-check. Plant
    `records-<date>/x.json -> /proc/self/environ` or the app's `application.conf` and open it as a
    superuser. This is scenario 13's untested half, now with a concrete mechanism.
23. **Stored strings are served raw to a future viewer** (`InsightsModels.kt:58`) — an unauthenticated
    attacker sets `User-Agent: <img src=x onerror=…>`; it is stored verbatim and returned as JSON.
    The deleted kotlinx.html GUI escaped; the Vue replacement must too. Re-run this once a viewer
    exists.

## Known, accepted, time-boxed

17. The kotlinx.html GUI routes stay reachable **without any credential** until the Vue rewrite removes
    them (maintainer decision, 2026-07-30). Not a finding for this sweep unless it outlives the rewrite —
    but confirm the records it serves are the redacted ones after the fix above.
