# kraft chartjs: live updates that keep the whole chart in step

**Status:** DONE (archived 2026-10-18) (requested by the maintainer from the spross project, 2026-10-18, for the release
that carries `20261017-kronos-advanceby-collapse.md`)
**Security-critical:** no (a UI component; no authz, no user input reaches Chart.js unescaped)

## Background

`ChartJsComponent` (`kraft/addons/chartjs/src/jsMain/kotlin/ChartJsComponent.kt`) creates the Chart.js
chart once, on mount, from `props.data`. Every later props change goes through `shouldRedraw`, which
copies `options` and each dataset's `data` array onto the live chart and calls `c.update()`. Nothing
else of the new config reaches the chart.

Seen in spross's dashboard: a line chart over a scrolling 3-day window of samples, re-rendered with a
fresh `ChartConfig` on every new sample (category x-axis, `data.labels` = the samples' local times).
The lines move, the x-axis keeps the labels it had at mount, so once the window scrolls the values
are drawn against the wrong times. spross works around it with a numeric x-axis (points carry their
own `x`), which only hides the bug.

Chart.js is 4.5.1 (`buildSrc/src/main/kotlin/Deps.kt:444`).

## Spec

- [x] **Labels follow the props** (the reported bug): `shouldRedraw` assigns
      `c.data.labels = nextProps.data.data.labels` before `c.update()`. A chart whose labels change
      shows the new labels after the update.
- [x] **A dataset added later appears.** Today the `else` branch of the merge reads
      `currentData[sid].data` where `currentData[sid]` is `undefined`: a TypeError, caught and logged
      by the `try`, and the new series never shows. Push the new dataset instead.
- [x] **A dataset removed later disappears.** When the next config has fewer datasets, the surplus
      stays on the chart today. Truncate `c.data.datasets` to the next length.
- [x] **A dataset's other fields follow too**, not only `data`: `label`, `borderColor`,
      `backgroundColor`, `fill`, `yAxisID`, `stepped`, ... are ignored after mount today (a legend
      that renames a series keeps the old name). Copy the next dataset's own properties onto the
      existing dataset object (`Object.assign`-style), so Chart.js keeps the dataset's metadata and
      animates from the old values rather than redrawing from scratch.
- [x] **A changed chart `type` rebuilds the chart** (destroy + create). Chart.js 4 could switch the type of a
      live chart, but a rebuild gives clean state and no animation across chart types.
- [x] **`Chart.update` typed for Chart.js 4**: the external declaration says
      `update(duration: Number?)`, the Chart.js 2 signature. Chart.js 3 and 4 take
      `update(mode?: string)` (`"none"`, `"resize"`, `"reset"`, `"active"`, `"hide"`, `"show"`,
      `"default"`). Fix the declaration (a `String?` or a small set of constants).
- [x] **Decide: an update mode for live charts.** A chart fed many times per second animates every
      update. A caller can already set `options.animation = false` (the options are merged); decide
      whether `ChartJs(data, updateMode = ...)` is worth adding on top, or document the options route
      in the KDoc. Recommendation from spross: document, no new parameter.
- [x] **Decide: side effects in `shouldRedraw`.** The merge runs inside a predicate, before the
      component has decided to redraw. Moving it into the props-change path (or `onUpdate` with the
      previous props) would be cleaner; keep it if kraft has no better hook, with a comment saying why.
- [x] Update the example (`kraft/examples/addons/src/jsMain/kotlin/chartjs/ChartJsExample.kt`): one
      chart whose labels scroll (a ticker shifting a window), so the fix is visible by eye.

## Acceptance

- `ChartJsAddonSpec` (or a new `ChartJsComponentSpec`) mounts a chart, re-renders with: new labels;
  one more dataset; one fewer; a renamed and recolored dataset; a different `type`. After each, the live
  `Chart`'s `data.labels`, `data.datasets.length`, the dataset fields and `config.type` match the new
  props. If a real canvas is not available in the JS test runner, test the merge as a pure function
  over plain JS objects (extract it from the component) and keep the component as a thin caller.
- Each new test mutation-checked: remove the labels line, restore the old `else` branch, skip the
  truncation, copy only `data`, skip the rebuild on type change; each must turn a test red.
- spross can drop its workaround: a category axis with scrolling labels stays correct. (spross will
  keep the numeric axis anyway, for uneven sampling, but checks the category case once by hand.)

## Implementation notes

Plan confirmed with the maintainer 2026-10-18, including deleting fields the next dataset no longer has.

- **Corrections to this task's Background/Spec (verified against Chart.js 4.5.1 source):** options are
  REPLACED, not merged (`c.options = …`; Chart.js `set options` assigns wholesale) — `animation = false`
  works only because each new config carries it, which the `ChartJs` KDoc now says. Chart.js 4 CAN switch a
  chart's type on a live chart (`Config` has a `type` setter, `update()` re-resolves options by type); a
  changed `type` rebuilds the chart anyway, by choice: clean state, no animation across chart types.
- `kraft/addons/chartjs/src/jsMain/kotlin/ChartJsComponent.kt` — the merge moved from `shouldRedraw` into
  `lifecycle.onNextProps` (kraft has the hook: `kraft/core/src/jsMain/kotlin/components/Component.kt:231`);
  `shouldRedraw` is the default again. A changed `type` destroys and recreates the chart.
- `kraft/addons/chartjs/src/jsMain/kotlin/chartjs_merge.kt` — `Chart.mergeFrom(next)`: options replaced,
  labels assigned, datasets merged by index onto the EXISTING objects (Chart.js keys its per-dataset state
  by object identity, `getDatasetMeta`), new ones pushed, surplus truncated. Fields absent from the next
  dataset are deleted EXCEPT `_`-prefixed keys: Chart.js plugins write onto dataset objects (decimation keeps
  `_data`/`_decimated` and turns `data` into an accessor; deleting them empties the data once it shrinks
  below the threshold). The colors plugin's `borderColor`/`backgroundColor` may be deleted — it re-applies
  them on every update.
- **Round-2 MINORs, all applied (maintainer 2026-10-18), datasets stay matched by index:** `_data` is taken
  only when `data` is the decimation accessor (JSON cannot forge one); every `data` field except `datasets`
  follows and is copied (`xLabels`, `yLabels`, …); a dataset that stops being decimatable is un-decimated before
  the merge (`undoDecimation`, mirrors Chart.js `cleanDecimatedDataset`); `ChartJs` KDoc states the one-level
  copy, `_` keys reserved, inline `plugins` read at creation, legend state staying with the index.
- **The chart owns its config** (`ownedCopy`, `chartjs_merge.kt`): Chart.js keeps the objects it is given and
  writes into them (`initOptions` sets `options.scales`/`plugins`, listeners patch data arrays, the merge itself
  rewrites datasets), so the chart is created from, and merged from, a copy: config shell, data, labels, datasets
  (without `_` keys, raw data from `_data` if decimated), data arrays, options. A config mutated in place is
  therefore not observed — pass a new one (KDoc). Creation is wrapped: Chart.js registers a chart on the canvas
  before its first update can throw, so a failed create destroys that orphan (`ChartJsAddon.getChart`).
- `chartjs.kt` — `Chart.update(mode: String?)` (Chart.js 4 update modes) and `val config: ChartConfig`.
- Update mode: no new parameter; documented the options route in the `ChartJs` KDoc (spross's recommendation).
- Example: "Scrolling Line Chart" card in `kraft/examples/addons/src/jsMain/kotlin/chartjs/ChartJsExample.kt`.
- Found, out of scope: the existing bar/line examples use Chart.js 2 `scales.yAxes`, which in Chart.js 4
  creates an extra 0–1 axis. For the docs follow-up.

## Test evidence

- [x] `ChartJsComponentSpec` (8 specs, real Chart.js in ChromeHeadless, live chart via `Chart.getChart(canvas)`):
      labels; dataset added; removed; own fields on the same object; absent field deleted; live objects
      re-passed reordered; type change rebuilds; decimated dataset shrinking below its threshold.
- [x] Mutation-checked (first pass), all RED on the named spec, restored + `cmp`: no labels line; old `else`
      branch; no truncation; copy only `data`; no rebuild on type change; no delete of absent fields; no
      shallow copy; replacing the dataset object instead of merging into it. ("Deleting `_` keys too" was RED
      on the shrink spec until `undoDecimation` landed; since then it is pinned by the dedicated
      "plugin state in `_` fields survives an update" spec — RED there.)
- [x] **Final tree:** ChartJsComponentSpec 17/17, ChartJsAddonSpec 2/2 (XML, after the round-3 MINOR fixes).
- [x] After the round-2 MINOR batch: ChartJsComponentSpec 16/16, ChartJsAddonSpec 2/2 (XML).
- [x] `:kraft:addons:chartjs:jsTest` green after round-1 fixes — ChartJsComponentSpec 11/11, ChartJsAddonSpec 2/2
      (XML); `:kraft:examples:addons:compileKotlinJs` green; repo compile sweep zero `^e:` (before round 1).
- [x] Example checked in headless Chrome (full-page PDF print at 4 s and 9 s): x-axis labels move with the
      values (`t-7…t1` → `t-2…t6`).
- Trap hit while writing the spec: `x shouldBe y` on a `dynamic` receiver compiles to a dynamic member call
  `x.shouldBe(y)` → `TypeError` at runtime. Cast to a typed value first.

## Review record — the LEDGER

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `ChartJsComponent.kt:104-107`, `chartjs_merge.kt:12,22` | Chart.js keeps the caller's config object (`initConfig`/`initData`, chart.js:5348-5379), so the merge writes labels/datasets/fields into the CALLER's mount-time config: re-passing a cached config A after B shows B's content | fix | Verified by reading; also found Chart.js `initOptions` writes `options.plugins`/`options.scales` onto the caller's options. The component now creates and merges from copies it owns (config shell, data, labels, datasets, data arrays, options). Spec + mutant |
| 1 | MINOR | `ChartJsComponent.kt:77-80`, `:96-108` | A rebuild whose `new Chart` throws (unregistered type, missing date adapter) leaves an orphan chart registered on the canvas (chart.js:5689 before 5698); every later create hits "Canvas is already in use", uncaught, and never recovers | fix | Verified by reading. Creation wrapped; on failure the orphan from `Chart.getChart(canvas)` is destroyed and logged. Spec + mutant |
| 1 | MINOR | `chartjs_merge.kt:43,47` | Copying a live DECIMATED dataset reads the `data` accessor (→ `_decimated`) and writes it back as raw data, shrinking `_data` on every re-pass | fix | Verified (chart.js:7608-7619). The owned copy skips `_` keys and takes `data` from `_data`. Spec + mutant |
| 1 | MINOR | `ChartJsComponent.kt:57` | A config mutated in place and re-rendered no longer reaches the chart (`onNextProps` fires only when the props differ by reference); the old `shouldRedraw` merge ran on every parent render | fix (KDoc) | Behaviour change is intended (declarative props); no in-repo or spross caller mutates in place (reviewer grep). `ChartJs` KDoc now says to pass a new config |
| 1 | MINOR | `ChartJsComponent.kt:78`, task Spec | "Chart.js cannot switch a live chart's type" is false: `Config` has a `type` setter (5389) and `update()` re-resolves options by type | fix (comment + task text) | Verified. Rebuild kept as a choice: clean state, no cross-type animation |
| 1 | MINOR | `chartjs_merge.kt:9-28` | Top-level inline `plugins` of a next config are not merged (Chart.js reads the mount-time `config.plugins`) | reject | `ChartConfig` (`chartjs_models.kt`) has no `plugins` member — reachable only through `asDynamic()`, outside the component's typed contract |

Round-1 fix batch: `ownedCopy` (`chartjs_merge.kt`) used at create and in every merge; creation wrapped with
orphan cleanup via `ChartJsAddon.getChart` (internal); KDoc + comment corrected. 3 new specs (caller configs
untouched incl. data arrays and options; re-passed decimated dataset keeps raw data; failed build recovers).
Mutation-checked: create with caller config, merge with caller options, decimated accessor copied, no data-array
slice, merge without owned copy → each RED on the named spec; no orphan cleanup → RED, but reported as a Kotest
EXECUTOR failure (the orphan's async update throws `"no-such-type" is not a registered controller` uncaught), not
on the named spec; labels array not copied → SURVIVES (Chart.js only appends to labels transiently via the
category scale's `_addedLabels`; guarded by reading). The decimation fixture first could not tell decimated from
full (min-max keeps ~4 points per pixel column: 200 points on 400px stayed ~200) — switched to LTTB, samples 20.
All 9 earlier mutants re-run against the restructured merge: all RED.

Probed CLEAN in round 1: prototype pollution through the merge (own enumerable keys only, `__proto__` never becomes
an own key), `js()` strings constant, no XSS path (labels reach canvas `fillText` only), cleanup on rebuild/unmount
and truncation (metas destroyed, array listeners detached), no per-update cache growth, colors plugin re-applies
deleted colours, options replaced wholesale is supported by `update()`, kraft `onNextProps` sees the new props,
`Chart.getChart(canvas)` test approach sound, expected values independent. SURPLUS: only the pre-merge copy
(justified, tested).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 | MINOR | `chartjs_merge.kt:79` | `ownedDataset` swaps `data` for ANY own `_data` key, not only the decimation plugin's; a JSON dataset carrying `_data` plots that instead | maintainer-decision (MINOR list) | Real: the design says "if decimated"; check the `data` accessor (JSON cannot create one) |
| 2 | MINOR | `ChartJsComponent.kt:19`, `chartjs_merge.kt:8-9`, `ChartJsComponentSpec.kt:277` | "Not observed" / "never written to" overclaim: the copy is one level deep; point objects and nested option objects stay shared, are picked up at Chart.js's own next update (resize, legend), and with `parsing: false` + stacking Chart.js writes `_stacks` into the caller's points (chart.js:515, 719-722) | maintainer-decision (MINOR list) | Real (all three charters); deep copy rejected by reviewers on cost; reword |
| 2 | MINOR | `chartjs_merge.kt:16` | The labels copy has no test (survivor U6); a category value missing from `labels` makes Chart.js push into the array (chart.js:10017) | maintainer-decision (MINOR list) | Real; a kill test is cheap |
| 2 | MINOR | `chartjs_merge.kt:78` | Caller-set `_`-prefixed dataset fields are now dropped (a tooltip callback reading `ctx.dataset._unit` gets undefined) | maintainer-decision (MINOR list) | Side effect of the round-1 decimated-copy fix; KDoc note or narrower strip |
| 2 | MINOR | `chartjs_merge.kt:15-16,37` | `data.xLabels`/`yLabels` neither copied nor followed — the reported bug in its x/yLabels form | maintainer-decision (MINOR list) | Untyped in `ChartData` (same reason as the inline-plugins rejection), but cheap to follow |
| 2 | MINOR | `chartjs_merge.kt:60-67` | Decimation accessor goes stale when the plugin bails without cleaning (series switched to `bar`, `parsing` on, non-linear x): `data` keeps reading the old decimated subset | maintainer-decision (MINOR list) | Real (chart.js:7585-7600); also Chart.js's own behaviour for `dataset.data = x`. Fix would delete `_data`/`_decimated` before merging |
| 2 | MINOR | merge by index | Legend-hidden state (`meta.hidden`, keyed by dataset object) moves to whichever series lands on that index | maintainer-decision | HEAD merged by index too; matching by `label` is the alternative |
| 2 | MINOR | `ChartJsComponent.kt` | Inline `plugins` fixed at creation | withdrawn (both raisers) | Overlaps the settled round-1 rejection |
| 2 | MINOR | task Spec | "Chart.js cannot switch a live chart's type" still in the Spec bullet | fix | Rewritten (not appended) |

Round 2 phase 2: all three confirmed every round-1 fix holds (MAJOR on create, merge and rebuild paths). Consumer
note for spross (domain): `DashboardPage.kt:247-251` sets no `animation = false` while publishing every frame.

**Round 2: 0 CRITICAL, 0 MAJOR → clean; loop terminates.** MINOR list handed to the maintainer, who chose to
apply all four fix groups and keep index matching. Batch: 5 new specs (16 total); mutation-checked — `_data`
swapped without the accessor check, no `undoDecimation`, labels not copied, only `labels` followed, absent data
fields not deleted, `xLabels` array not copied (first SURVIVED: no spec fed the x-axis a missing category; spec
added, then RED) → each RED on the named spec. Reviewed in round 3 (tiered: one `reviewer-xhigh`, all charters).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 3 | MINOR | `ChartJsComponentSpec.kt:236`, task Test evidence | Since `undoDecimation`, the `_`-key survival in `replaceOwnProperties` is untested and the shrink spec's comment / "deleting `_` keys → RED" are false | fix | Comment reworded; new spec pins third-party plugin state in `_` fields; removing the `_` check → RED on it; evidence line rewritten |
| 3 | MINOR | `chartjs_merge.kt:99-100` | `undoDecimation` KDoc "only when the data shrinks" is false (wider canvas, narrowed x-bounds also clean up) | fix | Reworded to "visible points no longer exceed its threshold" |

**Round 3 (tiered: one `reviewer-xhigh`, all charters, blind copy): 0 CRITICAL, 0 MAJOR → clean.** CLEAN:
`undoDecimation` equals Chart.js `cleanDecimatedDataset` for every reachable state (and restores more when the
algorithm threw), runs before the plugin reinstalls within the same synchronous `update()` (no flicker),
negligible cost; `replaceDataFields` never removes anything Chart.js needs; accessor check unforgeable from JSON;
security clean. Survivor noted, not chased: `undoDecimation`'s restored `value: raw` is immediately overwritten
by the merge, so a `value: undefined` mutant survives (behaviour-neutral).

**Verdict: gate PASS** (2026-10-18). Rounds: 1 (1 MAJOR fixed), 2–3 clean; all MINORs dispositioned by the
maintainer. Not security-critical → no red-team task. **Docs:** written in this session against the settled
contract — `docs-site/src/pages/ultra/kraft/addons.astro` (chartjs section) and `docs-site/src/data/llms/kraft.md`,
reviewed together with the Kronos docs (`docs/tasks-archive/2026-10/20261018-docs-kronos.md`); no separate
docs follow-up.

## History

- 2026-10-18: written from spross (`spross/app-web/MEMORY.md`, "Known gap in kraft's chart addon").
- 2026-10-18: implemented, gate PASS after 3 rounds, archived. Consumer note for spross: its dashboard sets no
  `animation = false` while publishing every frame (`DashboardPage.kt:247-251`).
