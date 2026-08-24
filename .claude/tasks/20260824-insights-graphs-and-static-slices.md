# Insights — the three graphs, and moving static data off every record

**Status:** TODO — created 2026-08-24 from maintainer decisions on
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

- [ ] **`GET /_/funktor/insights/graphs/kontainer`** — the full service definition graph. Static per
      boot, so it is cacheable client-side for the lifetime of the page.
- [ ] **`GET /_/funktor/insights/graphs/database`** — `DatabaseGraphModel` from `DatabaseGraphBuilder`.
      This is what makes the vault graph possible at all.
- [ ] **Both go on `InsightsApi`**, which already floors at `isSuperUser()` for the whole group. Do not
      mount them anywhere else: a service graph names every class in the app, and the database graph
      names every repository and reference. Neither is safe below that floor.
- [ ] **Consider an ETag or a boot id.** Both are static per boot, so a client that has one need not
      refetch. Cheap, and it is the difference between one fetch and one per page view.

## Part B — slim the kontainer slice

- [ ] `KontainerCollector.Data` keeps `numOld` / `numTotal` and **only the instantiated services** —
      the `instances` list with its `createdAt` values, plus enough identity (the service FQN) to join
      against the static graph.
- [ ] **Drop `info.services`' definitions from the record.** That is the 73.6%.
- [ ] **This is a WIRE BREAK for existing records.** Old records carry the fat shape and will still be
      read by the frontend. `slices.ts`'s readers already narrow rather than cast, so an old record
      degrades rather than throwing — but the tab must be explicit about which shape it got, not
      silently show an empty table for one of them.
- [ ] Re-measure a record afterwards. The point of the exercise is the size; state the before and after.

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

- [ ] e2e for both endpoints, including that a non-superuser gets the floor's answer, not the graph.
- [ ] `vite build`, not just `vue-tsc` — a new dependency and new imports are exactly what a typecheck
      cannot confirm (see the `shims-vue.d.ts` note in the insights task).
- [ ] Record size before and after Part B, measured on a real record from `InsightsRecordingSpec`.
- [ ] An OLD (fat) record still renders the kontainer tab.

## Red-team follow-up

Extend `.claude/tasks/20260730-redteam-insights-api.md` rather than opening a new file: the two new
endpoints expose the full class graph and the database schema, which is a bigger disclosure surface than
anything currently under that floor.
