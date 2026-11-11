# ULTRA-00161: Red team — insights REST API and collector data

> **Status (2026-10-19): NOT STARTED.** COLLECTED — not executed. Sweep in a dedicated penetration-test session.
>
> **Part of:** ULTRA-00259 (Red-team sessions epic)

**Feature:** ULTRA-00160 (insights rest api)

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

## Added by the graphs gate (T2, 2026-10-19)

The two superuser graph endpoints (`GET /_/funktor/insights/graphs/kontainer`, `/graphs/database`) widen
what the `isSuperUser()` floor guards. Scenarios 1-4, 15, 16 and 21 above now also cover them.

24. **Reach a graph below superuser through path variants** — trailing slash, `//_/funktor/…`, `%2F`,
    `%69nsights`, case changes, `HEAD`, `OPTIONS`/CORS preflight; also whether a variant that routes but
    misses the `startsWith(InsightsApi.base)` check gets RECORDED.
25. **Foreign or stale superuser credentials** against both graphs: a token minted by another realm, an
    expired or revoked one, an API-key caller (cf. 2-4).
26. **Inventory the disclosure** a superuser gets: `codeLocation` file:line in every definition, parameter
    names, Arango/Mongo server versions, database names. Confirm no `instance(...)` config value, credential
    or host appears — in apps that register config objects as instances too.
27. **Hammer `graphs/database`** — each call reflects over every repository and makes one blocking round trip
    per driver on the request thread; N parallel superuser calls vs request-thread starvation (compare
    `VaultApi.listRepositories`).
28. **Database down or slow during `graphs/database`** — what the production 500 body carries
    (`cause.message` from the driver may name hosts).
29. **Insights disabled in production** — both graphs (and the record endpoints) still answer a superuser;
    see the plan's "production gate for insights" row.
30. **Make the kontainer graph build fail, then send anonymous traffic** — `lazy` does not memoize an
    exception, so every recorded request rebuilds and prints a stack trace: log flooding by unauthenticated
    callers.
31. **Concurrent first calls on a cold app** — the `lazy` builds once and every caller sees the same
    fingerprint.

### Added 2026-10-19 from T1's gate (insights-vue, round 2) — Monko query text, the label allowlist

32. **Raw AQL under the trusted `aql` label** — a repository passing hand-built text to
    `driver.query(AqlTypedQuery.of(type, "… == \"$token\" …"))` (`AqlTypedQuery.kt:15`, labelled at
    `KarangoDriver.kt:101`): does the token reach the depot and the Database tab? Fix tracked in
    ULTRA-00226 (insights vue round2 minors) item 6.
33. **A secret as a document KEY at a value position** — `Filters.eq("grants", Document(apiKey, true))`: the
    placeholder-iser keeps keys (`MonkoDriver.kt:73-79`). Item 7 of the same follow-up.
34. **A failed Mongo query whose server error echoes literal filter values**, logged during the request —
    `LogCollector` (`LogCollector.kt:32-35`) records the message verbatim (reasoned, not run).
35. **`enrichApiResponseWithInsights` on API responses** (`funktor/rest/src/jvmMain/kotlin/respond.kt:71-84`)
    — server hostname and full URL in responses, the 401 sent to a non-superuser included
    (`vue-verify/fixtures/refusal-graph.json:19-28`). Unverified, outside T1's change.

### Added 2026-10-19 from the insights-minors review (security)

33a. **Addendum to 33:** a `$`-prefixed secret (a bcrypt/argon2 hash) as a key at a value position, in a query the
    server REJECTS — since the print moved before execution, the failed query's text holds the key
    (`MonkoDriver.kt:110-111`; fix: ULTRA-00228 (insights minors followups) item 1).
36. **Raw AQL that fails** — `KarangoQueryException`'s message embeds the full `query.query`
    (`karango/core/src/main/kotlin/vault/KarangoDriver.kt:130-137`); for `aql-raw` that includes the inlined token. If
    the app logs it (`LogCollector.kt:35` records verbatim) or a 500 body carries it, the record holds what the vault
    slice withholds.
37. **Secrets as field names** — `Filters.exists("sessions.$token")`, query-form `elemMatch("grants", Document(apiKey,
    true))`, Karango `property<…>(token)` / `aqlFunc(name)` / `AqlNameExpr(name)`, a custom `AqlExpression` using the
    public `AqlPrinter.append(String)`. Both printers keep names by design under a trusted label — inventory real apps
    for data-keyed maps.

38. **(ULTRA-00225) An enabled config that reaches only the kontainer, in production** — inject
    `InsightsConfig(enabled = true)` into a mount's request kontainer without `instrumentWithInsights`, environment
    `prod`: does anything record, and do all four endpoints answer 404 to a superuser? Old depot files present.
39. **(ULTRA-00225) Environment names at the edge** — `qa-prod-mirror`, `qatar`, `QA`, `dev ` (trailing space), an
    unresolved `${ENV}`, a homoglyph `dеv`, an unset key (default `prod`): which ones run insights?
40. **(ULTRA-00225) Secrets in system property values** — `-D` overrides carrying a connection string, a JDBC URL with
    `password=`, a Sentry DSN (`https://<key>@host`, no password: NOT caught by the value rule), a bearer token in a
    property whose name says `header`: what reaches the `runtime` slice?

41. **(ULTRA-00228) A hash-keyed map passed as a QUERY** — `find { filter(Document(hashKeyedMap)) }`,
    `elemMatch("grants", Document(hash, 1))`, a `$or` of such documents: does any `$2b$`/`$argon2` key reach the
    `mongo-bound` text, and what does the server's rejection put into the log slice when the exception escapes?

42. **(ULTRA-00171) Query values are recorded; credential repositories withhold them** — find a query path from the
    auth records or sent messages that records a value anyway: a repository built on the shared driver for the same
    collection, a `withProfiler()` copy, a driver call that profiles outside the repository, a custom `Entry`, an
    app's API-key or invitation store without `withholdingQueryValues()`. And a value that breaks the record:
    NaN, a self-referencing map, a 10 MB document.

43. **(ULTRA-00301) One switch for the app** — find a way to get insights active in `prod`, `staging` or an unknown
    environment: the switch in `application.common.conf`, an env-var substitution, a blueprint that sets it in code,
    an app that never instruments a route. And on a dev/qa box with the switch on: which hosts of the app answer the
    insights API (every host routing `InsightsApiFeature`), and does any of them face the internet?

### Added 2026-11-11 from ULTRA-00198 (the Kraft insights GUI)

44. **Stored payloads in the Kraft pages** — a record whose path, headers, query values, body, AQL text or log
    lines carry `</code><img src=x onerror=...>`, `{{...}}`, a `language-*` class or a vis-network label with HTML.
    Check every place the Kraft pages render them: plain text, Prism-highlighted blocks (Prism builds HTML from
    tokens; manual mode since ULTRA-00198), the vis-network graphs (node `title` as a string is text in
    vis-network 9+; confirm the version and that no element title is built from record data).
45. **The record path in the URL** — `/insights/details/<bucket>/<file>` with `..`, `%2e%2e`, an absolute path, a
    backslash, a non-`.json` file, a folder: must be 404, never 500, never a file outside `records-<date>/`
    (fix: ULTRA-00304, which also covers the depot paths).
46. **Redaction in the GUI** — values the server marked `***redacted***` (`Redacted.PLACEHOLDER`): confirm no tab,
    raw-JSON view or copy button shows the original, i.e. the server never sends it. Query values are shown again
    (maintainer, 2026-11-09): confirm a `?token=` value is redacted before it reaches the record.
47. **Cached graphs across users** — `InsightsGraphSource` caches graphs in the browser tab and forgets them only when
    the list page opens. Log out and in as a non-superuser in the same tab, deep-link a record page: is a graph
    of the earlier session shown? Are the Kraft routes guarded, or only the API?
48. **A host app's own Prism** — an app with a global Prism in auto mode highlights every `language-*` block on the
    page, including record data; see 44.

## Known, accepted, time-boxed

17. The kotlinx.html GUI routes stay reachable **without any credential** until the Vue rewrite removes
    them (maintainer decision, 2026-07-30). Not a finding for this sweep unless it outlives the rewrite —
    but confirm the records it serves are the redacted ones after the fix above.

## History

- 2026-10-21: numbered ULTRA-00161 (was `docs/tasks/20260730-redteam-insights-api.md`).
- 2026-11-11: items 44-48 added from ULTRA-00198 (the Kraft insights GUI).
