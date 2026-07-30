# Insights: split data from rendering, expose it through a superuser REST API

**Status:** DESIGN — awaiting confirmation before implementing
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
| The rendering code itself | **Comment it out in place**, do not delete. It is the reference for what each Vue tab has to show |
| `detailsUri` / `detailsUrl` | **Removed entirely.** It only ever existed as a cross-domain link to the `admin.*`-mounted staticweb routes; removing it also simplifies how api-responses are built |
| staticweb / semanticui deps | Removed from `funktor/insights` as a consequence |

Deleting the renderers first is what makes the rest cheap: the collector `Data` classes stop being
half-DTO-half-view, so the Slumber-compat blockers can be fixed without every shape change rippling into
kotlinx.html.

**Mark the commented-out blocks `// VUE-REF:`.** Commented code rots and nobody dares delete it later
because nobody knows whether it is still the truth. A grep marker gives it an explicit expiry: when a
collector's Vue tab ships, grep the marker and delete that block. Recorded here as the removal condition.

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

- [ ] `ApiRoutes("insights", authFloor = { isSuperUser() })` + `InsightsApiFeature : ApiFeature`,
      registered in `insights_module.kt` beside the existing singletons
- [ ] `GET /insights` — recent records: path, ts, method, url, status, durationMs
- [ ] `GET /insights/{bucket}/{file}` — one full record
- [ ] Explicit `key` on the collector data interface, replacing the FQN-derived `templateKey`
      (`InsightsCollectorData.kt:15`) and the stored `it::class.jvmName` (`impl/InsightsFull.kt:68`)
- [ ] **`InsightsFull`'s hardcoded URI filter (`impl/InsightsFull.kt:33`) must exclude the new API route.**
      It currently excludes `/insights/bar` and `/insights/details`; miss this and the API collects
      insights about itself on every call
- [ ] `funktor/insights/build.gradle.kts` — `api(project(":funktor:rest"))` (already added while
      verifying the cycle question)
- [ ] **Header redaction is broken in both directions** (raised by the maintainer 2026-07-31; see the
      section below). Shared, extensible denylist; replace values entirely, never truncate
- [ ] Fix `InsightsDataLoader.kt:36` — `Class.forName(it.cls)` runs before the `InsightsCollectorData`
      subtype check, so reading a stored file runs static initializers of whatever class it names.
      Resolve the declared key against a registry of known collectors instead
- [ ] Before touching them: check what still calls `getRequestDetailsUrl()` / `getRequestDetailsUri()`
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
| 1 | `HeaderLogging` unit tests | exact + regex rules, last-match-wins, case-insensitivity, `DROP` removes the name, `REDACT` keeps it, the sensitive-name regex catches `x-amz-security-token`, an explicit later `LOG` beats it. **Mutation: flip a default rule to `LOG` and confirm a test fails** |
| 2 | API DTOs | Slumber can describe every field; no `Any`, no star projections, no computed properties |
| 3 | `InsightsApi : ApiRoutes("insights", authFloor = { isSuperUser() })` + `InsightsApiFeature`, registered in `insights_module.kt` | both endpoints answer; boot-time `AuthChainBootCheck` passes |
| 4 | E2E via `AppSpec`/`AppUnderTest` | anonymous denied, non-superuser denied, superuser 200. **Mutation: set `authFloor = { public() }` and confirm the denial tests fail** — a gate test that survives the gate's removal is worthless |
| 5 | E2E: open envelope | a record with an unknown collector key round-trips instead of failing the response |
| 6 | E2E: no self-observation | calling the API produces no insights record of itself |
| 7 | Compile sweep + full module tests | `^e:`-free; counts confirmed from `build/test-results/**/TEST-*.xml`, not from console alone |
| 8 | `/feature-review` | **run it, record findings, then STOP.** Do not auto-fix security findings unattended — leave them for the maintainer |

### Stop and wake the maintainer

- **`AppConfigCollector.Data(val info: Any, val config: Any)`** — `Any` cannot be typed for Slumber or codegen. Typed DTO, `JsonElement`, or drop the collector from the API? Not a call to make alone.
- **Does the list endpoint page?** It feeds a Vue table that does not exist yet.
- **No superuser fixture** in the e2e harness that can be reused — do not invent an auth shape.
- Any step-8 finding rated security-relevant.
- Anything that would touch a file outside `funktor/insights` **except** the ones already in flight here.

### Guardrails — three agents share this worktree

- **Do not run `:ultra:codegen:check`.** The codegen agent has uncommitted work in `ts/runtime/client.ts`, `sse.ts` and `ts-verify/verifyRuntime.ts`; its result would be about their tree, not this change.
- **Stage only `funktor/insights` and this task file.** Never `git add -A` at the root — `ultra/slumber`, `ultra/log`, `ultra/vault` and `ultra/codegen` all have other owners.
- **If a build looks impossibly stale** — a signature error against code that was just fixed, or an `AbstractMethodError` surfacing as an assertion failure — suspect a concurrent build, not the logic. Recovery:
  `rm -rf funktor/insights/build/classes/kotlin/**/test` and re-run. This is the failure mode recorded in CLAUDE.md's verification traps, and its cause was two builds interleaving in one worktree.
- Commit per completed step, so a killed loop leaves reviewable work rather than a half-edited tree.

## Test evidence

- [ ] E2E via `AppSpec`/`AppUnderTest`: anonymous → denied; authenticated non-superuser → denied;
      superuser → 200 with the record
- [ ] **Mutation-test the gate** — flip `authFloor` to `public()` and confirm the denial tests fail. A
      gate test that still passes with the gate removed is worthless
- [ ] E2E: a record with an unknown collector key round-trips (proves the envelope stays open)
- [ ] E2E: calling the API does not produce an insights record of itself (the URI-filter item above)
- [ ] Unit: a short Authorization header neither throws nor is partially disclosed
- [ ] Full test command(s) run + green: `...`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

**Red-team follow-up** (required, security-critical): `.claude/tasks/20260730-redteam-insights-api.md`
