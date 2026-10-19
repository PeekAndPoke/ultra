# Insights — the three graphs, and moving static data off every record

**Status:** IN PROGRESS — Parts A+B DONE 2026-10-19 (track T2, branch `wt/insights-backend`), review gate
not yet run; Parts C+D are track T1's. Created 2026-08-24 from maintainer decisions on
`.claude/tasks/20260802-insights-vue-tabs.md`'s gap inventory.
**Plan:** `.claude/tasks/20260730-frontend-sdk-vue-contributors.md`
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

- [x] `KontainerCollector.Data` keeps `numOld` / `numTotal` and **only the instantiated services** —
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
  - [ ] **(T1)** the tab tells the two shapes apart and says which one it shows.
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

- [ ] **Verify the peer install rather than assuming it.** `auto-install-peers` defaults to true in
      pnpm 8+, and the demo app is on `pnpm@10.26.2` with no `.npmrc` overriding it — so this probably
      just works. Probably is not a good enough basis for a shipped component: install it in
      `sdkgen-app` and check `vite build` before committing to the choice.
- [ ] **The generator cannot install it.** A component that imports `vis-network` needs the dependency in
      the APP's `package.json` — a file outside `<out>/`, which the ownership rule forbids the generator
      to write. This is exactly the `out.requires` case: emit the requirement and verify it, failing with
      the line to paste. Coordinate with the codegen agent; do not silently ship an import that only
      works in the demo app.

**If the peer-dependency surface turns out to be a problem**, the fallback is worth knowing: the
kontainer graph is a force-directed layout of a few hundred nodes, which is a few hundred lines of SVG
and `d3-force`-style physics. That is a real cost, but it is bounded, and it removes seven packages from
every consuming app. Do not take this branch without measuring the first.

## Part D — the tabs

- [ ] `KontainerTab`: two buttons, "Show instances Graph" and "Show Full Graph", differing only in
      whether services with no instances are included. Node label `simpleName\ntype`, size = injected
      class count, colour by type (Singleton `#1E90FF`, Prototype `#FFD700`, Dynamic `#8B0000`,
      SemiDynamic `#9400D3`, DynamicOverride `#DC143C`); edge per injection, arrow at the target,
      **dashed when `provisionType == Lazy`**, weight 2 Direct / 1 Lazy.
- [ ] `VaultTab`: the "Show Database Graph" button and its network, plus the old "Database Graph Data"
      `<details>` holding the raw model.
- [ ] **Both fetch lazily, on first click.** The old GUI did the same (`setTimeout(..., 500)` after
      revealing the container) because a hidden canvas cannot be laid out. More importantly here: the
      static graph is a second HTTP request, and a user who never opens the tab should never make it.
- [ ] Both tabs must still render **without** the graph — the endpoint can 403 for a non-superuser or
      fail outright, and a broken graph must not take the tab with it.

## Verification

- [x] e2e for both endpoints, including that a non-superuser gets the floor's answer, not the graph.
      DONE 2026-10-19: `InsightsGraphsApiSpec`, `InsightsDatabaseGraphBackendsSpec`.
- [ ] `vite build`, not just `vue-tsc` — a new dependency and new imports are exactly what a typecheck
      cannot confirm (see the `shims-vue.d.ts` note in the insights task).
- [x] Record size before and after Part B, measured on a real record from `InsightsRecordingSpec`.
      DONE — the same request, recorded by `InsightsKontainerSliceSpec`.
- [ ] An OLD (fat) record still renders the kontainer tab.
  - [x] Backend half: an old record still LOADS through the API, its slice untouched
        (`InsightsKontainerSliceSpec`, frozen real fat record). DONE 2026-10-19.
  - [ ] **(T1)** the tab renders it.

## Red-team follow-up

Extend `.claude/tasks/20260730-redteam-insights-api.md` rather than opening a new file: the two new
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
  "numOld": 0,
  "numTotal": 0,
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

- `numOld`, `numTotal`: int. `graphFingerprint`: 64 lowercase hex, or `null` if the graph could not be built.
- `services`: ONLY services with at least one instance (74 of 179 here); order not significant; `instances`
  is never empty. Exactly the keys `cls`, `type`, `instances` — no definition.
- Every class name is a **flat string**; the old shape wrapped them as `{"fqn": …}`.
- `createdAt` is Slumber's instant object, `ts` in epoch **millis** — `readInstantMillis` reads it as is.
- `type` is the provider type **in this request's kontainer**: `Singleton`, `SemiDynamic`, `Dynamic`,
  `Prototype` or `DynamicOverride`. `DynamicOverride` (here `UserProvider`, `InsightsConfig`) means the
  instance was handed in when the kontainer was created; the graph lists that service as `Dynamic` with
  its blueprint definition, whose injects did NOT run in this request.
- Join on `cls` against the graph's `services[].cls`. "Injects" and "Definition & overwrites" exist only
  in the graph. If `graphFingerprint` differs from the graph's `fingerprint`, the record was written
  against another graph (another build): say so rather than draw today's edges as if they were its own.

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
`overwrites`. **Telling them apart:** `info` is an object → old; `services` is an array at the top level →
new; neither → unknown, show the raw tree. Records from before 2026-07-31 (Jackson) wrote `createdAt` as
epoch SECONDS (a double) — per `slices.ts`, not observed here.

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

- `services` holds EVERY service, sorted by `cls`. `type`: `Singleton`, `SemiDynamic`, `Dynamic` or
  `Prototype` — never `DynamicOverride`: the graph is read from a kontainer without overrides, so it does
  not depend on who asks. `injectionType`: `Singleton`, `Dynamic` or `Prototype` (as declared).
- `injects` in constructor order; `classes` are sorted strings and may name classes that are not
  service keys (e.g. `Kontainer`). `provisionType`: `Direct` or `Lazy`.
- `overwrites`: `null` or another object with the same five keys — a chain. All strings are flat.

**`GET /_/funktor/insights/graphs/database`** → `data` (4.5 KB for 13 repos; built per request):

```json
{
  "repos": [
    {
      "id": { "name": "insights_graph_e2e_books", "connection": "MongoDB(8.2.1)::ultra_wt_insights_backend" },
      "connection": "MongoDB(8.2.1)::ultra_wt_insights_backend",
      "storedClasses": [
        {
          "fqn": "io.peekandpoke.funktor.GraphE2eBook",
          "references": [
            { "repo": { "name": "insights_graph_e2e_authors", "connection": "ArangoDB(3.12.6)::ultra_wt_insights_backend" }, "fqn": "io.peekandpoke.funktor.GraphE2eAuthor", "type": "Direct" },
            { "repo": null, "fqn": "io.peekandpoke.funktor.GraphE2eUnstored", "type": "Direct" }
          ]
        }
      ]
    }
  ]
}
```

- Field for field `DatabaseGraphModel`. `connection` = `id.connection`, `ArangoDB(<version>)::<db>` or
  `MongoDB(<version>)::<db>`; a reference may cross backends; `repo: null` = no repository stores the
  type; `type` is always `Direct`. `repos` is `[]` for an app without repositories. 404 only for a kontainer
  without a `DatabaseGraphBuilder` — funktor core always registers one.

**Refusal** — anonymous or non-superuser, both graphs: `401` with `data: null` and messages
`"Access to the resource 'GET /_/funktor/insights/graphs/kontainer' is not authorized."` plus, outside
production only, `"Failed auth rules: Is SuperUser"`. Identical, apart from the URI, to the record endpoints'.

**Today's Vue `KontainerTab` on each shape** — checked read-only by RUNNING `readKontainer` from a copy of
`slices.ts` under `node --experimental-strip-types` against the real slices:

- new slice: `readKontainer` → `null`, no throw. The tab shows only `<JsonTree :expand-depth="2">` — top
  level and the `services` array open, the 74 entries collapsed. No facts, no table, no "Raw slice"
  details, nothing saying which shape it got. Degraded, readable, not misread.
- old slice: reads fully, as before (178 services, `createdAt` correct through `{ts}`).
- Found in passing: `KontainerTab.vue` says "Old is alive longer than 15 minutes". The threshold is 15
  SECONDS — `KontainerTracker.getAlive(olderThanSec)` compares epoch seconds; `OLD_AFTER_SECONDS` in
  `KontainerCollector.kt`.

## Implementation notes (T2, 2026-10-19)

- `funktor/insights/src/jvmMain/kotlin/api/InsightsGraphModels.kt` — the two wire DTOs. Plain strings for
  every enum, no JVM-only type, so T4 can move the file to commonMain as is.
- `funktor/insights/src/jvmMain/kotlin/InsightsGraphs.kt:19` — builds both graphs; a true singleton
  (`insights_module.kt:82`) injecting only the blueprint, itself a singleton. `kontainerGraph` (`:38`) is a
  `lazy`, read from `blueprint.create()` — a kontainer WITHOUT dynamic overrides — and sorted (`:41`);
  `fingerprintOf` (`:24`) is SHA-256 over the services' JSON. `databaseGraph` (`:47`) maps the model.
- `funktor/insights/src/jvmMain/kotlin/api/InsightsApi.kt:42,48` declare, `:95,105` mount, with
  `.noInsights()` and under the group's `isSuperUser()` floor (`:24`). The path layer in `routing.kt`
  already keeps everything under `InsightsApi.base` out of the depot.
- `funktor/insights/src/jvmMain/kotlin/collectors/KontainerCollector.kt:42` — the slim `Data`; `of` (`:73`)
  reads `factory.getAllProviders()` and keeps those with instances (`:83`) instead of `getDebugInfo()`,
  which would build all definitions only to drop them. Names go through `DebugInfo.ClassInfo`, so the join
  key is exactly the graph's. The fingerprint is fetched separately (`:101`): a graph that cannot be
  built leaves `null` there and the request's services still in the record.
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
- **The database DTO keeps the model's redundancies** (`connection` twice, the obsolete `Reference.type`)
  so the old renderer ports verbatim.
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

- `:funktor:insights:jvmTest` — 12 specs, 91 tests. New: `KontainerCollectorSlumberSpec` 6 (the slice
  through `InsightsCodec`, the writer's own codec, and through the record envelope), `InsightsGraphsSpec`
  9. Changed: `InsightsApiRoutesSpec`, `InsightsLevelSpec` (four routes).
- `:funktor:all:jvmTest` — 15 specs, 178 tests. New: `InsightsGraphsApiSpec` 11,
  `InsightsKontainerSliceSpec` 5, `InsightsDatabaseGraphBackendsSpec` 4 (one app, an Arango and a Mongo
  repository, a reference between them).
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

Probed and CLEAN: the old record also lists through `listRecords` and its headline still reads; the API
serves the new slice exactly as the file holds it (JSON-equal, numbers intact).

## Review record — the LEDGER, Parts A+B (T2), /feature-review 2026-10-19

Round 1 (blind, `reviewer-high` ×3: implementation, domain, security). **0 CRITICAL, 0 MAJOR → clean round.**
The MINORs below are handed to the maintainer as a list (review-loop termination rule), not applied.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR | `InsightsApi.kt:95-120`, `insights_module.kt:82,85` | Both graphs are served even when insights recording is disabled (a production app keeping `InsightsConfig.Disabled` still exposes ~180 definitions with code locations) | maintainer-decision | Joins the plan's "production gate for insights" row; options: 404 when disabled, or record that the graphs are superuser introspection independent of recording |
| 1 | MINOR | `InsightsGraphModels.kt:67-71`, `:90-95` | New DTO freezes dead/redundant fields (`Repo.connection` duplicating `Id.connection`, `Reference.type` always `Direct`) | maintainer-decision | Domain + security recommend dropping them now, before T4 publishes the DTO from commonMain; the implementer kept them for a verbatim port (deviation listed above) |
| 1 | MINOR | `InsightsGraphs.kt:24-27` | The fingerprint also hashes `codeLocation` line numbers, so nearly every pre-deploy record shows "another graph" | maintainer-decision | Trade-off: a second, topology-only fingerprint, or document "a mismatch is expected after any deploy touching a module file" |
| 1 | MINOR | `InsightsGraphModels.kt:50`, `:34`; `KontainerCollector.kt:16`; wire contract | KDoc/contract text factually wrong: `Injection.classes` is the DECLARED type (may be no service key); `injectionType` is after override upgrade; "request-specific" is false for global singletons; the contract's `Kontainer` example IS a service key | maintainer-decision | Factually-wrong-text findings; cheap, but they shape the T1/T4 readers — apply before T1 starts? |
| 1 | MINOR | wire contract | Missing contract statements: `codeLocation` went `{location}` → string; refetch at most once per page; `numOld`/`numTotal` are always 0 (tracking never enabled); presence of `graphFingerprint` marks the v2 shape; the record's own `ts` is a `LocalDateTime` encoded as UTC (`InsightsFull.kt:37`, already worked around at `InsightsModels.kt:37`) — readers must not use it | maintainer-decision | Contract completeness for T1/T4 |
| 1 | MINOR | `KontainerCollector.kt:97-118` | `finish` and both catch branches are untested; merging the two tries back would lose the whole record on a graph failure (the D1 mode) | maintainer-decision | A unit test with a throwing graph |
| 1 | MINOR | `KontainerCollectorSlumberSpec.kt:38-39`, `InsightsKontainerSliceSpec.kt:193` | `numOld`/`numTotal` pinned by nothing (swap or constant-0 mutants survive); evaluation order changed (`:110-111`, numOld read first) | maintainer-decision | Test with `trackKontainers = true`; restore the read order |
| 1 | MINOR | `InsightsGraphsSpec.kt:46-62` | "read WITHOUT the request's overrides" cannot fail for the reason in its name | maintainer-decision | Rename, or leave the claim to `InsightsGraphsApiSpec.kt:190` |
| 1 | MINOR | `InsightsKontainerSliceSpec.kt:177-179` | Compares UTF-16 chars with file bytes: fails on a machine with a non-ASCII path or user name | maintainer-decision | `toByteArray().size` |
| 1 | MINOR | `InsightsKontainerSliceSpec.kt:61,73-95,281` | The record wait counts one page of 200; once the JVM has ≥200 records the spec fails with a misleading message | maintainer-decision | Compare `Paged.fullItemCount` |
| 1 | MINOR (surplus) | `InsightsGraphs.kt:47`, `InsightsApi.kt:113-118`, `InsightsGraphs.kt:22`, the three sample-dump hooks | `databaseGraph(model)` is a pure mapper on a singleton; an untested 404 branch for a case funktor never produces; a `Json` alias; three copies of a test-only dumper | maintainer-decision | Small simplifications |

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

**Red-team:** scenarios 24-31 added to `.claude/tasks/20260730-redteam-insights-api.md` on master (`e20fd6dc`).

**Verdict:** gate PASS for Parts A+B (round 1 clean; e2e present on both backends and green). Parts C+D stay open for T1.
