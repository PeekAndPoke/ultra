# Insights: split data from rendering, expose it through a superuser REST API

**Status:** REVIEW FAILED — steps 1-8 run; 1 CRITICAL + 4 HIGH open, awaiting maintainer decisions
**Plan:** `.claude/tasks/20260730-frontend-sdk-vue-contributors.md` → Ordering **steps 4 and 5**
**Security-critical:** yes (superuser-only admin surface) → red-team follow-up task required

## Scope

The arc is: expose insights data through a superuser-only REST API, then have the SDK generator build
the frontend pages and components from `.vue` files in the module's resources.

This task is the **first half only** — plan steps 4 and 5. The Vue half (steps 6–7) is blocked on codegen
additions that do not exist yet: aggregation registries and `out.vue` (`20260729-ts-sdk-codegen.md`,
"Incoming requirements", items 3 and 4). Verified 2026-07-30 — codegen is at Phase 2 complete except Sse,
and grep finds no emit-phase registry, no profiles, no `out.shared`.

**Do not restate the plan here.** The envelope shape, the tab-registry-vs-sealed-hierarchy reasoning, the
Slumber-compat blocker table, the explicit-key decision and what deleting the renderers buys are all in
the plan's "Insights specifics" section. This file records only what that section does not: findings from
building it, and decisions that had to be made on contact with the code.

## What the plan already decides (pointers, not copies)

| Decision | Where |
|---|---|
| Open envelope `{ collectors: [{ key, data }] }`, `data` opaque | plan → "The tab registry replaces a sealed hierarchy" |
| `templateKey` must become an **explicit declared string**, not the class FQN | same section, checklist |
| Six Slumber-compat blockers in the collector DTOs | plan → "Slumber-compat blockers" |
| Deleting the renderers drops `ultra:semanticui` + `funktor:staticweb` from insights | plan → "What deleting the renderers buys" |

## Findings from contact with the code (2026-07-30)

### The raw GUI route cannot be gated — which is why this is an API, not a patch

`currentUserProvider()` returns an anonymous `User` when no principal is present, because "the route is
outside any `authenticate { }` block" (`funktor/rest/src/jvmMain/kotlin/auth/call.kt` KDoc), and the demo
mounts insights under `host("admin.*")` with **no `authenticate { }` block at all**
(`funktor-demo/server/src/main/kotlin/server.kt:103`). A hand-rolled check on
`gui/InsightsGui.kt:19` would deny everyone; re-plumbing ktor auth around it would reinvent `ApiRoutes`
including its boot-time chain validation (`AuthChainBootCheck`).

This retires the plan's step-1 assumption that the GUI could be "kept alive but superuser-gated" cheaply.

**Maintainer decision (2026-07-30): the static-web surface disappears wholesale with the Vue rewrite, so
nothing there is adjusted — not gated, not partially deleted.** Accepted consequence: the GUI routes stay
reachable without a credential until then; insights is network-protected only for that window. Recorded
so the red-team sweep treats it as a known, time-boxed exposure rather than a finding.

### `insights → rest` is not a cycle — no `funktor/insightsapi` module needed

Verified by compiling, not by reading. `rest` depends only on `funktor:core`, and the insights hook it
uses goes through `RequestMetricsProvider` — an interface **owned by core**
(`funktor/core/src/jvmMain/kotlin/metrics/RequestMetricsProvider.kt:12`) that `InsightsFull`/
`InsightsSlim` implement. `rest` never names the insights module.

### No hand-written `ApiClient` — the first funktor module to skip it

`IntrospectionApi` mounts via `IntrospectionApiClient.…mount { }`, with the client in
`funktor/inspect/src/commonMain`. That pattern exists because Kraft frontends hand-wrote clients, and the
plan counts exactly those files as deletion payoff. Insights has only `jvmMain`; adding a `commonMain` to
host a client codegen is meant to replace would be backwards.

So: declare `TypedApiEndpoint`s directly, no client class, no new source set. The TS client comes from
`RestApiTsContributor`. **This is the decision most worth challenging** — it diverges from every existing
funktor API module.

## The GUI's fate — decided 2026-07-30

The plan settled that the renderers are deleted; it never said **when**, and its step-1 answer ("keep the
GUI alive but superuser-gated") died with the finding above. Decided in discussion:

| Question | Decision |
|---|---|
| When does the GUI go? | **Now**, in this pass — not deferred to the Vue rewrite |
| The rendering code itself | **Preserved whole in `funktor/insights/reference/`**, outside the compile path — chosen over commenting in place once the volume was measured: ~1,062 render lines across ten collectors, 714 of them in Kontainer and Vault alone |
| `detailsUri` / `detailsUrl` | **Removed entirely.** It only ever existed as a cross-domain link to the `admin.*`-mounted staticweb routes; removing it also simplifies how api-responses are built |
| staticweb / semanticui deps | Removed from `funktor/insights` as a consequence |

Deleting the renderers first is what makes the rest cheap: the collector `Data` classes stop being
half-DTO-half-view, so the Slumber-compat blockers can be fixed without every shape change rippling into
kotlinx.html.

**Every live collector carries a `// VUE-REF:` pointer** to its preserved original, so
`grep -rn VUE-REF` lists what is still outstanding. Removal condition, recorded in
`reference/README.md`: when a collector's Vue tab ships, delete its reference copy; when every tab is
done, delete the directory.

### Cross-agent coordination: `detailsUri`/`detailsUrl` is a wire-format change

22 call sites, and two of them belong to the codegen agent's active work:

- `ultra/codegen/src/main/resources/ts/runtime/apiResponse.ts:58-59` — the hand-written TS mirror
- `ultra/codegen/src/test/kotlin/ts/ApiResponseParitySpec.kt:141-142` — the spec that pins the two together

Plus `ultra/remote/src/commonMain/kotlin/ApiResponse.kt` (published), `funktor/rest/respond.kt:84-85`,
`RequestMetricsProvider` in `funktor:core`, both insights impls, the Kraft ops page
(`funktor/inspect/src/jsMain/.../DevtoolsRequestHistoryPage.kt:62`) and two `ultra/remote` specs.

Remove it in ONE commit across Kotlin and TS. `ApiResponseParitySpec` exists precisely to catch this
drift, so a half-done removal fails loudly rather than silently — but it will also fail for the codegen
agent if they build mid-change.

## Build list

- [x] `ApiRoutes("insights", authFloor = { isSuperUser() })` + `InsightsApiFeature : ApiFeature`,
      registered in `insights_module.kt` beside the existing singletons
- [x] `GET /insights` — recent records: path, ts, method, url, status, durationMs
- [x] `GET /insights/{bucket}/{file}` — one full record
- [x] Explicit `key` on the collector data interface, replacing the FQN-derived `templateKey`
      (`InsightsCollectorData.kt:15`) and the stored `it::class.jvmName` (`impl/InsightsFull.kt:68`)
- [x] **`InsightsFull`'s hardcoded URI filter (`impl/InsightsFull.kt:33`) must exclude the new API route.**
      It currently excludes `/insights/bar` and `/insights/details`; miss this and the API collects
      insights about itself on every call
- [x] `funktor/insights/build.gradle.kts` — `api(project(":funktor:rest"))` (already added while
      verifying the cycle question)
- [x] **Header redaction is broken in both directions** (raised by the maintainer 2026-07-31; see the
      section below). Shared, extensible denylist; replace values entirely, never truncate
- [x] Fix `InsightsDataLoader.kt:36` — `Class.forName(it.cls)` runs before the `InsightsCollectorData`
      subtype check, so reading a stored file runs static initializers of whatever class it names.
      Resolve the declared key against a registry of known collectors instead
- [x] Before touching them: check what still calls `getRequestDetailsUrl()` / `getRequestDetailsUri()`
      (plan checklist item, not yet done)

## Credential capture in the collectors (found 2026-07-31)

Both directions are recorded in full and neither is properly anonymized. This is the most serious thing
found in insights so far, and it does not wait for the API work.

**Request side** (`collectors/RequestCollector.kt:47-51`) — every incoming header is stored. Exactly one
is treated specially, and it is **truncated, not redacted**:

```kotlin
if (k.lowercase() == "authorization") { k to v.map { it.substring(0, 20) + "..." } }
```

- **Leaks real credential material.** `Basic ` is 6 chars, so 14 base64 chars survive = 10 decoded bytes
  of `user:password` — for short credentials, all of it. funktor's API keys ride the *same* header
  (`apiKeyCaller` reads `Authorization: Bearer <token>`, see its KDoc at
  `funktor/rest/src/jvmMain/kotlin/auth/apiKeyCaller.kt:20`), where 13 surviving chars of a short key is
  a meaningful fraction.
- **Throws on short values** — `substring(0, 20)` on `Bearer abc`.
- **Covers only `authorization`.** `Cookie` is stored verbatim, and a session cookie is directly
  replayable — a worse leak than a truncated bearer token. Same for `X-Api-Key`, `Proxy-Authorization`,
  and any app-specific token header.

**Response side** (`collectors/ResponseCollector.kt:31`) — `call.response.headers.allValues()` with **no
redaction whatsoever**, including **`Set-Cookie`**. The insights record of a successful login therefore
contains a working session token.

**Why it compounds:** these records land in `./tmp/depot/insights` and are served by the GUI that has no
auth. Today, anyone reaching the `admin.*` host can lift session cookies out of captured login responses.

**Fix:** one shared redaction step used by both collectors — match a denylist of credential-bearing
header names (`authorization`, `proxy-authorization`, `cookie`, `set-cookie`, `x-api-key`, `api-key`,
`x-auth-token`), replace the value **entirely**, never truncate. Denylist rather than allowlist because
arbitrary custom headers are much of what makes insights useful; the residual risk is an unlisted
app-specific secret header, so the list must be **extensible by the application**, not hardcoded.

**Checked and CLEAN — do not re-investigate:** the `"$bucket/$file"` path handling is not a traversal
risk. `FileSystemRepository.getContent` calls `validateName()`, which rejects any `..`
(`funktor/cluster/.../fs/FileSystemRepository.kt:15`), and `File(root, path)` re-roots absolute children.
Its comment claims it checks "any slashes and dot-dots" but only checks dot-dots — a comment/code
mismatch worth one line, not a hole.

## Loop protocol (added 2026-07-31)

Running unattended. Work the queue top-down; each step is done only when its criterion holds.

| # | Step | Done when |
|---|---|---|
| 1 ✅ | `HeaderLogging` unit tests | exact + regex rules, last-match-wins, case-insensitivity, `DROP` removes the name, `REDACT` keeps it, the sensitive-name regex catches `x-amz-security-token`, an explicit later `LOG` beats it. **Mutation: flip a default rule to `LOG` and confirm a test fails** |
| 2 ✅ | API DTOs | Slumber can describe every field; no `Any`, no star projections, no computed properties |
| 3 ✅ | `InsightsApi : ApiRoutes("insights", authFloor = { isSuperUser() })` + `InsightsApiFeature`, registered in `insights_module.kt` | both endpoints answer; boot-time `AuthChainBootCheck` passes |
| 4 ✅ | E2E via `AppSpec`/`AppUnderTest` | anonymous denied, non-superuser denied, superuser 200. **Mutation: set `authFloor = { public() }` and confirm the denial tests fail** — a gate test that survives the gate's removal is worthless |
| 5 ✅ | E2E: open envelope | a record with an unknown collector key round-trips instead of failing the response |
| 6 ✅ | E2E: no self-observation | calling the API produces no insights record of itself |
| 7 ✅ | Compile sweep + full module tests | `^e:`-free; counts confirmed from `build/test-results/**/TEST-*.xml`, not from console alone |
| 8 ✅ | `/feature-review` | **run it, record findings, then STOP.** Do not auto-fix security findings unattended — leave them for the maintainer |

### On hitting a wall: park it, do not exit

A blocker stops **one step**, not the loop. Exiting on the first one wastes a night when the wall is a
single field in a single collector.

1. Append the blocker to "Blockers collected" below — the precise question, what was tried, and what
   the options are. A blocker recorded without its options is a wake-up call, not a hand-over.
2. Take the smallest reversible workaround that keeps the rest moving, and mark it in code as
   provisional. Example: `AppConfigCollector` blocks the DTOs → leave that one collector out of the API
   and carry on with the other nine, rather than stalling all of step 2.
3. Move to the next queue item that does not depend on the parked one.
4. When every queue item is done or parked, work the fallback pool below.
5. **Stop only when the queue is exhausted AND the fallback pool is empty.** Then report every collected
   blocker together, so one reply answers all of them.

### Decisions that must NOT be taken alone

- **`AppConfigCollector.Data(val info: Any, val config: Any)`** — `Any` cannot be typed for Slumber or
  codegen. Typed DTO, `JsonElement`, or drop the collector from the API?
- **Does the list endpoint page?** It feeds a Vue table that does not exist yet.
- **No superuser fixture** in the e2e harness that can be reused — do not invent an auth shape.
- Any step-8 finding rated security-relevant — record it, never auto-fix it unattended.
- Anything touching a file outside `funktor/insights` beyond those already in flight here.

### Fallback pool — always available, no decisions required

Ordered by value. All of it is genuinely useful and none of it needs the maintainer.

1. **Distil each reference renderer into a tab spec.** For every file in `reference/collectors/`, write
   down what the old tab actually displayed — fields, groupings, what the graphs plotted, which numbers
   were derived. This is precisely what `reference/` exists to enable, and it is the input the Vue work
   needs. Put it in `reference/TAB-SPECS.md`.
2. **Unit-test what has no auth dependency** — `InsightsDataLoader` prev/next selection at the ends of a
   list, the open-envelope shape, `HeaderLogging` cases missed the first time.
3. **Fix the stored-format star projection** — `CollectorData(val key: String, val data: Map<*, *>)`
   still uses `Map<*, *>`. Jackson does not care, but it is on the plan's blocker list.
4. **Tidy what the GUI removal left behind** — e.g. `DevtoolsRequestHistoryPage` now has an empty `td { }`
   whose `th { }` header is also empty; either remove both or leave a comment saying why the column stays.
5. **Update the plan doc** (`20260730-frontend-sdk-vue-contributors.md`) with what actually landed, so
   steps 4–5 are marked done rather than pending.

### Blockers collected

**B1 — the list endpoint's shape and cost.** *Parked; provisional implementation in place.*

The old GUI had **no list view at all** — `siblings` existed only to compute prev/next — so this is a
new surface with no precedent to follow. The cost problem is real: summary columns (method, url, status)
live inside each record, so a list of N rows means opening N files. `listItems` alone gives only path,
name, size and `lastModifiedAt`.

Provisional: `InsightsDataLoader.list(limit)` walks day folders newest-first and stops once `limit`
records are read. Bounded rather than paged, and marked provisional in the KDoc.

Options:
1. **Keep bounded-limit** (current). Simple, no cursor, no total count. Fine while records are small.
2. **Cursor paging** on the depot path, which sorts chronologically. Needs a page-size decision and a
   "has more" signal.
3. **Cheap list, lazy detail** — return only path + `lastModifiedAt` and let the frontend fetch each row.
   Fastest endpoint, but a request list without method/url/status is not usable as a table.
4. **A summary index** written alongside each record, so listing never opens a record. Fastest to read,
   but adds a second write path and a rebuild story for existing records.

Recommendation: 1 now, 2 when the Vue table exists and its page size is known. Nothing here blocks the
rest of the queue.

### Findings that REMOVE an anticipated blocker

**`AppConfigCollector.Data(val info: Any, val config: Any)` does not block this task.** The open envelope
serves every collector slice as a raw `JsonElement`, so the collector `Data` classes never enter the
API's type graph — Slumber is never asked to describe them, and nor is the walker. The `Any` becomes a
problem only when someone writes that tab's **codegen contributor** and roots `AppConfigCollector.Data`
to get a generated schema, i.e. plan steps 6–7.

Two consequences worth carrying forward:

- One of this task's three anticipated walls is not a wall. Do not stop for it.
- The plan's Slumber-compat blocker table describes the *contributor* stage, not the API stage. The same
  is true of `HttpMethod`/`HttpStatusCode` and `UserRecord`: they are inside slices, so they are opaque
  to the API and only matter per-tab later.

**`@Serializable` already enforces "no `Any`, no star projections"** — at compile time. Adding
`val probe: Any?` to a DTO fails the build, because kotlinx has no serializer for `Any` (verified by
mutation). The DTO criterion therefore needs no runtime test; `InsightsModelsSlumberSpec` exists to pin
the complementary property, that the shape Slumber emits is the one a client parses back.

### Guardrails — three agents share this worktree

- **Do not run `:ultra:codegen:check`.** The codegen agent has uncommitted work in `ts/runtime/client.ts`, `sse.ts` and `ts-verify/verifyRuntime.ts`; its result would be about their tree, not this change.
- **Stage only `funktor/insights` and this task file.** Never `git add -A` at the root — `ultra/slumber`, `ultra/log`, `ultra/vault` and `ultra/codegen` all have other owners.
- **If a build looks impossibly stale** — a signature error against code that was just fixed, or an `AbstractMethodError` surfacing as an assertion failure — suspect a concurrent build, not the logic. Recovery:
  `rm -rf funktor/insights/build/classes/kotlin/**/test` and re-run. This is the failure mode recorded in CLAUDE.md's verification traps, and its cause was two builds interleaving in one worktree.
- Commit per completed step, so a killed loop leaves reviewable work rather than a half-edited tree.

## Test evidence

All counts read from `build/test-results/**/TEST-*.xml`, never from console output.

- [x] E2E via `AppSpec`/`AppUnderTest` (`funktor/all/src/jvmTest/kotlin/InsightsApiSpec.kt`, 8 tests):
      anonymous → denied, authenticated non-superuser → denied, superuser → 200; a missing record is a
      404 rather than an error; two different record paths are refused identically, so status cannot be
      used to probe which files exist; the response never echoes the token that fetched it
- [x] **Gate mutation** — `authFloor = { public() }` fails **5 of 8** e2e tests and **2 of 6** route
      tests. Restored and re-verified green
- [x] E2E/unit: a record naming an unknown collector round-trips with its payload intact
      (`InsightsDataLoaderSpec`), and arbitrary JSON survives Slumber (`InsightsModelsSlumberSpec`)
- [x] Unit: `InsightsFull.isExcluded` covers the API's own routes, derived from `InsightsApi.base`
- [x] Unit: a short Authorization header neither throws nor is partially disclosed; no fragment of a
      redacted value survives (`HeaderLoggingSpec`)
- [x] **Redaction mutation** — flipping the sensitive-name rule to `LOG` fails 1 test; dropping `cookie`
      from the known list fails exactly the Cookie/Set-Cookie test
- [x] Compile sweep across jvm/js/common — no `^e:`
- [x] Full commands: `./gradlew :funktor:insights:jvmTest :funktor:all:jvmTest` →
      **42 tests / 5 specs** in insights and **146 tests / 10 specs** in funktor:all, 0 failures,
      0 errors

### Defects found by the tests themselves

1. **Redaction rule shadowing** — the sensitive-name heuristic was appended after the known-sensitive
   list, and last-match-wins meant `.*auth.*` overrode `authorization`'s own rule. Invisible while both
   said REDACT; it would have surfaced as an un-redacted credential the first time the pattern's action
   changed. Found by mutation, not by reading.
2. **Over-broad self-observation filter** — `contains("/insights")` also matched an application's own
   routes, so `/api/insights-dashboard` would have been silently unrecorded. Now matched against
   `InsightsApi.base`.

## Review record (/feature-review, 2026-07-31)

Diff reviewed: `b62ab6cd^..HEAD` restricted to this task's 38 files. Every finding below was
**re-verified against the code by the coordinator** before being recorded; nothing is forwarded on a
reviewer's word alone.

**GATE: FAIL** — one CRITICAL and four HIGH open. **Nothing was auto-fixed.** The protocol forbids
unattended security fixes, and every remaining finding needs a design decision or interacts with another.

| Reviewer | Verdict | Confirmed |
|---|---|---|
| 1. Implementation & code style | FAIL | 2 HIGH, 4 MEDIUM, 4 LOW |
| 2. Domain expert | FAIL | 1 CRITICAL, 3 HIGH, 8 MEDIUM, 5 LOW |
| 3. Security | FAIL (no CRITICAL) | 1 MEDIUM, 5 LOW/latent |

Reviewers 1 and 2 independently found the `limit`/codegen defect, and 1, 2 and 3 all landed on the
query-string gap from different directions. Convergence, not duplication.

### CRITICAL-1 — every pre-existing record loads as `200 OK` with an empty envelope

`InsightsDataLoader.kt:105`. `slices()` requires `obj["key"]` and `mapNotNull`s the entry away when it
is absent. Records written before this change carry `cls` — the JVM class name — and never `key`.

**Verified against real data, not inferred:** `funktor-demo/server/tmp/depot/insights` holds **2,116
records, 536 MB, going back to 2025-11**, and every collector entry has exactly `['cls', 'data']`.

So a superuser opening the list against any existing depot gets one blank row per record — `method`,
`url`, `status` all null — and `load()` answers `200 OK` with `collectors: []`. No 404, no error, no log
line. **Silent total data loss presented as success**, and it is indistinguishable from a record that
genuinely contains nothing.

I changed `CollectorData(cls)` to `CollectorData(key)` (`impl/InsightsFull.kt:76`) without a read path
for the old shape, and no test caught it because every fixture I wrote uses the new format.

**Options:** map `cls` → key through the known-collector registry (`RequestCollector$Data` → `request`
is lossless for all ten built-ins); or refuse the record explicitly — `null` → 404, or a sentinel
`{key: "__unreadable"}` slice. Anything beats success-with-nothing. Interacts with HIGH-5.

### HIGH-3 — "newest first" and prev/next are computed from mtime, which is not request order

`InsightsDataLoader.kt:49,82`. Records are written asynchronously —
`supervisorScope { launch(Dispatchers.IO) { delay(1) … } }` (`impl/InsightsFull.kt:66-83`) — so mtime is
*write* time, and under concurrency it reorders.

**Measured on real data** (`records-2026-03-30`): five records whose filenames run
`…872, …875, …895, …905, …945` have mtimes `.278, .260, .210, .518, .250`, so sorting by mtime yields
`905, 872, 875, 945, 895`. Nothing like request order. In the same folder, 89 files share 77 distinct
mtimes — 12 exact ties, where order falls back to arbitrary directory order, so prev/next between tied
records is undefined.

The record's own timestamp is already in its filename, and lexicographic order of
`LocalDateTime.toString()` **is** chronological. One-line fix in both places: sort by `name`.
`recordedAt` is likewise the write time presented under a name that reads as the request time.

### HIGH-5 — the stored format has no version, and CRITICAL-1 is what that costs

`domain.kt:5`. `InsightsData` carries no `formatVersion`. CRITICAL-1 *is* an unversioned format
migration, silently unhandled — and it recurs per collector: once a tab's zod schema is generated from
today's `KontainerCollector.Data`, a record from three months ago has the old shape under the same key
and the tab's `.parse()` fails, with nothing in the envelope to branch on.

Add `formatVersion: Int` now, while the only consumer is still unwritten. One field.

### Additional domain findings

| # | Sev | Where | Claim |
|---|---|---|---|
| D-M1 | MEDIUM | `InsightsModels.kt:16,45` vs `InsightsApiFeature.kt:17` | **The record id is returned as one string and consumed as two.** Summaries expose `path = "records-…/x.json"` but `getRecord` takes `RecordParam(bucket, file)`, so every generated call site must `path.split('/')` — depot layout re-implemented in the frontend, wrong the day a repository nests differently |
| D-M2 | MEDIUM | `InsightsDataLoader.kt:49` | **prev/next never cross a day folder**, but the KDoc says "null at either end". 16 day folders in the demo depot = 16 pairs of false ends |
| D-M3 | MEDIUM | `InsightsApi.kt:46` | **`MAX_LIMIT = 500` against 236 KB mean record size** — measured. `limit=50` parses 8.3 MB to read five scalars; `limit=500` parses ~83 MB, on a handler thread, since `FsFileContent.getContentBytes()` calls `readBytes()` with no `withContext(Dispatchers.IO)` |
| D-M4 | MEDIUM | `InsightsCollectorData.kt:21` | **`key` is serialized INTO the payload as well as onto the envelope**, because `override val key = KEY` is a getter and `mapper.convertValue(it)` emits it. **Confirmed on real data:** old records carry `templateKey` inside `data` by exactly this mechanism. This is the same shape-skew I removed `fullUrl` to avoid. Fix: move `key` onto `InsightsCollector`, not its `Data` |
| D-M5 | MEDIUM | `InsightsDataLoader.kt:126` | **Duplicate collector keys are silently last-wins.** Nothing validates uniqueness at registration, write or read; an app naming its collector `"request"` shadows the framework's. With `key` on the collector (D-M4) a boot check is three lines |
| D-M6 | MEDIUM | `InsightsModels.kt:44` | **The open envelope has no discovery and no fallback**, so extensibility is half-delivered: an app-defined collector gets a key nobody renders and no way to be enumerated. Decide before the tab registry is built — a generic JSON-tree fallback tab closes it cheaply |
| D-M8 | MEDIUM | `InsightsApiFeature.kt:22` | **Feature name `"Insights"` is not namespaced** — siblings are `"Funktor Introspection"`, `"Funktor Cluster"`, `"Funktor Logging"`. `TsClientNames.clientFile("Insights")` → `insightsClient.ts`, which an app's own `Insights` feature collides with. Free to fix now, breaking once Vue tabs import from it |
| D-L2 | LOW | `InsightsModels.kt:29,42` | `durationMs` is `Double?` on the summary and `Double` on the record for the same value, and the loader returns `0.0` for missing timing — "unknown" is indistinguishable from "instant" |
| D-L5 | LOW | `FileSystemRepository.kt:103` | Outside the diff: `listItems(path, limit)` **ignores `path`** (`listItems().take(limit)` — root, not `path`). My loader avoids it, but it is the obvious optimisation for D-M3 and would silently read the wrong folder |

### Domain questions answered clean

- **The open envelope holds.** The reasoning survives scrutiny; its costs are real but acceptable.
- **No hand-written `ApiClient` is safe** — verified, not assumed. Mounting is `ApiFeature`-driven, the
  e2e harness drives `ApiRoute` directly, introspection enumerates features not clients, and the access
  matrix works off `estimateAccess`. Nothing in funktor requires a client to exist.
- **Codegen will walk these routes.** `MpInstant` and `JsonElement` are both claimed; path params are
  bare-identifier strings filling declared placeholders; responses are `ApiResponse<…>` envelopes.
  Nothing hard-fails the walker.
- **Back-compat sweep clean** — no live reference to any removed symbol survives outside `reference/`.
- **Kontainer scoping correct** — `InsightsApiFeature` has zero constructor dependencies and reaches the
  `dynamic` loader through `call.kontainer`, per the SemiDynamic rule.

### HIGH-1 — the generated TypeScript client can never send `limit`

`api/InsightsApi.kt:55` reads `limit` from `call.request.queryParameters` directly.

**Verified:** codegen derives query params *exclusively* from the route's PARAMS type —
`RestApiTsContributor.paramsOf()` iterates `typed.reifiedParamsType.ctorParams2Types`
(`funktor/codegen/src/main/kotlin/RestApiTsContributor.kt:259`). `ListRecords.mount { }` is the
no-PARAMS overload, so the params type is `Unit` and the emitted client is
`listRecords = () => request(...)` — no arguments at all.

This is self-inflicted by the very decision this task is proudest of. Having chosen "no hand-written
client, codegen generates it", an endpoint whose only tuning knob is invisible to codegen is pinned at
50 rows forever, and `MAX_LIMIT` is unreachable. It is also the only `queryParameters[...]` read in all
of `funktor/` — every other API uses a typed params class, including GET routes with no path
placeholders (`LoggingApi.ListParam`, `BackgroundJobsApi.PagingParam`).

**Fix:** `data class ListParam(val limit: Int = DEFAULT_LIMIT)` on the feature, mount with
`ListRecords.mount(ListParam::class)`, clamp inside the handler. Interacts with blocker B1 — settle the
paging shape at the same time.

### HIGH-2 — nothing tests that the collectors apply the redaction

`collectors/RequestCollector.kt:42`, `ResponseCollector.kt:30`.

**Verified:** zero test files anywhere reference `RequestCollector`, `ResponseCollector` or
`headerLogging`. `HeaderLoggingSpec` is a thorough test *of the policy object*, but the policy is only
worth anything if it is called.

**The mutation that proves it:** restore `headers = call.request.headers.toMap()` — the exact
pre-change behaviour this whole task exists to fix — and the entire suite stays green. `Set-Cookie` goes
back into every login record and nothing notices. My own evidence table mutated the *policy* and called
it covered; it never mutated the *call site*. That is precisely the "right for the wrong reason" trap
`CLAUDE.md` warns about, and I walked into it.

Also unproven: the task's mandatory "the list must be extensible by the application" — nothing shows
that replacing the `instance(HeaderLogging.defaults)` binding reaches the collectors.

**Fix:** a spec that builds `RequestCollector(policy)` / `ResponseCollector(policy)` against a ktor
test call with `Cookie`/`Set-Cookie`/`Authorization` set and asserts `REDACTED` in the produced `Data`;
plus one with a non-default policy proving the override takes effect.

### Security findings — recorded, NOT fixed (unattended-fix rule)

| # | Sev | Where | Claim |
|---|---|---|---|
| S1 | MEDIUM | `RequestCollector.kt:41,43` | **Query strings are stored verbatim, twice.** `queryParams` gets no policy at all, and `uri = call.request.uri` includes the query string, which `InsightsDataLoader.kt:140` rebuilds into `InsightsRecordSummary.url`. An OAuth callback (`?code=`), magic link (`?token=`) or presigned URL (`?X-Amz-Signature=`) is stored and served. `HeaderLogging` exists to keep credentials out of records; the other half of the same request got no policy |
| S2 | LOW/MED | `HeaderLogging.kt:67-88` | **Deny-list gaps.** `referer` (carries the previous URL — where magic-link and OAuth query strings live), `cookie2`/`set-cookie2` (`cookie` is exact-match only; the regex has no `cookie` alternative, so cookie variants fail open), `stripe-signature`/`x-hub-signature-256`, `x-access-key` |
| S3 | LOW | `impl/InsightsFull.kt:44` | **Two independent ways to defeat `isExcluded`.** Reviewer 3: `uri` is un-decoded while ktor's router decodes, so `/_/funktor/%69nsights/records` routes but is not excluded. Reviewer 1: `uri` includes the query string, so `GET /api/orders?next=/_/funktor/insights` silently suppresses its own record. Both confirmed. The KDoc I wrote claims precision the code does not have |
| S4 | LOW, latent | `funktor/cluster/.../fs/FileSystemRepository.kt:76,115,139` | **Depot symlinks are followed; root is not canonicalised.** A symlink planted in the depot resolves outside it and `getContent` reads through. Converts "records on disk are readable with filesystem access" into "any file the app user can read, exfiltrated remotely to an authenticated admin". Pre-existing; this API makes it remotely reachable. Matches the gap already noted as unchecked in the red-team file |
| S5 | LOW, latent | `api/InsightsModels.kt:58-61` | **An obligation created, not a live bug.** `data` is a pass-through `JsonElement` and `LOG` is the default, so an unauthenticated attacker's `User-Agent: <img src=x onerror=…>` is stored verbatim and returned to whatever renders it. The deleted GUI used kotlinx.html, which escaped; the replacement makes escaping the consumer's problem and nothing says so |
| S6 | LOW | `InsightsDataLoader.kt:49,80-82` | `MAX_LIMIT` bounds records *parsed*, not directory entries *touched* — `listItems` materialises and stats every entry in a day folder before `limit` is consulted. Superuser-triggered, so not an outsider DoS |

### Other confirmed findings

| # | Sev | Where | Claim |
|---|---|---|---|
| I3 | MEDIUM | `InsightsApiRoutesSpec.kt:80` | "the list limit is bounded" asserts two constants against literals. Deleting `.coerceIn(1, MAX_LIMIT)` leaves it green. `?limit=abc/0/-1/99999` are all untested |
| I4 | MEDIUM | `InsightsApiSpec.kt:45-66` | **The two 200-path e2e tests are vacuous.** The test app never enables insights, so the depot is empty: `shouldNotContain superUserToken` passes because there is no data, and `apiResponseData<List<…>>() shouldNotBe null` passes on an empty list. **No e2e test ever serialises a populated record over HTTP**, and `getRecord`'s 200 branch is never exercised. Worse, `InsightsFileRepository` is rooted at the *relative* `./tmp/depot/insights`, so on a machine where the demo has run these tests read leftover files — different behaviour per machine |
| I5 | MEDIUM | `funktor/core/.../InsightsConfig.kt:9` | `baseUrl` is now dead — its only reader was the removed `getRequestDetailsUrl()` — but it is still a public field of a published config class, and both demo confs still set it to a route that no longer exists |
| I6 | LOW | `InsightsDataLoader.kt:132,140` | `port` is excluded from the null guard, so a record without one yields the literal `"https://example.com:null/x"` |
| I7 | LOW | `InsightsDataLoaderSpec.kt:21` | `java.io.File` — a fully-qualified class name, direct `CLAUDE.md` violation. The only one in the diff |
| I8 | LOW | `InsightsFull.kt:22`, `respond.kt:12`, `DevtoolsRequestHistoryPage.kt:9` | Three imports left dangling by the deletions; plus three blank lines in `build.gradle.kts` |
| I9 | LOW | `InsightsDataLoaderSpec.kt:158` | Ordering derives from millisecond mtime and the test forces it with `Thread.sleep(10)`. Record filenames already encode the timestamp and sort chronologically — a better key, needing no `stat`, and immune to the write being async (`InsightsFull.finish` writes inside `launch(Dispatchers.IO) { delay(1) }`, so mtime order can differ from record order under concurrency) |

### Probed and CLEAN — do not re-investigate

- **The floor genuinely covers both routes.** `ApiRoutes.addRoute` is the sole registration path, both `Get.mount` overloads funnel through it, it applies `withFloor` and asserts non-empty. `withFloor` *prepends*, so a per-route `authorize {}` can only strengthen. The floor is caller-only and runs in routing **phase 1**, before parameter conversion — so `bucket`/`file` handling is unreachable for a non-superuser and the 401 is identical whether the record exists or not.
- **Realm-agnostic `isSuperUser()` is the house convention** — `DepotApi`, `VaultApi`, `OrgsApi`, `LoggingApi`, `IntrospectionApi`, `BackgroundJobsApi` all floor identically. Framework-wide question, not a defect here. Worth noting only that insights is the most valuable of the set, since `AppConfigCollector` serialises the entire `AppConfig`.
- **No ReDoS** in the sensitive-name regex: `.*(literal|…).*` with one optional `-?`, no nested quantifier over an overlapping alternation. O(n·k).
- **No path traversal out of the depot root.** Params arrive already decoded, so `%2e%2e` becomes `..` and `validateName()` rejects it; there is no second decode. `%2f` injects a slash but a slash alone cannot leave the root; `File(parent, absoluteChild)` resolves *inside* parent on Unix; a NUL byte makes `exists()` false → 404.
- **No unsafe deserialization.** `Class.forName` is gone with no equivalent; `InsightsMapper` registers no polymorphic typing; deep nesting raises `StackOverflowError` which `runCatching` turns into a skipped record rather than a downed request.
- **`limit` parsing leaks nothing** — garbage, negative, zero and oversized all normalise silently.
- **`HeaderLogging.actionFor` precedence is consistent** with its KDoc and tests; `defaults` places the pattern *before* the exact list so explicit entries win; immutability is real.
- **prev/next index arithmetic is correct** at both ends and for a not-found path.
- **`detailsUri`/`detailsUrl` removal is complete and atomic** across all nine files. It was also a real hardening, not just cleanup: before it, *every* API response — including anonymous ones — carried the depot path of its own record, which would have made `GET /records/{bucket}/{file}` directly addressable without the list endpoint.
- **No new wildcard imports**; all KDoc `[Reference]` links resolve; `RecordParam` placement and `call.kontainer.get()` in handlers both match existing funktor patterns.

**Red-team follow-up** (required, security-critical): `.claude/tasks/20260730-redteam-insights-api.md`
