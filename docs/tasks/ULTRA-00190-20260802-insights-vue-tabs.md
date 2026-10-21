# ULTRA-00190: Insights — the Vue detail page and its collector tabs

**Status:** IN PROGRESS — started 2026-08-02. Gate blockers S1, I4, TAB-SPECS, D5, D6 and the harness DONE
2026-10-19 (track T1); `/feature-review` gate PASS in round 2, merged to master 2026-10-19. Still open: the inventory gaps
below (icons, highlighting, sort, FQNs, `vars`) and step 10 (delete `reference/` — kept until T4 ports from it). The
round-2 MINORs are DONE (`docs/tasks-archive/2026-10/20261019-insights-vue-round2-minors.md`, merged `348c66bc`)
**Plan:** ULTRA-00153 (frontend sdk vue contributors) → "Insights specifics"
**Spec:** `funktor/insights/reference/TAB-SPECS.md` — all ten tabs, wire shapes read from a real record
**Security-critical:** yes. Superuser-only data, rendered from attacker-controlled strings.

## Where this actually starts from

More is done than the plan's "TODO — not started" suggests. Verified on disk 2026-08-02:

| Piece | State |
|---|---|
| REST API — `listRecords`, `getRecord` | DONE, `authFloor = { isSuperUser() }` on the group |
| Generated TS client `api/funktorInsightsClient.ts` | DONE (moved under `api/` by the codegen agent, `920ca11a`) |
| Envelope types in `models.ts` — `InsightsRecord`, `…Summary`, `…Ref`, `…CollectorSlice` | DONE |
| `TAB-SPECS.md` — 224 lines distilling all ten collectors | DONE |
| `reference/` — old renderers kept verbatim, with `VUE-REF:` breadcrumbs in the live collectors | DONE |
| The `.vue` files and the `TsSdkContributor` that ships them | **this task** |

So this is the view layer only. `grep -rn "VUE-REF" funktor/` lists what is outstanding; the README says
to delete a reference file and its TAB-SPECS section as each tab ships.

## Two things to settle before writing `.vue`

### D-1. A hand-written `.vue` has no way to import generated code

The plan opens with this as "the core problem" and it is still unsolved in code:

- `funktor-demo/sdkgen-app/tsconfig.json` has **no `paths`**; `vite.config.ts` has **no `resolve.alias`**.
  There is no `@sdk`.
- Every shipped resource so far only imports its own siblings inside `runtime/` (`./apiResponse.ts`,
  `./auth.ts`), which is why this has not bitten yet.
- The barrel `index.ts` **is** emitted (`TsBarrelEmitter` even documents `import … from '@sdk'`), so the
  target exists — nothing resolves the specifier to it.

A component needing `InsightsRecord` from `models.ts` therefore has no import that survives being emitted
to an arbitrary depth. The generator *does* choose the destination path, so rewriting a sentinel
specifier at emission time is available and needs no app-side config — but that is generator work, and
the generator belongs to the codegen agent.

**Consequence for build order:** components that take everything through props import nothing generated
and can be written today. The page that calls the client cannot. So build leaves-first, and the page last.

### D-2. Ownership overlaps the codegen agent

Their backlog (ULTRA-00184 (codegen loop handoff) §4) has "an insights contributor — the registry's
SECOND consumer". Proposed split, to be confirmed with them via `.claude/BUILD-LOCK.md`:

- **Them:** the mechanism — import-specifier resolution, registry, emission.
- **Me:** the view layer — the `.vue` files, and the `TsSdkContributor` in `funktor/insights` that ships
  them as classpath resources.

## Where the files live — `funktor/ui` was a mistake, corrected 2026-08-02

All generator inputs are in `funktor/codegen/src/main/resources/ts/` — `ui/` for the design system,
`insights/` for the tabs and pages. **`funktor/ui` existed for about two hours and is deleted.**

The reasoning that created it — "shared UI deserves a module, views stay with their feature module" —
applied a *runtime* boundary to files that never run in a JVM. Nothing in Kotlin reads them (verified by
grep), and `ultra/codegen/…/ts/runtime/auth.ts` already mirrors `funktor:auth` from inside the generator.
The version-skew argument does not survive either: a view ships iff its CONTRIBUTOR IS REGISTERED, and
contributors must live in `funktor/codegen` regardless (`AuthTsContributor`'s KDoc — a feature module
depending on `ultra:codegen` would drag the generator into a production artifact). Resource location has
no bearing on it. The move also deleted two cross-module dependencies that existed only so a contributor
could read another module's bytes.

**No parity spec for the collector `Data` classes** (maintainer, 2026-08-02). Backend and frontend are
assumed in sync — the SDK is generated per app from the running server, nothing is published, and there
is no back-compat requirement, so the two cannot be at different versions in practice.

Worth knowing why that is affordable here specifically, since `runtime/datetime.ts` *does* carry a parity
spec: the readers in `slices.ts` narrow rather than cast, so a renamed collector field degrades to `n/a`
or a JSON-tree fallback instead of throwing. `datetime.ts` has no such cushion — a codec change there
silently corrupts a value rather than showing a gap.

## One blocker remains, in the codegen agent's area

Written up in ULTRA-00193 (insights to codegen handover).

**The emit path is a depth contract.** The pages import `../models.ts` and `../ui/JsonTree.vue`
relatively, so `ui/` and `insights/` must both sit at depth 1 under `<out>/`. The codegen agent's
example used `pages/insights/…`, which would miss every one of those imports. Decided one way or the
other by one of us, not both — it is baked into nine files, and it stops mattering when `@sdk` lands.

**`@layer` adopted** (the decision they left open). `theme.css` declares `@layer fk.base, fk.features;`
and wraps its body in `fk.base`; `insights.css` uses `fk.features`. Unlayered CSS beats layered CSS
regardless of specificity, so an app's own `.fk-*` rule wins with no `!important` and no specificity
fight — level 3 of the customization ladder stops depending on load order.

**Measured, and counter-intuitive:** esbuild's CSS minifier **deletes** the bare
`@layer fk.base, fk.features;` statement. Verified present unminified and absent from the production
bundle. It is safe — the minifier only drops it when the layers are defined later in that same order — but
**layer order cannot be confirmed by grepping the built CSS.**

## The stylesheets cannot ship yet — and a comment is not a mechanism

`theme.css` and `insights.css` are written but **inert**. The codegen agent is building the mechanism in
ULTRA-00191 (css contribution and app scaffold); two of their findings land on this task:

- **Nothing imports a `.css` in the SDK dir.** Emission is not the gap — `validatePath` is
  extension-agnostic — but Vite bundles only what something imports, so the file is dead weight until a
  `styles.ts` imports it.
- **Order is semantic, and I did not guard it.** `insights.css` reads variables `theme.css` defines, and
  contributors arrive from a DI container with no order. Getting it backwards produces no error, just
  overrides that silently lose. `insights.css` carries a comment saying "load after `theme.css`" — that
  is documentation, not enforcement. When their `Scope.style(path, order)` lands, **declare the order
  rather than relying on that comment.**

## Security rules for this task, not negotiable

- **Never `v-html`.** Every string in a slice is recorded verbatim from an unauthenticated request —
  headers, query params, paths, user agents. `{{ }}` and `v-text` escape; `v-html` does not. This is
  already written into `InsightsCollectorSlice`'s KDoc and is the single rule most likely to be broken by
  a "render the pre-formatted trace" shortcut. `routing.trace` and `log.entries[].text` are exactly that
  shortcut, and both must go in a `<pre>` via text interpolation, never as markup.
- `***redacted***` is a **value to render**, not an error state.
- The tab must never widen what the API serves. If something is missing from a record, it is missing —
  do not add a live lookup to fill it in (see the vault graph below).

## Per-tab gates — two are blocked, and not by the same thing

| Tab | Gate |
|---|---|
| `vault` — the `vars` field | **BLOCKED** (D4, not blocking this task). ULTRA-00171 (query vars in insights) is open and needs a maintainer decision. Bind values include auth lookups — the token or activation code *being looked up*. Build the tab **without rendering `vars`**; everything else in it is fine |
| `vault` — the database graph | **BUILT 2026-10-19** from its own endpoint (T2's `graphs/database`), not from the record |
| `app-config` | **UNBLOCKED 2026-08-02**, was not on 2026-07-31. `Redacted<T>` closed all four known leaks — verified field by field, see the corrected note in TAB-SPECS. Residual, and it must reach the docs: an *app's own* config secret in a plain `String` is still rendered verbatim |
| `kontainer` | Graphs **BUILT 2026-10-19**: a full (old) record draws from itself, a slim one joins `graphs/kontainer` |
| the rest | Free |

## Build order

Leaves first, because of D-1 — each step is useful even if the import question stays open.

- [x] **1. `funktor/ui` — `theme.css` and the primitives** — DONE 2026-08-02, see "Step 1 evidence" below.
      Props-only, importing nothing generated:
      `JsonTree` (the honest rendering for anything untyped), `StatStrip`, `KeyValueTable` (used by
      `request` and `response`), `PreBlock`. Unscoped `fk-`-prefixed classes, CSS variables as the
      theming seam, neutral default palette — settled 2026-08-02, see the plan's "Styling and the design
      system". These land in a **new module**, not in `funktor/insights`.
- [x] **2. The cheap tabs** — DONE 2026-08-02. `request`, `response`, `user`, `routing`, `template`, `log`.
      Each is a table or a `<pre>`, with the raw slice behind a native `<details>` — no state, no JS, and
      collapsed by default so a big slice costs nothing. `template` treats `timeNs: null` as the normal
      case. **Every tab degrades to `JsonTree` rather than throwing** when the slice shape is not
      recognised: records outlive the code that wrote them, and one bad record must not take the page.
- [x] **3. `runtime`** — DONE 2026-08-02. Seven-cell strip in `%d MB`, then system properties as a table
      rather than cells (long, unordered, only ever read by searching). Says "not measured" when both
      descriptor counts are 0, since that is what non-Unix means — not "none open".
- [x] **4. `vault`** — DONE 2026-08-02, minus `vars` and minus the graph. Totals summed in `slices.ts`;
      every one was a private `lazy` and is not in the record.
- [x] **5. `kontainer`** — DONE 2026-08-02. The sortable table, instantiated services first. **Graph not
      built:** everything it needs IS derivable from the record (unlike vault's), but it needs a graph
      library, which is an SDK dependency decision rather than this tab's to make.
- [x] **6. `app-config`** — DONE 2026-08-02. `JsonTree` twice, with the caveat rendered **in the UI**:
      `Redacted` fields show as `***redacted***`, anything else is verbatim, and an app's own secret in a
      plain `String` is therefore displayed. A comment would not have reached the person reading it.
- [x] **Overview** now inlines the Database and Runtime strips, as the old GUI did. The cell builders
      live in `slices.ts` rather than in the tabs, because `<script setup>` cannot export and two copies
      would drift the first time a unit or threshold moved.
- [x] **7. The detail page** — DONE 2026-08-02. Tab shell, prev/next, Overview landing section.
      The Database and Runtime strips are NOT inlined yet; they arrive with steps 3 and 4.
      **The tab registry is open by design:** an unregistered key renders through `JsonTree` rather than
      erroring, which is how an app-defined collector appears and how the four unbuilt tabs appear today.
      So the page is complete and usable now, and each later tab is an upgrade rather than an unblock.
- [x] **8. The list page** — DONE 2026-08-02. Paged `listRecords`, and the deleted insights *bar*'s job.
      Duration bands are three, not the old four: olive ("slightly slow") is dropped, because the theme
      carries three tones and a fourth colour earns less than explaining it costs.
- [x] **D-1 turned out not to block these.** The pages import `../models.ts`, `../runtime/*` and
      `../api/funktorInsightsClient.ts` — relative, and correct as long as the contributor emits `funktor/ui`
      at `<out>/ui/` and these at `<out>/insights/`. That is the plan's "freeze the layout" option, taken
      as an interim. When the `@sdk` alias lands the imports become alias-based and stop depending on
      depth; until then **the layout is a contract the contributor must honour**.
- [x] **9. The contributor** — `InsightsTsContributor`, in `funktor/codegen` (see "Where the files live"),
      not `funktor/insights` as first planned. Page routes are `requiresAuth = true`, **declared not derived**.

      **Use `out.sharedResource` for every `funktor/ui` file, never `out.resource`** — flagged by the
      codegen agent, verified in the code. `resource` delegates to `file`, which is exclusive: *"a second
      writer is a hard error"* (`sdk/TsSdkOutput.kt:218`). `shared` dedupes identical content and still
      fails loudly on a genuine conflict, and its KDoc names `components/JsonTree.vue` as the case it
      exists for. **Mixing the two on one path is also an error**, which settles a design question:
      `funktor/ui` must NOT have its own contributor emitting these exclusively while consumers share
      them. Every module that needs the primitives asks for them via `sharedResource`, so "the ui ships
      iff something needs it" holds by construction — the same property that keeps views in their module.
      The tabs and pages themselves stay `resource`: `funktor/insights` genuinely is their sole owner.
- [ ] **10. Delete as you go** — per `reference/README.md`, a shipped tab deletes its `reference/collectors/*.kt`
      and its TAB-SPECS section. When both are empty, delete `reference/`.
      **Not done, and needs a maintainer decision (T1, 2026-10-19):** every tab has shipped, but T4 (the Kraft
      port) still needs the old renderers as its spec, and TAB-SPECS is now the CURRENT wire reference rather
      than a distillation to delete. Recommendation: keep `reference/` until T4 lands, then delete
      `collectors/` + `gui/` + `assets/` and keep TAB-SPECS (moved next to the collectors).

## What the wire actually looks like

`funktor/insights/reference/TAB-SPECS.md`, re-derived from current records on 2026-10-19; the samples
themselves are `funktor/codegen/vue-verify/fixtures/`. The traps worth knowing without opening it:
`request.method` / `response.status` are objects; `request.uri` is the path only; `user.userId` is flat and
`isSystem`/`isAnonymous` do not exist (`_type` does); `createdAt` is `{ts, timezone, human}` in MILLIS (only
Jackson-era records have seconds); the kontainer slice has two shapes; `vault` has no `vars`.

## The collector `Data` classes are NOT typed in TypeScript, despite the KDoc

`InsightsCollectorSlice`'s KDoc says "each collector's `Data` class is a codegen root, so the frontend
gets a generated schema for exactly its own slice". **That is aspirational — verified false on
2026-08-02.** `models.ts` contains the four envelope types and *no* collector `Data` type.

It is also not uniformly achievable: `AppConfigCollector.Data(val info: Any, val config: Any)` has no
derivable shape at all, and `ResponseCollector.Data` holds a ktor `HttpStatusCode` that would need a
claim. So the honest split is per-collector: claim the ones with a real shape, render the rest through
`JsonTree`. Either make the KDoc say that, or make it true — but it must not stay as it is.

## Step 1 evidence — the primitives, 2026-08-02

`funktor/ui/src/main/resources/ts/ui/`: `theme.css`, `types.ts`, `JsonTree.vue`, `KeyValueTable.vue`,
`StatStrip.vue`, `PreBlock.vue`. Not yet a gradle module — `settings.gradle` is untouched, so none of
this is on the build path yet.

**One rule worth stating because it is easy to undo: no `<style>` block in any component.** All CSS lives
in `theme.css`. A component that injects its own styles cannot be fully restyled by an app that declines
to import the theme, which would quietly cost level 4 of the customization ladder.

Verified by rendering, not by reading:

- `vue-tsc --noEmit` clean, in an isolated scratch dir against the demo app's toolchain.
- SSR-rendered every primitive with `<img src=x onerror=alert(1)>` as key, value, label and scalar.
  **All five payload cases came out escaped; zero reached the DOM as markup.**
- `JsonTree` at `expandDepth: 0` does not render the child at all — confirmed, since `v-if` on collapsed
  subtrees is what keeps the 137.8 KB kontainer slice cheap.
- **Mutation-tested.** Switching `PreBlock`'s interpolation to `v-html` produced `leaked=1` and a
  non-zero exit. The guard fails when it should, so the green run means something.

## Step 2 / 7 / 8 evidence — the tabs and pages, 2026-08-02

Six tabs, three pages, `slices.ts` and `insights.css` under
`funktor/insights/src/jvmMain/resources/ts/insights/`. Still no gradle module, still nothing on the
build path.

- **`vue-tsc --noEmit` clean against the REAL generated SDK** — the scratch tree mirrors the emitted
  layout, so this proves the relative imports resolve and the types match what the generator emits, not
  what I assumed it emits.
- **18 render cases, 0 failures.** Every tab with the payload in each attacker-reachable position; the
  URL recomposed from `scheme`/`host`/`port`/`uri`; status and log-level tones; `***redacted***` marked
  as metadata.
- **The harness fails a case that supplies a payload but never renders it** (`BLIND`). Without that, a
  tab that silently dropped a field would pass the escaping check by not showing anything.
- **Degradation is tested, not assumed:** `[1,2,3]`, `'just-a-string'`, `{entries:'nope'}` and `{}` all
  fall back to a JSON tree. None throws.
- All three pages mount, compile and reach their loading state.

**The gap this left — the pages' LOADED state, and a harness that lived in a scratch dir — is closed**
(2026-10-19): `funktor/codegen/vue-verify` runs on every `check`, with happy-dom, and
`tests/detail-page.spec.ts` drives the detail page through the real generated client over a recorded record.

## Steps 3–6 evidence — the four heavy tabs, 2026-08-02

27 render cases, 0 failures; `vue-tsc` clean. Three things worth keeping:

**The `vault` tab has no raw dump and no JSON fallback, and that is deliberate.** I wrote both in, the
way every other tab has them, and then noticed they render the whole slice — `vars` included. Suppressing
one field while shipping a viewer for the object that contains it is not a control. An unreadable vault
slice now shows a notice explaining the suppression instead of falling back to `JsonTree`.
**Mutation-tested:** reinstating the raw dump makes the harness report the bind value on the page.

**Two of the three initial failures were the TEST being wrong, not the code** — worth recording because
both would have been easy to "fix" in the wrong direction:

- I asserted `2 / 3` for young/old/total where the code correctly produces `3 / 2 / 5`. My arithmetic.
- The `app-config` case put its payload in `config`, which renders collapsed at `expandDepth: 1`, so it
  never appeared — the harness caught this itself as `BLIND` rather than passing quietly.

**A false positive with a real cause:** the leak check `html.includes('vars')` fired on the *garbage*
case, which has no bind values. **Vue SSR renders template comments into the output**, and my own comment
explaining the omission contains the word. The check now asserts on the bind VALUE, which is the property
that matters.

## IDE diagnostics sweep, 2026-08-02 — and a hole in the verification story

Swept the generated SDK file-by-file with IntelliJ's inspections (`mcp__idea__get_file_problems`).
30 of ~44 files checked individually; all clean except the two noted below.

**The sweep found the app was BROKEN, and `vue-tsc` could not see it.**
`InsightsTsContributor.INSIGHTS_FILES` is a manual mirror of a resource directory and had drifted: the
four newer tabs were imported by `InsightsDetailPage.vue` but never emitted. `vite build` failed with
`UNRESOLVED_IMPORT`; `vue-tsc` was clean, because the app's `shims-vue.d.ts` declares `module '*.vue'`
— a wildcard that resolves ANY `.vue` specifier whether the file exists or not.

> **`vue-tsc` cannot catch a missing `.vue` file. Only `vite build` can.** That applies to every
> contributed component, not just these. Do not treat a green typecheck as proof the SDK is consumable.

`InsightsTsContributorSpec` now fails when a list and its directory disagree, in either direction. It
reads the SOURCE tree, not the classpath — the classpath copy is a build output, so a stale build would
let the list agree with it while the source disagreed. Mutation-tested.

### Fixed

| Finding | Fix |
|---|---|
| 4 tabs emitted-but-not-listed | added to `INSIGHTS_FILES`, plus the spec above |
| `defineEmits<{ select: [...] }>` — IntelliJ cannot type the emit and reports every call as *"not assignable to parameter type any"*, including a plain `string` | call-signature form; equally typed, `vue-tsc` accepts both |
| 3 `{@link}` references to components the file does not import | plain `` `code` `` — the same rule CLAUDE.md sets for Kotlin KDoc |

### Fixed on 2026-08-09 — the maintainer's suggestion was right

**Annotate the const, do not parameterise `ref()`.** `const selected: Ref<T | null> = ref(null)` resolves
in IntelliJ where `const selected = ref<T | null>(null)` does not. Identical to TypeScript; `vue-tsc`
accepts both. Applied to `InsightsPage.selected` and `InsightsDetailPage.record`.

That alone made the detail page WORSE — two template errors became four, because resolving the type
newly exposed `record.value?.collectors.find((s) => …)` as *"Argument types do not match parameters"*.
Two further changes finished it: a `collectors` computed so the template never reaches into the nullable
ref, and an annotated local plus an **explicitly typed callback parameter** at each `.find`. Both pages
are now clean, confirmed twice each.

### The one left, and why no code change fixes it

`InsightsPage.vue` ×1 **weak warning**: `props.client === undefined` reported as always false. IntelliJ
drops `| undefined` from optional props — measured with AND without `withDefaults`, so there is no code
shape that avoids it. It also sits in the provide/inject fallback the codegen agent flagged as
PROVISIONAL pending a `useClient()` composable, so it is not worth contorting.

### A correction worth keeping

I told the maintainer `v-if` makes `selected.bucket` safe and they pushed back, reasoning that props are
evaluated before being passed. **The measurement is on the `v-if` side:** a component rendered with
`selected = null` and `:bucket="selected.bucket"` renders fine rather than throwing, because `v-if`
compiles to a ternary wrapping the whole vnode. So `selected?.bucket` is not needed — and it would break
the build anyway, since the prop is `string` and optional chaining yields `string | undefined`.

### Methodology note, learned the hard way

**IntelliJ's analyzer is non-deterministic.** Two identical queries on an unchanged file returned
different results, and that false negative briefly "confirmed" a fix that a later run contradicted.
Anything verified through it needs repeat runs before it counts.

## INVENTORY — old kotlinx.html impl vs the Vue port, 2026-08-09

Read from the inert copy in `funktor/insights/reference/`, collector by collector, **not** from
`TAB-SPECS.md` — the spec is a distillation and omits some of what the renderers actually did.

### Cross-cutting gaps

- [ ] **Icons. Every tab had one; none of the Vue tabs do.** `cloud_upload_alternate` (request),
      `cloud_download_alternate` (response), `user`, `compass_outline` (routing), `microchip` (runtime),
      `list` (logs), `tv` (view), `database` (vault), `cubes` (kontainer), `cog` (config). The registry
      already carries an `icon` for nav entries (`Nav(icon = "gauge")`), so there is a concept to hang
      this on — but no icon set ships with the SDK, and picking one is a dependency decision.
- [ ] **Syntax highlighting.** The old GUI ran everything through Prism: `json()` was
      `prism(Language.Json)`, and the vault tab highlighted the query by its `queryLanguage` plus the
      explain output as `text`. `JsonTree` replaces the JSON case and is better (collapsible,
      searchable by eye) — **but query text is now unhighlighted plain `<pre>`.**
- [x] **Three vis.js graphs** — ported 2026-10-19 (T1), `vis-network` 10.1.2. See
      ULTRA-00197 (insights graphs and static slices) Parts C+D.
- [ ] **The bar's git version and environment id have no home.** The bar is deleted by decision, and
      the list page absorbed status/duration — but `appInfo.version.describeGit()` and
      `appConfig.ktor.application.id` were bar items and are now shown nowhere. Both are in the
      `app-config` slice, so this is a placement decision, not a data problem.

### Per tab

| Tab | Old | Vue now | Gap |
|---|---|---|---|
| Overview | request line, status label, response time, timestamp, Database + Runtime strips | all of it | **none** |
| `request` | raw JSON dump only | facts + headers table + query-param table + raw behind `<details>` | icon. Otherwise **better than the old one** |
| `response` | raw JSON dump only | status fact + headers table + raw | icon. **Better** |
| `user` | `H4 User` + JSON, `H4 Permissions` + JSON | facts + permissions tree + raw | icon. **Better** |
| `routing` | `<pre>` of trace, then a JSON dump | `<pre>` of trace | icon. JSON dump dropped on purpose — one field, nothing to add |
| `runtime` | 7-cell strip, then system properties as a flat `k: v` list | 7-cell strip + a searchable table | icon. **Better** |
| `log` | "No log entries", else one coloured block per entry with `<pre>` | same, same colour mapping | icon |
| `template` | raw JSON dump | render time with threshold tone | icon. **Better** |
| `app-config` | `H4 AppInfo` + JSON, `H4 Config` + JSON | two trees + the secrets caveat rendered in-UI | icon |
| `vault` | strip, **graph button + vis.js network**, per-query segment, **Prism-highlighted query**, **`vars` JSON**, Explained in `<details>`, **"Database Graph Data" raw dump** | strip, per-query cards, plain-text query, Explained | **4 gaps — see below** |
| `kontainer` | **2 graph buttons + vis.js**, **click-sortable table**, full FQNs | table, sorted once, shortened FQNs with full in `title` | **3 gaps — see below** |

### `vault` — the four gaps

- [ ] **Query syntax highlighting** — `prism(it.queryLanguage) { it.query }`, and `prism("text")` for the
      explain output. Needs a highlighter the SDK can ship.
- [ ] **`vars`** — BLOCKED on ULTRA-00171 (query vars in insights), deliberately omitted,
      and the reason the tab has no raw dump or JSON fallback either.
- [x] **The repository graph** (red "Show Database Graph" button + network) — DONE 2026-10-19, from
      `GET …/graphs/database`, fetched on first click.
- [x] **"Database Graph Data"** — DONE 2026-10-19, the same single fetch.

### `kontainer` — the three gaps

- [x] **"Show instances Graph" (green) and "Show Full Graph" (red)** — DONE 2026-10-19. Two networks differing only
      in whether services without instances are included. Node label `simpleName\ntype`, size = injected
      class count, colour by type (Singleton `#1E90FF`, Prototype `#FFD700`, Dynamic `#8B0000`,
      SemiDynamic `#9400D3`, DynamicOverride `#DC143C`); edge per injection, arrow at target, **dashed
      when `provisionType == Lazy`**, weight 2 Direct / 1 Lazy. **All of it IS derivable from the
      record**, unlike vault's — this one is purely a graph-library decision.
- [ ] **Click-to-sort.** The old table was `ui.sortable.celled.table` with `semanticui-tablesort.js`
      loaded by `InsightsGuiWebResources` and `$("table.sortable").tablesort()` in `gui.js`. The Vue
      table sorts once (instantiated first, then FQN) and cannot be re-sorted.
- [ ] **Full FQNs.** The old table printed `service.cls.fqn` in full; the Vue one shortens to the simple
      name with the FQN in a `title` tooltip. Deliberate for width, but it makes the column unsearchable
      with the browser's own find — worth revisiting alongside sorting.

### Not gaps — deliberate, recorded so they are not "fixed" later

- The **bar** is deleted (maintainer, 2026-07-30); its job moved to the list page.
- The old **4th duration band** (olive above 75ms) is gone; the theme carries three tones.
- The **`routing` JSON dump** is gone; that slice has one field.

## SUB-TASK — there is no Kotlin/JS (Kraft) insights frontend, and there never was

Checked 2026-08-09. **The old insights UI was server-rendered kotlinx.html**, not Kraft:
`funktor/insights/reference/gui/InsightsGuiRoutes.kt` mounted ktor routes that rendered
`InsightsGuiTemplate` on the server. `funktor/inspect/src/jsMain` — which *is* the Kraft ops UI — covers
cluster, introspection and logging, and **has no insights pages at all**.

Two loose ends left behind by its removal:

- [ ] `funktor-demo/adminapp/.../AdminAppConfig.kt:11` still carries
      `insightsDetailsBaseUrl = ".../_/insights/details/"`, pointing at a route that no longer exists.
- [ ] `funktor/inspect/.../DevtoolsRequestHistoryPage.kt:59-61` has a disabled link with a comment
      saying it "returns when the Vue insights page can" be linked to. It now can — the route is
      `/insights` in the generated SDK app.

**DECIDED 2026-08-24 (maintainer): yes, build it — Vue and Kraft should be mostly on par.** That
amends the plan's "Vue only, no second target", and the amendment is recorded in the plan's decision
table rather than left implicit. The old *server-rendered kotlinx.html* GUI still stays deleted; the
Kraft target is an SPA page alongside the rest of `funktor/inspect/src/jsMain`.

**Tracked in `docs/tasks/20260824-insights-kraft-gui.md`**, which also owns the two loose ends above.
Sequenced after this task's review gate, so the port does not inherit an unreviewed design — including
the `vault` `vars` suppression, which it must carry.

## Test evidence

- [x] `vue-tsc --noEmit` AND `vite build` green in `funktor-demo/sdkgen-app` against a freshly generated SDK
      (2026-10-19), and in `funktor/codegen/vue-verify` on every `check`.
- [x] A component test per tab against a **real recorded slice** — `vue-verify/tests/tabs.spec.ts`, fixtures
      copied by `fixtures/refresh.mjs` from what the funktor/all e2e specs serve (the request
      `InsightsRecordingSpec` records, via `InsightsKontainerSliceSpec`'s dump).
- [x] **The `v-html` guard is a test** — `vue-verify/tests/escaping.spec.ts`, nine positions. Mutation-tested:
      `v-html` in `PreBlock`, `KeyValueTable` or `FactList` each turns exactly the tabs using it red.
- [x] e2e: this task DID add backend behaviour (S1, Monko) — `InsightsVaultBindValuesSpec`, both backends.

## Review record — /feature-review, 2026-08-24

**Gate: FAIL.** All three reviewers reported. Every frontend finding is fixed; the gate fails on two
BACKEND defects outside this diff, and the first is a precondition for this feature meaning anything.

**The CRITICAL was confirmed twice, independently and empirically.** Reviewer 1 compiled a probe against
the built jars (`NO SLUMBERER for QueryProfiler$Entry$Impl`) and then scanned **1193 real depot records**:
`vault.entries` is `[]` in every single one. So the vault tab has never rendered a query, and the
per-query cards were written against a shape inferred from Kotlin types rather than observed — the one
thing TAB-SPECS warns against everywhere else, and admits it did in that section.

**Root cause worth keeping:** `TAB-SPECS.md` was distilled from a **Jackson-era** record. The switch to
Slumber changed three of its stated traps — `createdAt`, the user booleans, and the `_type`
discriminator. **Re-derive the spec and the task's "what the wire actually looks like" from a CURRENT
record**; two of the findings below are the same staleness.

### The premise this whole tab rested on was false

I wrote, in three places, that the `vars` exposure was "contained meanwhile: no frontend renders it".
**That was true of the field name and false of the data**, and the maintainer's 2026-08-24 deferral was
taken on it. Corrected everywhere.

| # | Sev | Finding | Verdict |
|---|---|---|---|
| S1 | HIGH | **Mongo puts bind values in `query`, not `vars`.** `MonkoDriver` records `builder.print()` — the filter document WITH values — and sets `vars = emptyMap()`. `findByToken` filters on the raw, unhashed session token, so the Database tab rendered a live token. | **CONFIRMED**, fixed |
| S2 | HIGH | **Arango's EXPLAIN substitutes bind values into the plan**, so `queryExplained` reproduced exactly what `vars` withholds. Reviewer measured it against a real server. Both backends affected. | **CONFIRMED**, fixed |
| D1 | CRITICAL | **The vault slice cannot be written at all.** `QueryProfiler.Entry.Impl` is a plain class with a 3-arg ctor: `BuiltInModule` has no matching branch (`isData` no, empty-ctor no) so slumbering throws — inside `launch(Dispatchers.IO)` after the response, so **the whole record is silently dropped** whenever a query was profiled. | **CONFIRMED** — backend, NOT fixed here |
| D2 | HIGH | `kontainer` `createdAt` is a `java.time.Instant`, which Slumber writes as an object, not the epoch-seconds double this read. Every instance timestamp was `n/a`, and a bare-millis value would have been misread as year 58,000 — the one reader that could misread rather than degrade. | **CONFIRMED**, fixed |
| D3 | HIGH | Paging used `items.length >= epp`, but the loader SKIPS unparseable records while consuming their slot, and truncated records are routine. One on page 1 disabled "Next" beside a pager reading "Page 1 of 25". | **CONFIRMED**, fixed |
| D4 | MED | `isAnonymous`/`isSystem` are FUNCTIONS on `UserRecord`, so Slumber never emitted them: both rows read `n/a` on every record ever written. | **CONFIRMED**, fixed — replaced by the `_type` discriminator |
| S3/D7 | MED | `TABS[key]` resolves through `Object.prototype`, so a collector keyed `constructor` skips the `JsonTree` fallback. | **CONFIRMED**, fixed |
| I2 | MED | **`TemplateTab` had no JSON fallback** and collapsed "no view rendered" with "slice unreadable" into one `null` — so a renamed field showed a 4.5 ms render as nothing, with the data displayed nowhere. | **CONFIRMED**, fixed |
| I3 | MED | **A header or query parameter named `__proto__` vanished from the table.** `out[key] =` on an object literal hits `Object.prototype`'s setter: no own property is created, so the table omitted it while the raw-slice tree beside it showed it — two views of one record disagreeing, in a forensic tool, on a key an attacker picks (`GET /x?__proto__=1`). | **CONFIRMED**, fixed — null-prototype maps |
| I4 | MED | The spec guards the file LISTS but not the IMPORTS between them: rename a `ui/` file, update `UI_FILES`, miss one importing tab, and both set comparisons pass while `vite build` fails downstream. | CONFIRMED, **fixed 2026-10-19** |
| I5 | LOW | `durationTone` restated `toneAbove` by hand — the helper's one call site that had gone its own way, and it exists precisely to stop that. | **CONFIRMED**, fixed |
| D5 | LOW | The Overview runtime strip loses the "not measured" caveat the tab shows. | CONFIRMED, **fixed 2026-10-19** — the caveat is in the cell (`runtimeCells`) |
| D6 | LOW | Overview status drops the reason phrase the old Overview had. | CONFIRMED, **fixed 2026-10-19** — read from the `response` slice |
| I6 | INFO | `bytesToMb` rounds where the old tab truncated (100.7 MB → `101` vs `100`). Recorded so it is not "fixed" backwards; rounding is the better choice. | noted |

### The fix for S1/S2, and why it is an allowlist

`VaultTab` now renders `query` only for languages whose recorded text is known to keep values OUT of it
(`aql`), and does not render `queryExplained` at all. **Fail-closed**: an unknown driver is suppressed,
not trusted. Mutation-tested — reinstating the explain block makes the harness report the token.

**This is a mitigation, not the fix.** The token is still written to a depot file on disk; the UI change
only stops it reaching a screen. The real fix belongs with D1 in the backend.

### Blocking the gate

- [x] **D1 — DONE 2026-08-24** (`e448c64d`). `VaultCollector.Data` is an explicit `data class`;
      `VaultCollectorSlumberSpec` proves the slice slumbers at all, that the timings survive rather than
      serialising to `{}`, and that no bind value reaches the record. Mutation-tested. The kontainer
      half of the same reshape is still in ULTRA-00197 (insights graphs and static slices).
      *(Ticked 2026-09-28 — the work landed a month ago and this box was never updated.)*
- [x] **S1 at the source** — DONE 2026-10-19 (T1). Monko records typed `@pN:<type>` placeholders with the
      values in `vars`, under its own label `mongo-bound` (round 1), which `VaultCollector` admits; the Database
      tab shows Mongo queries again. Both backends e2e.
- [x] **I4** — DONE 2026-10-19. The spec follows every relative import of every emitted file of the REAL
      insights SDK and asserts the target is emitted.
- [x] **Re-derive TAB-SPECS from a current record** — DONE 2026-10-19.
- [x] Re-run the suite, regenerate, and re-verify — DONE 2026-10-19, see "Test evidence (T1)".

**Red-team follow-up:** required — ULTRA-00161 (redteam insights api) already exists for the
API half; extend it with the rendering scenarios (stored XSS via header/UA, secret disclosure through the
config tab) rather than opening a second file. T1 may not edit that file; its proposals are below under
"Red-team scenarios proposed (T1)" for the coordinator to carry over.

## T1 — implementation notes (2026-10-19, branch `wt/insights-vue`)

The gate blockers. The graphs and the kontainer shapes are recorded in
ULTRA-00197 (insights graphs and static slices), Parts C+D.

- **S1 at the source.** `monko/core/src/main/kotlin/MonkoDriver.kt:148` `FindQueryBuilder.printQuery()` turns
  every filter value into an `@pN:<type>` placeholder (`placeholderise`) with the values, as plain Java values,
  in `vars`; field names, `sort`, `limit`, `skip` stay, as AQL keeps attribute names and `LIMIT`. Fail-closed:
  only documents and arrays holding documents are walked, so a whole `$in` list is ONE value. `findStored`
  records it; `print()` stays the raw debugging aid. `VaultCollector.kt:107` admits Monko's label — `mongo-bound`
  since round 1 (the line numbers of this bullet are in "T1 — round 1 fix batch").
- **I4.** `funktor/codegen/src/test/kotlin/InsightsTsContributorSpec.kt:103` walks every relative import of
  every emitted `.ts`/`.vue` of the SDK built from the REAL `InsightsApiFeature` (`RealInsightsSdk.kt`, which
  needs `testImplementation(project(":funktor:insights"))`, `build.gradle.kts:45`, test-only) and resolves it
  (`:45`). Two further pins: no barrel-exported `.ts` imports a package but `vue`; only `NetworkGraph.vue`
  imports `vis-network`. The scanner is `funktor/codegen/src/main/kotlin/TsImports.kt:20` (comments stripped).
- **TAB-SPECS** re-derived from current records: `funktor/insights/reference/TAB-SPECS.md`.
- **D5** `slices.ts:708` `fileDescriptorsUnmeasured`, used by the cell hint in `runtimeCells`, so the Overview
  strip says "not measured" too. **D6** `InsightsDetailPage.vue:132` reads the reason phrase from the
  `response` slice, only when its status equals the headline's.
- **How an app gets `vis-network`:** `funktor/codegen/src/main/kotlin/cli/TsSdkGenerateCliCommand.kt:128` —
  `sdk:ts:generate` refuses an SDK whose package imports `<out>/package.json` does not declare (`:169`), naming
  the package, its importers and the `pnpm add` line (`:194`). Verified on the demo: it refused until
  `sdkgen-app` declared `vis-network`.

### The harness — where, and why there

`funktor/codegen/vue-verify/`, wired into `funktor/codegen`'s `check` as `vueVerify`
(`build.gradle.kts:70,98,121`; opt out `-PskipVueVerify=true`). Modelled on `ultra/codegen/ts-verify`: its own
`package.json`, frozen `pnpm-lock.yaml`, `.npmrc` (pinned Node 22.12.0, pnpm enforces `packageManager`).

- **Not in `sdkgen-app`**, because that app's SDK is generated by RUNNING the demo server, is gitignored, and
  is not built by Gradle at all — tests there would guard nothing in CI.
- **In `funktor/codegen`**, because the Vue resources and their contributor live there: a change to either
  fails that module's own `check`. The SDK it tests is generated from the real insights feature by the same
  kontainer wiring `sdk:ts:generate` uses (`generateVueVerifySdk`).
- **No `shims-vue.d.ts`** in its tsconfig, unlike the demo: `vue-tsc` resolves `.vue` itself, so a missing
  component fails the typecheck (probed: TS2307). `verify` = `vue-tsc --noEmit && vite build && vitest run`;
  vitest writes JUnit XML to `funktor/codegen/build/test-results/vueVerify/`.
- **Fixtures are real responses**, copied by `vue-verify/fixtures/refresh.mjs` from the funktor/all e2e dumps
  (`INSIGHTS_WIRE_SAMPLES_DIR`). It rewrites three things, each identifying the build machine: the runtime
  slice's `systemProperties` keep six keys, `insights.server` becomes `build-host`, and per-worktree database
  names become `funktor_test`.

**Versions** (looked up on npm 2026-10-19, pinned exact): vitest 5.0.5, happy-dom 21.0.3, vis-network 10.1.2,
vue 3.5.44, vite 8.3.4, @vitejs/plugin-vue 6.0.10, vue-tsc 3.3.13, zod 4.6.7 — all newest. TypeScript **6.0.3**,
not the newest 7.0.2: `vue-tsc` cannot run on TS 7 (see `sdkgen-app/README.md`), and 6.0.3 is the newest it
runs on. No `@vue/test-utils`: `createApp` + happy-dom sufficed, one dependency fewer.

## Test evidence (T1, 2026-10-19)

Counts from `build/test-results/**/TEST-*.xml`, after rebasing onto `525b1da1`; all failures 0:

- `:monko:core:test` — 26 specs, 256 tests (new `MonkoDriverPrintQuerySpec` 7).
- `:funktor:insights:jvmTest` — 13 specs, 95 tests (`VaultCollectorSlumberSpec` 4, rewritten for `json`).
- `:funktor:codegen:test` — 9 specs, 100 tests (`InsightsTsContributorSpec` 13, +3; `TsImportsSpec` 5, new;
  `TsSdkGenerateCliCommandSpec` 24, +4).
- `:funktor:all:jvmTest` — 16 specs, 183 tests (`InsightsVaultBindValuesSpec` 5, new: Arango AND Mongo).
- `:funktor:codegen:vueVerify` — vue-tsc clean, vite build (vis-network its own 652 KB / 158 KB gzip chunk),
  vitest **6 files, 58 tests**: `tabs` 15, `escaping` 9, `graphs` 17, `graphs-real-library` 2, `detail-page` 5,
  `slices` 10.
- `funktor-demo/sdkgen-app`: `pnpm run build` (vue-tsc on TS 5.9.3 + vite build) green on a freshly generated SDK.
- **Full suite** (`console/wt-gradle.sh --full-suite`, 2026-10-19 16:59-17:09, 8m 10s, on `18cef881` rebased onto `ea7bd99b`): BUILD SUCCESSFUL -- 69 targets, 929 spec files, 15 751 tests, 7 skipped, **0 failures, 0 errors** (`vueVerify` ran inside it: its 58 vitest cases are in that count).

## Mutation table (T1, 2026-10-19)

Each mutant backed up with `cp` to the session scratchpad (source-set path in the name), run, restored with
`cp`, verified by `cmp` and a clean `git status`. Kotlin failures read from the console, counts from the XML.

| # | Mutant | Red in | Why that is the right reason |
|---|---|---|---|
| S1-a | `findStored` records `print()` again | `InsightsVaultBindValuesSpec` 2/4 | "the token appears NOWHERE" (the token is in the served record) and "recorded as placeholder text" (no `@p1`) |
| S1-b | `placeholderise` leaves strings unbound | `MonkoDriverPrintQuerySpec` 3/7; e2e 2/4 | the token in the text, on both levels |
| S1-c | allowlist without `json` | `VaultCollectorSlumberSpec` 1/4; e2e 1/4 | the Mongo text is dropped; the leak test rightly stays green |
| S1-d | allowlist ignored, every text kept | `VaultCollectorSlumberSpec` 2/4 | the unvetted `sql` entry's token. e2e green — correctly: both real drivers are safe now |
| S1-e | the EXPLAIN plan recorded as the query | e2e 2/4 | the database substitutes the token into the plan (S2), on both backends |
| S1-f | Karango appends `vars` to its text (temporary, outside T1's area) | e2e 1/4 | the Arango half of "nowhere in the record" |
| V1 | `PreBlock` `{{ text }}` -> `v-html` (Gradle) | escaping 3/9: routing, log, vault | "the payload reached the DOM as markup: expected `<img …>` to be null" |
| V2 | `KeyValueTable` value -> `v-html` (Gradle) | escaping 3/9: request, response, runtime | same message |
| V3 | `FactList` value -> `v-html` | escaping 2/9: request, user | same message |
| B1 | `UserTab` silently drops the e-mail row | escaping user (BLIND), tabs user | the payload is not on the page: a dropped field cannot pass as escaped |
| D5 | the caveat back in the tab only | detail-page D5, slices | the Overview strip says `max: 0` |
| D6 | the reason phrase dropped | detail-page D6 | `200` instead of `200 OK` |
| I4-a | `KontainerTab` imports `./NetworkGraphs.vue` | `InsightsTsContributorSpec` 1/13 — the import walk ONLY | the mirror checks pass, exactly the defect class I4 named |
| I4-b | `graphs.ts` imports a type from `vis-network` | `InsightsTsContributorSpec` 2/13 | a barrel-exported file would make every app depend on it |
| C1 | the package.json check never runs | `TsSdkGenerateCliCommandSpec` 2/24 | **`--dry-run` stays green**: it pins "writes nothing", not that the problem is printed |

Graph and reader mutants: the graphs task, "Mutation table (T1)".

**Found while mutating, and worth keeping:** a Gradle `vueVerify` run copies the generated SDK into the
harness's gitignored `src/funktorsdk/`, so restoring the RESOURCE after V2 left the mutant alive in that copy.
The next ten vitest mutants all showed the same three extra failures. Caught because the extras repeated
across unrelated mutants; the mutation helper now diffs the whole copy against the resources before and after
every mutant, and the ten were re-run clean.

## Deviations from the brief (T1)

- **The graph library is the app's dependency, verified rather than installed.** There is no `out.requires`
  mechanism in `ultra:codegen` (outside T1's area); the check sits in `funktor:codegen`'s CLI instead and
  derives the requirement from what the SDK imports, so it cannot drift. It applies to every package (`vue`,
  `zod` too) and only when `<out>/package.json` exists.
- **A failed graph fetch is retried on the next click.** A SUCCESSFUL fetch is kept for the page's lifetime
  (`InsightsPage` owns the source; the kontainer graph per fingerprint since the round-2 MINORs), a failure is
  not cached. A 401 therefore repeats per click; a network blip recovers without a reload.
- **The S1 probe logs two lines**, so `record-vault.json` carries a real log slice — none of the samples had
  one.

## Red-team scenarios proposed (T1) — for ULTRA-00161 (redteam insights api)

Collected, not run; T1 may not edit that file.

1. **A Mongo field path built from a secret.** Placeholders cover VALUES only; keys stay in the text. An app
   that filters on `"tokens.<secret>"` (a map keyed by token) records the secret. Find such a filter in
   funktor and in the demo.
2. **The language label as a trust boundary.** `VaultCollector` trusts `aql` and `mongo-bound` by name; any
   path recording raw text under either label would be trusted. Try a custom `QueryProfiler` entry, and look
   for a Monko path other than `findStored` that profiles under `mongo-bound`.
3. **`$where` / `$expr` / `$function`.** Their JavaScript and expressions are bound as one value each — check
   that no part of an expression survives as structure (e.g. an operator document inside `$expr` with a
   literal in it).
4. **Stored XSS through every tab**: header names, query keys, user agents, a `__proto__` key, a service
   class name, a log line — the escaping spec covers nine positions; look for a tenth (e.g. `JsonTree` keys
   inside the collapsed raw views, the `title` attribute of a service cell).
5. **The graph library**: `vis-network` injects its stylesheet; under a strict CSP without `'unsafe-inline'`
   styles the tooltip breaks. Confirm nothing else (no `eval`, no HTML `title`) is needed.

## What T1 is unsure about

- **`sort` is kept verbatim.** A sort value is a direction or a `{$meta: …}` expression, and a failed query
  records no text (its `query` is set after execution), so no bind value can reach it — reasoned, not tested
  against a server.
- **The CLI check changes behaviour for every app**: an app with funktor insights must declare `vis-network`,
  or `sdk:ts:generate` refuses. Intended per the plan ("failing with the line to paste"), but visible.
- *(Resolved in round 1: the placeholder format — now typed, `@p1:string`.)*

## T1 — round 1 fix batch (2026-10-19)

Every ledger row marked "fix", plus the maintainer's 2026-10-19 decision to drop `numOld`/`numTotal` from the
v2 kontainer slice. Rebased onto master `d9ffbb32` (T3) first, without conflicts.

### What changed, `path:line`

- **MAJOR, the `DynamicOverride` join** — `funktor/codegen/src/main/resources/ts/insights/slices.ts:647` gives a
  slim record's `DynamicOverride` what v1 recorded (`overrideDefinition`, `:667`): it creates the instance's
  class, is `Dynamic`, injects nothing, and overwrites the graph's (blueprint) definition. Pinned against the
  REAL fat record of the same service (`UserProvider`, creates `UserProvider$Lazy`); no edge leaves either
  override in either graph. Edges INTO it (services that inject `UserProvider`) stay, as v1 drew them.
- **The join per `cls`** — `vue-verify/tests/slices.spec.ts`: each joined definition deep-equals the graph's for
  the same class, plus a decoy sharing a simple name put first in the graph.
- **Monko** — `monko/core/src/main/kotlin/MonkoDriver.kt`:
  - the text names the `collection` (`:185`);
  - typed placeholders `@pN:<type>` (`typeLabel`, `:97`): Mongo's `$type` alias read from `bsonType` only,
    arrays `array[n]` — never the value;
  - the label `mongo-bound` (`Printed.LANGUAGE`, `:204`), not `json`; `profile` (`:371`) takes the label, so
    each call site states it;
  - `printQuery`/`Printed` are `internal` (`:181`, `:195`); the sort KDoc names `{$meta}`.
- **The trust boundary** — `funktor/insights/src/jvmMain/kotlin/collectors/VaultCollector.kt:107` allowlists
  `mongo-bound` instead of `json`; the KDoc says the collector trusts the label alone.
- **Old `json` entries** (2026-08-24..2026-10-19) — `slices.ts:408` `isInlinedMongoEntry`: their bind count is
  `unknown`, not the false 0 (`VaultTab.vue:159`), and their empty text says why (`VaultTab.vue:105`).
- **A record naming no graph** — `slices.ts:683` `graphMatch` is `same` / `other` / `unknown`;
  `KontainerTab.vue:175` says the match cannot be verified.
- **The database graph is today's schema** — `VaultTab.vue:140`, one line.
- **CSP note** — `NetworkGraph.vue:11`: no `title` is ever set, so a strict CSP blocking the injected style
  costs nothing.
- **The CLI** — `funktor/codegen/src/main/kotlin/cli/TsSdkGenerateCliCommand.kt`:
  - the class KDoc is true (`:25`);
  - the fix-it line is `pnpm add --save-exact <pkg>@<tested>` (`TESTED_VERSIONS`, `:75`), kept equal to
    `vue-verify/package.json` by a spec;
  - a bad `package.json` fails naming the file (`:192`);
  - `--alias <prefix>` exempts an import the app resolves itself (`:53`);
  - `UndeclaredPackagesException` (`:214`) and `TsImports` are `internal`;
  - the `--dry-run` test asserts the printed `[problem]` and the `pnpm add` line.
  - **Design note, not a defect:** in a pnpm workspace the app's own `package.json` still has to declare
    what it imports (pnpm is strict), so checking `<out>/package.json` alone is right; a root-only declaration
    would fail in the app's own build too.
- **Fixtures** — `vue-verify/fixtures/refresh.mjs`:
  - `insights.server` becomes `build-host` and every `ultra_wt_…` becomes `funktor_test`; grep of the
    committed fixtures for the hostname, `ultra_wt_`, the user name and `/home/`: 0 hits;
  - the comment says re-serialised;
  - more than one candidate record is an error;
  - `fileURLToPath`.
- **The e2e** — `InsightsVaultBindValuesSpec`: `varsCount shouldBe 1` on both backends; Arango
  `` `token` == @v_ ``; Mongo's `collection` and `"@p1:string"`.
- **`numOld`/`numTotal` dropped from v2** (maintainer) — `KontainerCollector.kt:71` `Data` is
  `{graphFingerprint, services}`, `collect` (`:42`) and `finish` (`:120`) lose the tracker, and the collector
  loses its `blueprint` dependency (it existed only for the tracker). Pinned by SLUMBERING the slice
  (`KontainerCollectorSlumberSpec`, `KontainerCollectorFinishSpec`) and on the wire (`InsightsKontainerSliceSpec`).
  - `KontainerCollectorFinishSpec`'s two counter tests are gone with the fields.
  - v1 records keep theirs: shown as recorded, marked "not measured" (`KontainerTab.vue:103`, `counts` `:122`).
  - v2 shows none, and a stray field is not read (`slices.ts:593`).
  - The wire contract in ULTRA-00197 (insights graphs and static slices) and TAB-SPECS follow.

### Test evidence (round 1 batch)

Counts from `build/test-results/**/TEST-*.xml`, all failures 0:

- `:monko:core:test` — 26 specs, 259 tests (`MonkoDriverPrintQuerySpec` 10, +3).
- `:funktor:insights:jvmTest` — 13 specs, 94 tests (the two counter tests gone; one "exactly two keys" added).
- `:funktor:codegen:test` — 9 specs, 103 tests (`TsSdkGenerateCliCommandSpec` 27, +3).
- `:funktor:all:jvmTest` — 18 specs, 185 tests (T3's specs arrived with the rebase).
- `:funktor:codegen:vueVerify` — vue-tsc clean, vite build, vitest 6 files, **66 tests**.
- Full suite: `console/wt-gradle.sh --full-suite` on `ff2a865c`, 2026-10-19 17:53-18:03 (10m): BUILD SUCCESSFUL -- 69 targets, 932 spec files, 15 864 tests, 7 skipped, **0 failures, 0 errors** (vueVerify inside it).

### Mutation table (round 1 batch)

Same protocol: backup outside the worktree with the path in its name, `cp` back, `cmp`, the harness copy diffed
against the resources before and after every Vue mutant.

| # | Mutant | Red in | Why that is the right reason |
|---|---|---|---|
| T1 | the type suffix carries the value (`@p1:string=<value>`) | `MonkoDriverPrintQuerySpec` 8/10; e2e 2/5 | the token, the PIN and the ObjectId hex appear in the text |
| T2 | `array[n]` carries the elements | `MonkoDriverPrintQuerySpec` 2/10 | the token in the `$in` placeholder |
| L1 | Monko records under `json` again | Monko label test; e2e Mongo lookup | the collector drops the text: no `mongo-bound` entry |
| K1 | the collection dropped | Monko structure + paging tests; e2e Mongo lookup | |
| L2 | the allowlist trusts `json` again | `VaultCollectorSlumberSpec` 2/4 | the old Monko entry's token is kept |
| C2 | `--dry-run` prints nothing | the dry-run test | **was the round-0 survivor**; now red |
| C3 | no tested version in the fix-it line | 2 CLI tests | `pnpm add --save-exact zod@4.6.7` missing |
| C4 | `--alias` ignored | the alias test | `@sdk/models.ts` refused as a package |
| C5 | the parse error unwrapped | the bad-manifest test | the message does not name the file |
| O1 | a `DynamicOverride` joined to its blueprint (the MAJOR) | the override test | `creates` is the service key, `overwrites` null |
| O2 | the override keeps the blueprint's injects | **survived first** | the real overrides' blueprints inject nothing: the fixture cannot express it. Killed by a derived test (a real graph, `UserProvider`'s blueprint given a real service's injects) |
| O3 | the override `creates` the service key | the override test | |
| J1 | the join by position | 3 tests | wrong definitions, wrong edges |
| J2 | the join by simple name | **survived first** | the real graph has no two services sharing a simple name. Killed by a decoy put first |
| N1 | a record naming no graph counts as `same` | 2 | |
| M1 | old `json` entries keep their 0 | 2 | |
| M2 | old `json` entries get the generic text | 1 | |
| V4 | the today's-schema note dropped | 1 | |
| Z1 | the v2 `Data` carries `numOld` again | `KontainerCollectorSlumberSpec`, `…FinishSpec`, e2e wire shape | SERIALIZED with the writer's codec: a third key |
| Z2 | a slim record shows counters | the slim tab test | |
| Z3 | the reader reads a slim slice's stray counters | the reader test | |
| Z4 | a full record's counters lose "not measured" | the fat tab test | |

## Review record — T1 round 1 (/feature-review, 2026-10-19), the LEDGER

`reviewer-high` ×3, blind, base `ea7bd99b`. **1 MAJOR → not clean.** S1 holds on both backends (no construct records a
bind value; the e2e proves it on one app with Karango + Monko), no XSS path, the supply chain is pinned. The maintainer
asked for the MINORs to be fixed too, so they ride in the same batch and round 2 reviews them.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `slices.ts:632-635`, `KontainerTab.vue:53,66` (domain) | A slim record joins a `DynamicOverride` service to its BLUEPRINT definition, so the table and both graphs show a definition and edges that did not produce this request's instance (the v1 record of the same request truthfully shows `creates UserProvider$Lazy … overwrites`) | fix | T2's contract (`20260824-…:186-189`) says the graph lists such a service with its blueprint definition, "whose injects did NOT run". Fix: for `DynamicOverride`, synthesise the v1 shape (`creates` = instance class, `injectionType` Dynamic, no injects, `overwrites` = the graph definition), pinned against the real fixtures |
| 1 | MINOR | `slices.ts:622-641`, `vue-verify/tests/slices.spec.ts:83-92` (implementation) | The join is never checked to attach the RIGHT definition per `cls` (a join by index or simple name would stay green) | fix | Assert each joined `definition` deep-equals `graph.services.find(cls).definition` |
| 1 | MINOR | `MonkoDriver.kt:151-155` (domain) | The Mongo query text does not name the collection (AQL does), so five `{_id: @p1}` lookups in one request cannot be told apart | fix | Add `"collection": <name>` to the printed document; a collection name is not a bind value |
| 1 | MINOR | `MonkoDriver.kt:83-87` (domain; T1's own open question) | `"@p1"` erases the BSON type (an ObjectId vs a string `_id` — the classic Mongo wrong-query bug — reads the same), and reads like a literal string | fix | Typed placeholders, e.g. `"@p1:objectId"`, `"@p2:string"`, `"@p3:array[2]"` — the type, never the value (this also restores `$in` length and `$exists` truthiness) |
| 1 | MINOR | `VaultCollector.kt:102-104`, `MonkoDriver.kt:325-330` (domain m7, security 1) | The trust boundary is the generic label `json`: a later Monko path that sets `query` from raw `print()` (update, delete, aggregate), or a pre-T1 `monko:core` under a strict pin, is trusted by name and records the token; the KDoc "never by default" is false for it | fix | A driver-specific label set next to `printQuery()` (e.g. `mongo-bound`), allowlisted instead of `json`; correct the KDoc |
| 1 | MINOR | `MonkoDriver.kt:148-161` (domain m9) | `printQuery()` / `Printed` are public in a published library, used in-module only | fix | `internal` |
| 1 | MINOR | `MonkoDriver.kt:146` (domain m8) | KDoc "sort values can only be directions" is false (`{$meta: "textScore"}`) | fix | Correct the text (still no bind value) |
| 1 | MINOR | `slices.ts:651-653` (domain m3) | A slim record with a null `graphFingerprint` is joined to today's graph with no notice | fix | Treat null as "cannot verify" and say so |
| 1 | MINOR | `VaultTab.vue:117-133,180-187` (domain m4) | The database graph is today's schema shown under a historical record without saying so | fix | One line of note text |
| 1 | MINOR | `slices.ts:414`, `VaultTab.vue:146` (domain m5) | Mongo entries recorded 2026-08-24..2026-10-19 show "Bind vars 0" (values were inlined then) as a fact | fix | `json` + no query → "unknown" |
| 1 | MINOR | `TsSdkGenerateCliCommand.kt:24-25,169-198`, `TsImports.kt:35-39` (domain m6, implementation 4, security 4) | The CLI KDoc ("build plus file I/O") is now false; the hint `pnpm add vis-network` installs latest, not the tested 10.1.2; a malformed `package.json` throws a parser error naming no file; an `@sdk/…` alias would be refused as a package | fix | KDoc; print `pnpm add --save-exact <pkg>@<tested version>` (tested versions for the packages the generator contributes, `--save-exact` otherwise); wrap the parse error with the manifest path; exempt configured aliases. Workspace-root manifests: design note, not a defect |
| 1 | MINOR | `TsSdkGenerateCliCommandSpec.kt:~241` (implementation 3) | "--dry-run reports it and still writes nothing" asserts only the second half (the check-disabled mutant stays green) | fix | Capture stdout; assert the `[problem]` and the `pnpm add` line |
| 1 | MINOR | `vue-verify/fixtures/refresh.mjs`, all six fixtures (implementation 2, security 2) | The fixtures carry the build machine's hostname (`insights.server`) and the test database name; the "byte-for-byte" comment is false (re-serialised); a stale record can be picked (`readdir` order); `.pathname` breaks on spaces | fix | Rewrite `insights.server` (and the database name) to constants, regenerate; correct the comment; fail on more than one match; `fileURLToPath` |
| 1 | MINOR | `InsightsVaultBindValuesSpec.kt:298,307,309` (implementation 5) | `varsCount >= 1` and `query shouldContain "@"` are looser than known (a wrongly bound `limit` would pass) | fix | `varsCount shouldBe 1`; Arango `"== @v_"` |
| 1 | MINOR | `NetworkGraph.vue:10` (security 3) | KDoc tells operators to allow inline styles "for the tooltip" — no tooltip is ever set | fix | Correct the text (a strict CSP blocks the injected `<style>` harmlessly); optionally the `peer` build + its CSS file |
| 1 | MINOR (surplus) | `TsImports`, `UndeclaredPackagesException` (security) | New public API in `funktor:codegen` main, used in-module | fix | `internal` |
| 1 | — | `reference/` (T1's open question) | Delete it now (vue-tabs step 10)? | coordinator: keep until T4 lands | T4 ports from it; TAB-SPECS is now the current wire reference |
| 1 | — | `RuntimeCollector.kt:58` (security red-team note) | Pre-existing: ALL system properties are recorded unredacted on every request (a secret passed as `-D…` reaches the depot and the Runtime tab) | out-of-scope → follow-up | Recorded in the plan backlog with the insights production gate |

**Verdict:** round 1 NOT clean (1 MAJOR). Fix batch, then round 2 (`reviewer-xhigh`, two-phase).

### Round 2 (2026-10-19) — `reviewer-xhigh` ×3, two-phase, base `d9ffbb32`, head `f1f5acb3`

**Clean: 0 CRITICAL / 0 MAJOR.** Phase 2: every round-1 fix HOLDS in the current files (all three reviewers, each
row checked with `path:line`), including the MAJOR; no rejected CRITICAL/MAJOR existed to audit; the `reference/` and
RuntimeCollector rows accepted (the follow-up is master's `20261019-insights-production-gate.md`). The domain reviewer
disclosed a grep that hit two ledger lines in phase 1; nothing it reported came from them. Coordinator spot-checks:
`RequestTab.vue:64` (the raw tree repeats `uri`), `KarangoDriver.kt:101` (`aql` on any text), the pre-T1 driver
(`query = builder.print()`, `vars = emptyMap()`), `.npmrc:3` — all as claimed. 12 MINORs, all stand after phase 2,
none overlapping a settled row → the follow-up task `docs/tasks-archive/2026-10/20261019-insights-vue-round2-minors.md`.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 | MINOR | `graphs.ts:298-307`, `InsightsPage.vue:59`, `KontainerTab.vue:170-174`, `VaultTab.vue:139-141` (domain) | The graph is fetched once per page; after a redeploy the mismatch notice calls the cached old graph "today's", and Vault's "fetched from the server now" is false for every record after the first | follow-up | T2's Part A decision (graphs task `:44-48`): cache by fingerprint, refetch for a record naming another one |
| 2 | MINOR | `slices.ts:408-410` (domain; reopens round-1 m5 with a fact) | A pre-D1 Monko entry carries text AND `vars: {}` (pre-T1 driver: `query = builder.print()`, `vars = emptyMap()`), so it shows a false "Bind vars 0" | follow-up | m5's "no query" condition assumed the false 0 only where text was dropped — factually incomplete. Drop the `query === null` clause |
| 2 | MINOR | `MonkoDriver.kt:420-446` (domain) | A failed Mongo query records no text (set after execution); Arango sets it before | follow-up | Compute `printed` before executing. Persistence/security path → mutation-checked |
| 2 | MINOR | `ULTRA-00171 (query vars in insights):4,13-14`; `slices.ts:418` (domain) | The query-vars task (input to maintainer decision D4) is stale on four facts; any `vars` key marks an entry legacy, so recording vars later would hide all text | follow-up | Rewrite that task; note the coupling |
| 2 | MINOR | this file `:548`; graphs task `:307-316`, `:336-338`, `:488`, `:492` (domain + implementation) | Notes describe a deleted `OLD_AFTER_SECONDS`, four `finish` sources, "one change" in `refresh.mjs`, and symbols `instanceCountsMeasured` / `graphMismatch` that no longer exist | follow-up | Rewrite the notes |
| 2 | MINOR | `VaultCollector.kt:103-107`, `KarangoDriver.kt:101`, `AqlTypedQuery.kt:15` (security) | `aql` is stamped on any text, including the public raw `AqlTypedQuery.of(type, query)`; the KDoc "each label names exactly one placeholder-ising printer" is false for it | follow-up | New, not inside round-1 row m7 (Monko-only). No in-repo caller of `of(`. Give raw roots their own label, or correct the KDoc + red-team scenario |
| 2 | MINOR | `MonkoDriver.kt:73-79` (security) | Keys of a document used as a filter VALUE (embedded-document equality) are kept, so a secret used as a key is recorded | follow-up | Walk a value-position document only if it is an operator document; else bind it as `@pN:object`. Mutation-checked |
| 2 | MINOR | `vue-verify/.npmrc:3` (+ `ultra/codegen/ts-verify/.npmrc`) (security) | Node pinned at 22.12.0 (2024-12); 22.x has had security releases since | follow-up | Bump within 22.x, newest looked up online |
| 2 | MINOR | `vue-verify/tests/escaping.spec.ts:41,52` (implementation) | `>=` with `times=4`, but the page holds 5 copies (the raw tree repeats `uri`), so a tab dropping one field stays green | follow-up | Exact count, or assert per position |
| 2 | MINOR | `vue-verify/tests/graphs.spec.ts:216-221` (implementation) | Only checks dashes ⇔ weight; inverting the Lazy predicate (`graphs.ts:97`) survives | follow-up | Count Lazy injections independently |
| 2 | MINOR | `InsightsPage.vue:82-83` (implementation) | "the only place a generated SDK is compiled by a real toolchain" — false since vue-verify | follow-up | Restate |
| 2 | — | surplus (security, implementation, domain) | `--alias` has no user until `@sdk` lands; barrel exports `overrideDefinition`, `isInlinedMongoEntry`, `readDatabaseGraph`; v1's always-"not measured" counters row | maintainer list | Not defects; listed in the follow-up for a decision |

Red-team scenarios proposed (security, collected only): raw AQL under `aql`; a document value keyed by a secret; a
failed Mongo query whose server error echoes literal values into a logged message (`LogCollector.kt:32-35`, reasoned);
`enrichApiResponseWithInsights` (`funktor/rest/src/jvmMain/kotlin/respond.kt:71-84`) adding hostname + URL to API
responses, the 401 to a non-superuser included (unverified, outside this change).

**Verdict:** gate PASS — round 2 clean, e2e present and green on both backends, full suite green on `ff2a865c`
(15 864 tests, 0 failures; `f1f5acb3` is task docs only).

## History

- 2026-10-21: numbered ULTRA-00190 (was `docs/tasks/20260802-insights-vue-tabs.md`).
