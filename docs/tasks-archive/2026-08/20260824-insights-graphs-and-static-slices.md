# Insights — the three graphs, and moving static data off every record

**Status:** DONE (archived 2026-10-19) — Parts A+B (track T2, gate PASS round 1, merged `d9ffbb32`); Parts C+D
(track T1, gate PASS round 2, merged `a578ef0a`). Created 2026-08-24 from maintainer decisions on
`docs/tasks/20260802-insights-vue-tabs.md`'s gap inventory. No docs follow-up yet: insights is still moving (the
production gate, T4's Kraft GUI) — revisit when it settles. Round-2 MINORs: `docs/tasks/20261019-insights-vue-round2-minors.md`.
**Plan:** ULTRA-00153 (frontend sdk vue contributors)
**Security-critical:** yes — it adds REST endpoints that expose the service graph and the database
schema. Both are superuser-only today only because they are inside a record; as endpoints they need
their own floor. Red-team follow-up required.

## Why this is one task and not two

The graphs and the record slimming are the same decision seen from two sides. The old GUI drew three
vis.js networks:

| Graph | Source in the old GUI | Derivable from a record today? |
|---|---|---|
| Kontainer "instances" | the record's own `info.services` | yes |
| Kontainer "full" | the record's own `info.services` | yes |
| Database (repositories) | `DatabaseGraphBuilder`, a **live kontainer service** at render time | **no** |

**Maintainer's insight (2026-08-24), and it reframes the whole thing:** the full kontainer graph is
*also* static — it describes the container, not the request. There is no reason to write it into every
record. So:

> **A record should carry only what is REQUEST-SPECIFIC: which services were instantiated.**
> The static definition graph is fetched once from its own endpoint and merged client-side.

That turns the vault graph from an exception into the general rule, and it pays for itself immediately:
the `kontainer` slice is **137.8 KB average, 73.6% of a record**. Almost all of that is the static
service definitions repeated on every single request.

## Part A — new endpoints for the static data

- [x] **`GET /_/funktor/insights/graphs/kontainer`** — the full service definition graph. Static per
      boot, so it is cacheable client-side for the lifetime of the page. DONE 2026-10-19.
- [x] **`GET /_/funktor/insights/graphs/database`** — `DatabaseGraphModel` from `DatabaseGraphBuilder`.
      This is what makes the vault graph possible at all. DONE 2026-10-19.
- [x] **Both go on `InsightsApi`**, which already floors at `isSuperUser()` for the whole group. Do not
      mount them anywhere else: a service graph names every class in the app, and the database graph
      names every repository and reference. Neither is safe below that floor. DONE; mutation-checked (S1, S2).
- [x] **Consider an ETag or a boot id.** Both are static per boot, so a client that has one need not
      refetch. Cheap, and it is the difference between one fetch and one per page view.
      **Decided: a content fingerprint, no HTTP ETag.** The kontainer graph carries `fingerprint`
      (SHA-256 of its services) and every new record's slice carries the `graphFingerprint` it was
      written against, so a client can cache the graph by fingerprint and refetch only for a record that
      names another one. It also answers what an ETag cannot: whether an OLD record joins the graph
      served today (after a redeploy it may not). An HTTP 304 would bypass `apiRespond`'s envelope, and
      the SPA keeps the graph in memory anyway. The database graph has no fingerprint: nothing joins it.

## Part B — slim the kontainer slice

- [x] `KontainerCollector.Data` keeps **only the instantiated services** (and, until 2026-10-19 T1, `numOld` /
      `numTotal` — dropped by maintainer decision: always 0) —
      the `instances` list with its `createdAt` values, plus enough identity (the service FQN) to join
      against the static graph. DONE 2026-10-19, plus `type` per service and `graphFingerprint` (see
      the wire contract).
- [x] **Drop `info.services`' definitions from the record.** That is the 73.6%. DONE.
- [x] **This is a WIRE BREAK for existing records.** Old records carry the fat shape and will still be
      read by the frontend. `slices.ts`'s readers already narrow rather than cast, so an old record
      degrades rather than throwing — but the tab must be explicit about which shape it got, not
      silently show an empty table for one of them. **Backend half DONE**: old records are served
      unchanged (asserted JSON-equal), and the new slice lives under a new top-level key (`services`, not
      `info.services`), so today's reader cannot mistake it for the old shape — it returns null and the
      tab falls back to its raw tree (verified by running it, below).
  - [x] **(T1)** the tab tells the two shapes apart and says which one it shows. DONE 2026-10-19.
- [x] Re-measure a record afterwards. The point of the exercise is the size; state the before and after.
      DONE — see "Record size".

## Part C — the graph library

**Chosen: `vis-network`.** The old renderers are vis.js, so the node/edge construction, the colour map
and the physics options port almost verbatim — `reference/collectors/KontainerCollector.kt:172-440` and
`VaultCollector.kt:209-...` are the spec. Rewriting them against a different API would be a translation
with no upside.

Checked on npm 2026-08-24, rather than from memory:

- **Latest is `vis-network@10.1.2`**, licensed `(Apache-2.0 OR MIT)`.
- **It has no runtime `dependencies`** — but **six `peerDependencies`**: `uuid`, `keycharm`, `vis-data`,
  `vis-util`, `@egjs/hammerjs`, `component-emitter`. The old v6 line depended on `moment`; v10 does not.

- [x] **Verify the peer install rather than assuming it.** DONE 2026-10-19 (T1): pnpm 10.26.2 put all six
      peers (+ `@types/hammerjs`) into `sdkgen-app`'s lockfile unasked; `vite build` is green there. `auto-install-peers` defaults to true in
      pnpm 8+, and the demo app is on `pnpm@10.26.2` with no `.npmrc` overriding it — so this probably
      just works. Probably is not a good enough basis for a shipped component: install it in
      `sdkgen-app` and check `vite build` before committing to the choice.
- [x] **The generator cannot install it.** DONE 2026-10-19 (T1) as a CHECK in `sdk:ts:generate`, not an
      `out.requires` API (that would be `ultra:codegen`'s) — see "T1 — implementation notes". A component that imports `vis-network` needs the dependency in
      the APP's `package.json` — a file outside `<out>/`, which the ownership rule forbids the generator
      to write. This is exactly the `out.requires` case: emit the requirement and verify it, failing with
      the line to paste. Coordinate with the codegen agent; do not silently ship an import that only
      works in the demo app.

**If the peer-dependency surface turns out to be a problem**, the fallback is worth knowing: the
kontainer graph is a force-directed layout of a few hundred nodes, which is a few hundred lines of SVG
and `d3-force`-style physics. That is a real cost, but it is bounded, and it removes seven packages from
every consuming app. Do not take this branch without measuring the first.

## Part D — the tabs

- [x] `KontainerTab`: two buttons, "Show instances Graph" and "Show Full Graph", differing only in
      whether services with no instances are included. Node label `simpleName\ntype`, size = injected
      class count, colour by type (Singleton `#1E90FF`, Prototype `#FFD700`, Dynamic `#8B0000`,
      SemiDynamic `#9400D3`, DynamicOverride `#DC143C`); edge per injection, arrow at the target,
      **dashed when `provisionType == Lazy`**, weight 2 Direct / 1 Lazy.
- [x] `VaultTab`: the "Show Database Graph" button and its network, plus the old "Database Graph Data"
      `<details>` holding the raw model.
- [x] **Both fetch lazily, on first click.** The old GUI did the same (`setTimeout(..., 500)` after
      revealing the container) because a hidden canvas cannot be laid out. More importantly here: the
      static graph is a second HTTP request, and a user who never opens the tab should never make it.
- [x] Both tabs must still render **without** the graph — the endpoint can 403 for a non-superuser or
      fail outright, and a broken graph must not take the tab with it.

## Verification

- [x] e2e for both endpoints, including that a non-superuser gets the floor's answer, not the graph.
      DONE 2026-10-19: `InsightsGraphsApiSpec`, `InsightsDatabaseGraphBackendsSpec`.
- [x] `vite build`, not just `vue-tsc` — a new dependency and new imports are exactly what a typecheck
      cannot confirm (see the `shims-vue.d.ts` note in the insights task). DONE 2026-10-19 (T1): on every
      `check` in `funktor/codegen/vue-verify`, and in `sdkgen-app`.
- [x] Record size before and after Part B, measured on a real record from `InsightsRecordingSpec`.
      DONE — the same request, recorded by `InsightsKontainerSliceSpec`.
- [x] An OLD (fat) record still renders the kontainer tab.
  - [x] Backend half: an old record still LOADS through the API, its slice untouched
        (`InsightsKontainerSliceSpec`, frozen real fat record). DONE 2026-10-19.
  - [x] **(T1)** the tab renders it — `vue-verify/tests/tabs.spec.ts`, on the served `FAT_RECORD`.

## Red-team follow-up

Extend ULTRA-00161 (redteam insights api) rather than opening a new file: the two new
endpoints expose the full class graph and the database schema, which is a bigger disclosure surface than
anything currently under that floor.

**Not done by T2** — that file is outside the track's edit scope. Scenarios to add, collected not run:
reach either graph below superuser by path variants (trailing slash, `%2F`, case, `HEAD`/`OPTIONS`);
what the graphs disclose to a superuser (source file and line in every `codeLocation`, database server
version and database name in every `connection`); hammering `graphs/database`, which reflects over every
repository and costs one blocking server round trip per driver per call.

## Wire contract — observed 2026-10-19 on real data (T2)

**Read from real files, not from the Kotlin types.** `INSIGHTS_WIRE_SAMPLES_DIR=<dir> console/wt-gradle.sh
:funktor:all:jvmTest` makes `InsightsKontainerSliceSpec`, `InsightsGraphsApiSpec` and
`InsightsDatabaseGraphBackendsSpec` dump the real record, the API responses and both graphs; regenerate
rather than trust this section once the code moves. Every response is the usual envelope
`{status: {value, description}, data, messages, insights}`, written by **Slumber**; a null is written as an
explicit `null`, never omitted. Both graph `data` objects also decode strictly (default kotlinx `Json`)
into their DTOs — pinned by the specs.

**New `kontainer` slice** (`collectors[].key == "kontainer"`, every record from now on):

```json
{
  "graphFingerprint": "7ef2c0349a48b0b81fa41681c1fb55999d30808ae303af6795fb0a85f8650d70",
  "services": [
    {
      "cls": "io.peekandpoke.ultra.security.user.UserProvider",
      "type": "DynamicOverride",
      "instances": [
        {
          "cls": "io.peekandpoke.ultra.security.user.UserProvider$Lazy",
          "createdAt": { "ts": 1792413528458, "timezone": "UTC", "human": "2026-10-19T12:38:48.458665961Z" }
        }
      ]
    }
  ]
}
```

- **The `graphFingerprint` KEY marks this shape (v2)** — present even when its value is `null`. The old
  shape (v1) has `info` instead; a slice with neither is unknown: show the raw tree.
- **No `numOld` / `numTotal`** (maintainer, 2026-10-19, T1): they counted tracked kontainers, tracking
  (`KontainerBlueprint.Config.trackKontainers`) cannot be switched on from outside `ultra:kontainer`, so they
  were always 0. Exactly two keys; pinned by slumbering the slice (`KontainerCollectorSlumberSpec`,
  `KontainerCollectorFinishSpec`) and on the wire (`InsightsKontainerSliceSpec`). v1 records keep theirs.
- `graphFingerprint`: 64 lowercase hex, or `null` if the graph could not be built.
- `services`: ONLY services with at least one instance (74 of 179 here); order not significant; `instances`
  is never empty. Exactly the keys `cls`, `type`, `instances` — no definition.
- **Not all of it is the request's own — `type` tells.** A `Singleton` entry is process state, shared by
  every request (created by the boot or by whichever request first needed it); every other type
  (`SemiDynamic`, `Dynamic`, `Prototype`, `DynamicOverride`) is this kontainer's own. `createdAt` only says
  whether this request was the one that created the instance.
- Every class name is a **flat string**; the old shape wrapped them as `{"fqn": …}`.
- `createdAt` is Slumber's instant object, `ts` in epoch **millis** — `readInstantMillis` reads it as is.
- `type` is the provider type **in this request's kontainer**: `Singleton`, `SemiDynamic`, `Dynamic`,
  `Prototype` or `DynamicOverride`. `DynamicOverride` (here `UserProvider`, `InsightsConfig`) means the
  instance was handed in when the kontainer was created; the graph lists that service as `Dynamic` with
  its blueprint definition, whose injects did NOT run in this request.
- Join on `cls` against the graph's `services[].cls`. "Injects" and "Definition & overwrites" exist only
  in the graph. If `graphFingerprint` differs from the graph's `fingerprint`, the record was written
  against another graph: say so, rather than draw today's edges as if they were its own. **A mismatch is
  expected after almost any deploy**: the fingerprint covers every code location, line numbers included,
  so moving one registration line changes it even when the topology did not change.
- **The record's own top-level `ts` is wrong by the server's UTC offset — do not use it.** It is a
  `LocalDateTime` (`InsightsFull.kt:37`) that Slumber writes as if it were UTC: the frozen `FAT_RECORD`
  says `"human": "2026-10-19T14:21:28…Z[UTC]"` while its instances were created at `12:21:28Z`, on a CEST
  machine. Use the API's `recordedAt` (the depot file's time, `InsightsModels.kt:41`).

**Old `kontainer` slice** (records written before 2026-10-19; still on disk, served unchanged). A trimmed
real one is frozen as `FAT_RECORD` in `InsightsKontainerSliceSpec`:

```json
{
  "numOld": 0,
  "numTotal": 0,
  "info": {
    "services": [
      {
        "cls": { "fqn": "io.peekandpoke.funktor.insights.InsightsRepository" },
        "type": "Dynamic",
        "definition": {
          "creates": { "fqn": "io.peekandpoke.funktor.insights.InsightsFileRepository" },
          "injectionType": "Dynamic",
          "injects": [ { "name": "kontainer", "provisionType": "Direct", "classes": [ { "fqn": "io.peekandpoke.ultra.kontainer.Kontainer" } ] } ],
          "codeLocation": { "location": "io.peekandpoke.funktor.Index_testJvmKt.createBlueprint$lambda$0(index_testJvm.kt:94)" },
          "overwrites": { "creates": { "fqn": "…" }, "injectionType": "Dynamic", "injects": [], "codeLocation": { "location": "…" }, "overwrites": null }
        },
        "instances": [ { "createdAt": { "ts": 1792412488864, "timezone": "UTC", "human": "2026-10-19T12:21:28.864064539Z" }, "cls": { "fqn": "…" } } ]
      }
    ]
  }
}
```

(The `injects` entry is `KontainerCollector`'s, spliced in for illustration; `InsightsRepository`'s is `[]`.)
ALL services (178 here), `instances` may be `[]`. A `DynamicOverride` entry carries the override's own
definition: empty `injects`, `codeLocation` inside `ServiceProviderFactory.<init>`, the blueprint one under
`overwrites`. Records from before 2026-07-31 (Jackson) wrote `createdAt` as epoch SECONDS (a double) — per
`slices.ts`, not observed here.

**v1 → v2, what a reader of both must translate** — every `{"fqn": …}` became a plain string (`cls`,
`creates`, `classes[]`, instance `cls`); `codeLocation` went from `{"location": …}` to a plain string;
definitions moved from the record to the graph; `info.services` (all services) became `services` (only
those with instances).

**`GET /_/funktor/insights/graphs/kontainer`** → `data` (93 KB compact, 179 services; computed once per
blueprint):

```json
{
  "fingerprint": "7ef2c0349a48b0b81fa41681c1fb55999d30808ae303af6795fb0a85f8650d70",
  "services": [
    {
      "cls": "io.peekandpoke.funktor.insights.collectors.KontainerCollector",
      "type": "Dynamic",
      "definition": {
        "creates": "io.peekandpoke.funktor.insights.collectors.KontainerCollector",
        "injectionType": "Dynamic",
        "injects": [
          { "name": "kontainer", "provisionType": "Direct", "classes": [ "io.peekandpoke.ultra.kontainer.Kontainer" ] }
        ],
        "codeLocation": "io.peekandpoke.funktor.insights.Insights_moduleKt.Funktor_Insights$lambda$0(insights_module.kt:71)",
        "overwrites": null
      }
    }
  ]
}
```

- **Fetch it at most once per page lifetime.** It changes only with the deployed build; cache it, keyed by
  `fingerprint` if anything.
- `services` holds EVERY service, sorted by `cls`. `type`: `Singleton`, `SemiDynamic`, `Dynamic` or
  `Prototype` — never `DynamicOverride`: the graph is read from a kontainer without overrides, so it does
  not depend on who asks.
- `injectionType`: `Singleton`, `Dynamic` or `Prototype` as registered — upgraded to `Dynamic` or
  `Prototype` when the definition replaces one of that type (`KontainerBuilder.withTypeUpgrade`). The
  downgrade to `SemiDynamic` shows only in `type`.
- `injects` in constructor order; `provisionType`: `Direct` or `Lazy`. `classes` (sorted) is what the
  parameter ASKS for: for a single service, its **declared** type, which need not be a service key —
  `AppConfigCollector`'s `appConfig` asks for `io.peekandpoke.funktor.core.config.AppConfig`, while the
  key is the app's concrete config class (`io.peekandpoke.funktor.FunktorAllTestConfig` here). Resolve such
  an edge by supertype, or draw it dangling. For a `List`/`Lookup` parameter it is the candidate keys.
- `overwrites`: `null` or another object with the same five keys — a chain. All strings are flat.

**`GET /_/funktor/insights/graphs/database`** → `data` (4.5 KB for 13 repos; built per request):

```json
{
  "repos": [
    {
      "id": { "name": "insights_graph_e2e_books", "connection": "MongoDB(8.2.1)::ultra_wt_insights_backend" },
      "storedClasses": [
        {
          "fqn": "io.peekandpoke.funktor.GraphE2eBook",
          "references": [
            { "repo": { "name": "insights_graph_e2e_authors", "connection": "ArangoDB(3.12.6)::ultra_wt_insights_backend" }, "fqn": "io.peekandpoke.funktor.GraphE2eAuthor" },
            { "repo": null, "fqn": "io.peekandpoke.funktor.GraphE2eUnstored" }
          ]
        }
      ]
    }
  ]
}
```

(Re-observed after the MINOR batch.) `DatabaseGraphModel` WITHOUT its two dead fields — no repo-level
`connection` (it duplicated `id.connection`), no reference `type` (always `Direct`; the old renderer's
dashed "Lazy" edges can no longer occur). `id.connection` is `ArangoDB(<version>)::<db>` or
`MongoDB(<version>)::<db>`; a reference may cross backends; `repo: null` = no repository stores the type.
`repos` is `[]` for an app without repositories. Built per request.

**Refusal** — anonymous or non-superuser, both graphs: `401` with `data: null` and messages
`"Access to the resource 'GET /_/funktor/insights/graphs/kontainer' is not authorized."` plus, outside
production only, `"Failed auth rules: Is SuperUser"`. Identical, apart from the URI, to the record endpoints'.

**The Vue `KontainerTab` BEFORE T1 on each shape** — checked read-only (T2, 2026-10-19) by running
`readKontainer` from a copy of `slices.ts` under `node --experimental-strip-types`: the slim slice read as
`null` (raw tree only, degraded but not misread), the fat one fully. T1 since reads both
(`slices.ts:586`). Found in passing then: the tab said "15 minutes" for an "old" kontainer, the threshold
was 15 SECONDS. Moot now: the slim slice has no `numOld`/`numTotal` and `OLD_AFTER_SECONDS` is deleted
(`KontainerCollector.kt:67`).

## Implementation notes (T2, 2026-10-19)

Line numbers as of the MINOR batch (2026-10-19, evening).

- `funktor/insights/src/jvmMain/kotlin/api/InsightsGraphModels.kt` — the two wire DTOs. Plain strings for
  every enum, no JVM-only type, so T4 can move the file to commonMain as is.
- `funktor/insights/src/jvmMain/kotlin/InsightsGraphs.kt:19` — builds the kontainer graph; a true singleton
  (`insights_module.kt:82`) injecting only the blueprint, itself a singleton. `kontainerGraph` (`:37`) is a
  `lazy`, read from `blueprint.create()` — a kontainer WITHOUT dynamic overrides — and sorted (`:40`);
  `fingerprintOf` (`:23`) is SHA-256 over the services' JSON. The database mapping is a pure extension,
  `DatabaseGraphModel.toInsightsDatabaseGraph()` (`:72`), outside the DTO file so that stays commonMain-ready.
- `funktor/insights/src/jvmMain/kotlin/api/InsightsApi.kt:43,49` declare, `:96,106` mount, with
  `.noInsights()` and under the group's `isSuperUser()` floor (`:25`). The database handler uses
  `kontainer.get(DatabaseGraphBuilder::class)` (`:113`): funktor core always registers it. The path layer
  in `routing.kt` already keeps everything under `InsightsApi.base` out of the depot.
- `funktor/insights/src/jvmMain/kotlin/collectors/KontainerCollector.kt:79` — the slim `Data`; `of`
  (`:110`) reads `factory.getAllProviders()` and keeps those with instances (`:120`) instead of
  `getDebugInfo()`, which would build all definitions only to drop them. Names go through
  `DebugInfo.ClassInfo`, so the join key is exactly the graph's. `finish` (`:120`) wires two sources, the
  graph's fingerprint and the providers, into `collect` (`:42`), which never throws — a collector that throws
  drops the whole record — with two separate tries (`:46`, `:53`). (Lines as of 2026-10-19, after T1 dropped
  `numOld`/`numTotal`.)
- **Database graph is NOT backend-independent**, so it is e2e'd on both: each repository's `connection` is
  read from its driver, which asks the SERVER (Arango `version`, Mongo `buildInfo`, `runBlocking`, once
  per driver instance — on the request thread, as the drivers already do for profiling).

## Deviations from the plan (T2) — for the gate to accept or reject

- **`fingerprint` / `graphFingerprint` were not in the plan.** They replace the ETag/boot-id idea and make a
  join across builds detectable instead of silently wrong.
- **The slice keeps `type` per service**, beyond "instances + FQN": without it `DynamicOverride` cannot be
  recovered from anything static.
- **The graph is read from an override-free kontainer**, not from the asking request's — so it never shows
  `DynamicOverride` and always the blueprint definition (pinned e2e, mutation G1).
- **`createdAt` stays Slumber's `{ts, timezone, human}`**, not a bare number: a bare number is exactly what
  today's reader misreads as seconds (D2).
- **The database DTO drops the model's two dead fields** (`Repo.connection`, `Reference.type`) — maintainer,
  MINOR batch, before T4 publishes the DTO. Not a field-for-field mirror any more.
- Sizes are measured on funktor/all's app, not the demo the 73.6% came from.

## Record size — the same request on the same app, before and after (2026-10-19)

`GET /_/funktor/introspection/lifecycle-hooks` as a superuser on the `insights.*` host — the request
`InsightsRecordingSpec` records — on funktor/all's test app (178 services; the 137.8 KB / 73.6% above was
the demo). Pretty JSON is what the writer puts on disk.

| | before | after | change |
|---|---|---|---|
| record on disk (pretty) | 409 316 B | 105 969 B | −74.1% |
| kontainer slice's share of it | 355 009 B (86.7%) | 51 661 B (48.8%) | −85.4% |
| record, compact | 165 101 B | 68 826 B | −58.3% |
| kontainer slice, compact | 115 737 B (70.1%) | 19 461 B (28.3%) | −83.2% |
| services / instances in the slice | 178 / 77 | 74 / 78 | |

One record per side (`records-2026-10-19/2026-10-19T14:21:28.864097350.json` before,
`…T14:42:39.445051398.json` after); a second after-run gave 105 974 B, so a few bytes of jitter. The rest
of the record is now dominated by `runtime` (39.9 KB compact, the system properties) and by the
pretty-printing itself (106 KB on disk for 69 KB of JSON). Neither is this task's; both are worth a look.

## Test evidence (T2, 2026-10-19)

Counts from `build/test-results/**/TEST-*.xml`, all failures 0:

- `:funktor:insights:jvmTest` — after the MINOR batch 13 specs, 95 tests (was 12 / 91). New:
  `KontainerCollectorSlumberSpec` 6 (the slice through `InsightsCodec`, the writer's own codec, and through
  the record envelope), `InsightsGraphsSpec` 8, `KontainerCollectorFinishSpec` 5 (`finish` through a real
  `ApplicationCall`, both guards, the counters). Changed: `InsightsApiRoutesSpec`, `InsightsLevelSpec`
  (four routes).
- `:funktor:all:jvmTest` — 15 specs, 178 tests, before and after the batch. New: `InsightsGraphsApiSpec`
  11, `InsightsKontainerSliceSpec` 5, `InsightsDatabaseGraphBackendsSpec` 4 (one app, an Arango and a Mongo
  repository, a reference between them). `InsightsWireSamples.kt` is the one shared opt-in dumper.
- **Full suite** (`console/wt-gradle.sh --full-suite`, 2026-10-19 15:24–15:33, 9m 40s): BUILD FAILED — 68
  targets, 924 spec files, 15 632 tests, 7 skipped, **16 failures**, 0 errors. insights 91/0 and
  funktor/all 178/0 inside it. All 16 failures are worktree infrastructure, not this change (the branch
  touches only `funktor/insights` and new `funktor/all` specs):
  - `:funktor-demo:server:test` 14/14 — `ConfigException$UnresolvedSubstitution: ${JWT_SIGNING_KEY}`
    (`application.test.conf:38`) at spec instantiation. The gitignored `keys.env.conf` (since `2397c0a6`)
    was missing when resources were processed; a symlink to the main worktree's file appeared at 15:29,
    mid-run, not created by T2. Re-run module-level afterwards: **7 specs, 47 tests, 0 failures.**
  - `:monko:core:test` 2/249 — `MongoDbConfigSpec` and `MonkoModuleSpec` pin `forUnitTests` to `test`,
    but in a worktree `ULTRA_TEST_MONGO_DB` (Wave 0, E2, `5fe625a8`) makes it `ultra_wt_insights_backend`.
    Fails in every worktree; needs E2 to reconcile those two specs.
- No Kotlin-daemon failure, OOM or non-daemon fallback in any run of this track.

## Mutation table (T2, 2026-10-19)

Each mutant was backed up with `cp` (to the session scratchpad, source-set path in the name), run, and
restored with `cp`, verified by `cmp` and a clean `git diff`. Failing cases read from the console, counts
from the XML.

| # | Mutant | Red in | Why that is the right reason |
|---|---|---|---|
| M1 | slice `Instance` also holds the live service object | `KontainerCollectorSlumberSpec` 6/6; `InsightsKontainerSliceSpec` 4/5; `InsightsRecordingSpec` 6/6 | "There is no known way to slumber the type …Handler"; e2e: "no NEW insights record appeared" — the D1 failure, whole record dropped |
| M2a | keep services WITHOUT instances | unit "only services with instances"; e2e wire shape | `Unused` present; `instances` empty |
| M2b | definitions back (`info = getDebugInfo()`) | unit four-keys; e2e wire shape + size guard | size guard: "the definitions are back in the record: 135929 compact bytes for 78 instances" |
| M3 | `createdAt` as bare epoch millis | e2e wire shape (unit cannot compile against it) | "Expected [ts, timezone, human] but actual was null" |
| M3c | epoch SECONDS stored as millis | e2e window; unit — **survived first**, test fixed | unit read its expected value off the slice under test; now from the kontainer: `expected 1792414792308 but was 1792414792` |
| M4 | class names as `{fqn}` objects | unit "flat strings" (+3 lookups); e2e wire shape, join | "is of type LinkedHashMap but expected kotlin.String" |
| M5 | record's `graphFingerprint` null | e2e join | `expected:<7ef2…> but was:<null>` |
| G1 | graph read from a kontainer WITH an override (e2e only: the fixture has no `UserProvider`) | `InsightsGraphsApiSpec` 2; slice join | `expected:<Dynamic> but was:<DynamicOverride>` |
| G2 | constant fingerprint | unit "changes when only a definition changes" | e2e stays green — correctly: a constant agrees between record and graph |
| G3 | fingerprint over class names only | same unit test | same-named graphs with different definitions collided |
| G4 | graph unsorted | unit order-independence; e2e "ordered by cls" | fingerprints differ; elements differ by index |
| G5 | graph rebuilt per access, not `lazy` | unit "built once" | identity check |
| G6 | `InsightsGraphs` injects a dynamic (e2e only: the new constructor breaks the unit compile) | e2e "true singleton" | `expected:<Singleton> but was:<SemiDynamic>` |
| G8 | repo id `name`/`connection` swapped | unit mirror; backends spec 3 | exact-model equality; lookup by name fails |
| G9 | reference loses its repo | unit mirror; backends cross-reference | "Expected Id(name=insights_graph_e2e_authors…) but actual was null" |
| S1 | floor `isSuperUser()` → `authenticated()` | graphs non-superuser ×2, same-refusal, backends non-superuser; old `InsightsApiSpec` ×2; `InsightsApiRoutesSpec` | a regular user got `200` with the graph; anonymous cases rightly stay green |
| S2 | graphs mounted on a separate `public()` group | every floor case incl. anonymous (5 + backends 1); routes/level specs | anonymous got `200` with the graph |
| O1 | loader drops a fat kontainer slice | old-record e2e | the slice is not served |
| O2 | loader strips `info` from a fat slice | old-record e2e | "nothing reshapes an old slice on the way out" |
| F1 | `collect`'s two tries merged into one (MINOR batch) | `KontainerCollectorFinishSpec`: graph failure, unreadable services | "one try around both would have dropped the request's services with the graph"; fingerprint `null` instead of kept |
| F2 | no guard around reading the services | unreadable-services test | the providers' exception escaped `collect` |
| F3 | `numOld`/`numTotal` swapped into each other's field | "own fields", "cannot exceed" | `expected:<5> but was:<2>` |
| F4 | `numTotal` constant 0 | same two | `expected:<5> but was:<0>` |
| F5 | `numOld` constant 0 | "own fields" | `expected:<2> but was:<0>` |
| F6 | `numOld` read before `numTotal` | "cannot exceed" | "numOld=5 numTotal=1: numTotal must be read first" |
| F7 | the swap moved into `finish`'s wiring | **survives** — expected, see the batch notes | both counters are 0 through `finish`; no public API gives a live tracker |
| D1 | `Reference.type` back on the database DTO | e2e database wire shape | reference keys `["repo", "fqn"]` violated; unit equality survives by construction (a default on both sides) |

Probed and CLEAN: the old record also lists through the list endpoint and its headline still reads; the API
serves the new slice exactly as the file holds it (JSON-equal, numbers intact).

## MINOR batch (T2, 2026-10-19 evening) — what changed, per ledger row

Maintainer: fix the open MINORs except "serve the graphs while insights is disabled". Rebased onto master
first. Commits `249eca0a` (wire + simplifications) and `7579868b` (collector + specs).

1. **Dead DTO fields** — `Repo.connection` and `Reference.type` dropped (`InsightsGraphModels.kt:61-100`);
   contract, `InsightsGraphsSpec`, `InsightsGraphsApiSpec`, `InsightsDatabaseGraphBackendsSpec` follow.
   Re-observed on a real response. Mutation D1.
2. **Wrong text** — `Injection.classes` KDoc (`InsightsGraphModels.kt:53-57`): the DECLARED type, e.g.
   `AppConfig`, which is no key (verified in the real graph: `AppConfigCollector.appConfig`).
   `injectionType` (`:34-38`): as registered, after the upgrade. `KontainerCollector.kt:15-24`: no longer
   "request-specific only". The contract's non-key example is now `AppConfig`, not `Kontainer`.
3. **Contract completeness** — v1→v2 translation (`{fqn}`/`{location}` → strings), refetch at most once per
   page, counters always 0, `graphFingerprint` key marks v2, the record's `ts` is off by the UTC offset
   (observed +2 h), a fingerprint mismatch is expected after almost any deploy. **No topology-only
   fingerprint:** it would call two graphs equal whose `codeLocation`s — shown in the Definition column —
   differ, a false "same" in exchange for a false "different"; and nothing yet needs to tell a
   line-move from a real change. Documented instead.
4. **`finish` and both guards** — `KontainerCollector.kt:47-68` `collect`, `finish` (`:134`) wires into it.
   `KontainerCollectorFinishSpec`: through a real `ApplicationCall` with a blueprint that fails validation
   (`KontainerFixture.brokenBlueprint`) → `graphFingerprint == null`, services kept; the unreadable-services
   branch through `collect`, since no real kontainer fails there. Mutations F1, F2.
5. **Counters** — read order restored, `numTotal` first (`:61`). **Pinning them with
   `trackKontainers = true` is impossible from here:** `KontainerBuilder.build(config)`
   (`ultra/kontainer/.../KontainerBuilder.kt:543`) and the `KontainerBlueprint` constructor are
   `internal`, and `kontainer { }` (`index.kt:19`) always uses the default config — no public API, no
   funktor code, enables tracking. So `collect` pins them with distinct values (F3–F6 die), while the swap
   moved into `finish`'s wiring survives (F7): both are 0 there. A maintainer call: make tracking
   enable-able in `ultra:kontainer`, or drop two fields that are always 0.
6. **Misnamed unit test** — "is read WITHOUT the request's overrides" deleted; its `creates` assertion moved
   into the listing test; the claim stays with `InsightsGraphsApiSpec` (mutation G1).
7. **Bytes** — `InsightsKontainerSliceSpec` measures `toByteArray().size` throughout. Not mutation-checkable
   here: on this ASCII-only machine chars and bytes agree.
8. **Record wait** — `Paged.fullItemCount` on a page of one, done when the count grew and the newest file
   parses; the old-record lookup walks every page (`allRecords`). The ≥200-record case was not reproduced.
9. **Simplifications** — `DatabaseGraphModel.toInsightsDatabaseGraph()` extension (`InsightsGraphs.kt:72`);
   `kontainer.get` replaces the 404 branch (`InsightsApi.kt:113`); no `Json` alias; one
   `insightsWireSample` (`funktor/all/src/jvmTest/kotlin/InsightsWireSamples.kt`).

Tests: `:funktor:insights:jvmTest` 13 specs / 95 tests, `:funktor:all:jvmTest` 15 / 178, all green. No
full suite for this batch (coordinator).

## T1 — implementation notes, Parts C+D (2026-10-19, branch `wt/insights-vue`)

All under `funktor/codegen/src/main/resources/ts/insights/` unless named otherwise.

- **Both slice shapes** — `slices.ts:565` `readKontainer`: the `graphFingerprint` KEY marks v2 (`slim`), `info`
  marks v1 (`full`), neither is unrecognised (raw tree). `readFqn` and `readDefinition` (`:531`) read both the
  `{fqn}`/`{location}` wrappers and flat strings. No `instanceCountsMeasured` any more: a full
  record's young/old/total row always says "not measured" (`KontainerTab.vue:111`). `isProcessWide` (`:598`): `type == Singleton` is process state, every other type the request's own
  (T2's final correction) — each row and the summary say which. `joinKontainerGraph` (`:622`) fills a slim
  record's definitions from the graph and keeps the RECORD's type (only the record knows `DynamicOverride`).
  `graphMatch` (`slices.ts:686`: `same` / `other` / `unknown`) makes the tab say whether the record joins the
  graph shown.
- **The graphs** — `graphs.ts`: `kontainerNetwork` (`:72`) and `databaseNetwork` (`:210`) port the old
  renderers' nodes, edges, colours and sizes; `networkOptions` (`:117`) their vis.js options. An injection
  whose DECLARED type is no service key is not drawn but counted (the old graph drew it to an id with no
  node, which vis.js drops silently); the note under the graph says how many. Database edges are never dashed:
  `Reference.type` is gone and was always `Direct`.
- **Lazy, cached for the page** — `createGraphSource` (`graphs.ts:352`) caches the kontainer graph by
  fingerprint (`byFingerprint`, `:325`, Part A's decision, done in the round-2 MINORs 2026-10-19): a record
  naming an uncached fingerprint triggers one refetch. The database graph is fetched once (`once`, `:313`).
  A failure is forgotten, nothing throws (`attempt`, `:301`: a non-envelope 404 from an older server is a
  `GraphResult`, not an exception). `InsightsPage.vue:59` provides ONE source for the page's lifetime;
  `InsightsDetailPage.vue:61` provides its own only when used standalone. Tabs inject it
  (`useInsightsGraphsOrNull`, `:382`); without one a slim record shows its table and says why there is no graph.
- **Drawing** — `NetworkGraph.vue:35` imports `vis-network/standalone` dynamically: one 652 KB chunk (158 KB
  gzip), downloaded on the first click. Standalone bundles its peers and injects its CSS into `<head>`. A
  failure to load or draw is shown inside the component and the tab keeps rendering (tested with the REAL
  library, which throws under happy-dom for want of a canvas).
- **Tabs** — `KontainerTab.vue:69` `toggle`; a full record draws from itself with no request. `VaultTab.vue:77`
  `loadGraph`, shared by "Show Database Graph" and opening "Database Graph Data" (`:95`).
- **The app's dependency** — `funktor/codegen/src/main/kotlin/cli/TsSdkGenerateCliCommand.kt:128`: generation
  refuses an SDK whose package imports `<out>/package.json` does not declare. `funktor-demo/sdkgen-app` declares
  `vis-network@10.1.2`.

### Deviations (T1) — for the gate

- **Retry after a failed fetch**, against the contract's "at most once per page": a success is kept for the
  page's lifetime, a failure is asked again on the next click (a 401 thus repeats per click).
- **No ETag/fingerprint refetch**: a record naming another fingerprint is drawn against today's graph with a
  notice, not refetched — "at most once" wins over "the server may have redeployed meanwhile".
- **The check, not an API**: no `out.requires` in `ultra:codegen`; see the vue-tabs task.

### Test evidence (T1)

Module counts are in `docs/tasks/20260802-insights-vue-tabs.md`, "Test evidence (T1)". The graph-specific
component tests: `vue-verify/tests/graphs.spec.ts` (17: lazy, once per page across two tabs, 401, HTML 404,
retry after failure, mismatch said and not said, the fat record drawing with no request, no source; edges,
dangling count, Lazy dashes, `DynamicOverride` colour; the database graph's fetch, raw view, 401, the
cross-backend edge), `graphs-real-library.spec.ts` (2), `detail-page.spec.ts` (ancestor's source vs own).

**Full suite** (`console/wt-gradle.sh --full-suite`, 2026-10-19 16:59-17:09, 8m 10s, on `18cef881` rebased onto `ea7bd99b`): BUILD SUCCESSFUL -- 69 targets, 929 spec files, 15 751 tests, 7 skipped, **0 failures, 0 errors** (`vueVerify` ran inside it: its 58 vitest cases are in that count).

### Mutation table (T1) — readers, graph source, rendering

Applied to the resource, copied into the harness SDK (emission is verbatim, `diff -r` before and after every
mutant), vitest run, restored with `cp` + `cmp`. 58 tests per run.

| # | Mutant | Red | Why that is the right reason |
|---|---|---|---|
| R1 | v2 told by `services[]`, not by the key | 2 | a slice with neither marker is guessed instead of shown raw |
| R2 | `codeLocation` only as a flat string | 3 | the old record's `{location}` vanishes from table and reader |
| R3 | class names only as `{fqn}` | 8 | every slim-record and graph class name is null: join, edges, colours |
| R4 | shapes swapped | 17 | every shape-dependent behaviour |
| R5 | the join takes the graph's type | 2 | `DynamicOverride` lost: drawn and listed as `Dynamic` |
| R6 | counts always shown | 2 | "0 / 0 / 0" instead of "not measured" |
| R7 | Jackson-era seconds not scaled | 1 | a 1970 date |
| R8 | process-wide told by having instances | 1 | every row "process-wide" |
| G1 | no caching | 1 | a second request on the same page |
| G2 | a failure cached | 1 | the retry click does not ask again |
| G3 | the kontainer graph fetched on mount | 3 | a request before any click; the fat record's no-request case |
| G4 | a thrown client error not contained | 1 | the HTML-404 case: the tab fails instead of saying so |
| G5 | a drawing failure escapes `NetworkGraph` | 2 | the real library's failure takes the tab down |
| G6 | dangling injection edges drawn | 1 | an edge with no target node |
| G7 | Lazy not dashed | 1 | |
| G8 | database edge reversed | 1 | the arrow points the old GUI's other way |
| G9 | mismatch never said | 1 | |
| G10 | the detail page ignores the ancestor's source | 1 | a second fetch per opened record |

### Round 1 fix batch (T1, 2026-10-19)

The `DynamicOverride` join (round 1's MAJOR), the three-way fingerprint match, the database graph's
"today's schema" note and the dropped `numOld`/`numTotal` are recorded, with `path:line`, evidence and mutants,
in `docs/tasks/20260802-insights-vue-tabs.md`, "T1 — round 1 fix batch".

### Red-team scenarios proposed (T1) — for ULTRA-00161 (redteam insights api)

Collected, not run; T1 may not edit that file. The rendering and S1 ones are in the vue-tabs task.

1. The graph endpoints' class names, code locations and repository names are rendered as canvas labels: confirm
   no `title`/tooltip path turns one into HTML (vis-network sets string titles via `innerText`; none are set).
2. A record crafted on disk (depot write access) with a huge `services` list or a deep `overwrites` chain:
   the reader bounds the chain at 16, not the list — measure what freezes the tab.

## Review record — the LEDGER, Parts A+B (T2), /feature-review 2026-10-19

Round 1 (blind, `reviewer-high` ×3: implementation, domain, security). **0 CRITICAL, 0 MAJOR → clean round.**
The MINORs below are handed to the maintainer as a list (review-loop termination rule), not applied.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR | `InsightsApi.kt:95-120`, `insights_module.kt:82,85` | Both graphs are served even when insights recording is disabled (a production app keeping `InsightsConfig.Disabled` still exposes ~180 definitions with code locations) | maintainer-decision | Joins the plan's "production gate for insights" row; options: 404 when disabled, or record that the graphs are superuser introspection independent of recording |
| 1 | MINOR | `InsightsGraphModels.kt:67-71`, `:90-95` | New DTO freezes dead/redundant fields (`Repo.connection` duplicating `Id.connection`, `Reference.type` always `Direct`) | FIXED in the maintainer-approved batch (`249eca0a`, `7579868b`) | Domain + security recommend dropping them now, before T4 publishes the DTO from commonMain; the implementer kept them for a verbatim port (deviation listed above) |
| 1 | MINOR | `InsightsGraphs.kt:24-27` | The fingerprint also hashes `codeLocation` line numbers, so nearly every pre-deploy record shows "another graph" | FIXED in the maintainer-approved batch (`249eca0a`, `7579868b`) | Trade-off: a second, topology-only fingerprint, or document "a mismatch is expected after any deploy touching a module file" |
| 1 | MINOR | `InsightsGraphModels.kt:50`, `:34`; `KontainerCollector.kt:16`; wire contract | KDoc/contract text factually wrong: `Injection.classes` is the DECLARED type (may be no service key); `injectionType` is after override upgrade; "request-specific" is false for global singletons; the contract's `Kontainer` example IS a service key | FIXED in the maintainer-approved batch (`249eca0a`, `7579868b`) | Factually-wrong-text findings; cheap, but they shape the T1/T4 readers — apply before T1 starts? |
| 1 | MINOR | wire contract | Missing contract statements: `codeLocation` went `{location}` → string; refetch at most once per page; `numOld`/`numTotal` are always 0 (tracking never enabled); presence of `graphFingerprint` marks the v2 shape; the record's own `ts` is a `LocalDateTime` encoded as UTC (`InsightsFull.kt:37`, already worked around at `InsightsModels.kt:37`) — readers must not use it | FIXED in the maintainer-approved batch (`249eca0a`, `7579868b`) | Contract completeness for T1/T4 |
| 1 | MINOR | `KontainerCollector.kt:97-118` | `finish` and both catch branches are untested; merging the two tries back would lose the whole record on a graph failure (the D1 mode) | FIXED in the maintainer-approved batch (`249eca0a`, `7579868b`) | A unit test with a throwing graph |
| 1 | MINOR | `KontainerCollectorSlumberSpec.kt:38-39`, `InsightsKontainerSliceSpec.kt:193` | `numOld`/`numTotal` pinned by nothing (swap or constant-0 mutants survive); evaluation order changed (`:110-111`, numOld read first) | fixed in part | Test with `trackKontainers = true`; restore the read order | — PARTLY: read order restored and both values pinned through `collect`; a tracker-enabled blueprint cannot be built from outside `ultra:kontainer`, so "make tracking switchable vs drop the always-0 fields" is a NEW maintainer decision
| 1 | MINOR | `InsightsGraphsSpec.kt:46-62` | "read WITHOUT the request's overrides" cannot fail for the reason in its name | FIXED in the maintainer-approved batch (`249eca0a`, `7579868b`) | Rename, or leave the claim to `InsightsGraphsApiSpec.kt:190` |
| 1 | MINOR | `InsightsKontainerSliceSpec.kt:177-179` | Compares UTF-16 chars with file bytes: fails on a machine with a non-ASCII path or user name | FIXED in the maintainer-approved batch (`249eca0a`, `7579868b`) | `toByteArray().size` |
| 1 | MINOR | `InsightsKontainerSliceSpec.kt:61,73-95,281` | The record wait counts one page of 200; once the JVM has ≥200 records the spec fails with a misleading message | FIXED in the maintainer-approved batch (`249eca0a`, `7579868b`) | Compare `Paged.fullItemCount` |
| 1 | MINOR (surplus) | `InsightsGraphs.kt:47`, `InsightsApi.kt:113-118`, `InsightsGraphs.kt:22`, the three sample-dump hooks | `databaseGraph(model)` is a pure mapper on a singleton; an untested 404 branch for a case funktor never produces; a `Json` alias; three copies of a test-only dumper | FIXED in the maintainer-approved batch (`249eca0a`, `7579868b`) | Small simplifications |

**Probed and CLEAN in round 1:**
- the `isSuperUser()` floor is structural (`ApiRoutes.addRoute`), and S1/S2 mutants go red;
- the graph endpoints are not recorded (`.noInsights()` plus the path prefix);
- connection strings carry no credentials or hosts;
- no config values in the service graph (`forInstance` records the class only);
- `blueprint.create()` instantiates no services and has no side effects; `InsightsGraphs` is a true singleton, built once;
- record and graph join on the same key;
- old and new slice shapes are disjoint; old records are served unchanged;
- the fingerprint is deterministic;
- no injection or unsafe deserialization;
- the DoS cost is bounded and superuser-only (cheaper than `VaultApi.listRepositories`);
- code style clean (the wildcard imports in `insights_module.kt` and `InsightsFull.kt` predate this change).

**Red-team:** scenarios 24-31 added to ULTRA-00161 (redteam insights api) on master (`e20fd6dc`).

**Verdict:** gate PASS for Parts A+B (round 1 clean; e2e present on both backends and green). Parts C+D stay open for T1.

**Batch review (one `reviewer-high`, the maintainer-approved MINOR batch, 2026-10-19): 0 CRITICAL, 0 MAJOR, 3 MINOR — all fixed by the coordinator.**

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| batch | MINOR | `KontainerCollector.kt:18-19`, contract | Names `createdAt` as what tells request state from process state; it is `type` (`Singleton` is shared, `ServiceProvider.kt:39-46`; every other type is per kontainer, `:48-56`) | fix | KDoc and contract corrected |
| batch | MINOR | `KontainerCollectorFinishSpec.kt` | The v2 marker's null case ("key present even when null") is serialized by no test | fix | The broken-graph case now slumbers its data and asserts the key is present and null |
| batch | MINOR | this file | Ledger rows not re-dispositioned, stale Status line, `InsightsApi.kt:24` → `:25` | fix | Done in this edit |

**Verified clean by the batch review:** every listed fix landed (re-read in the current files); the `collect`/`finish` split keeps
two independent guards and `finish` cannot throw; the counters are structurally 0 (`DummyTracker`); the record-`ts` offset and
`codeLocation`-as-string claims hold; nothing outside the diff references the removed fields; no surplus.
