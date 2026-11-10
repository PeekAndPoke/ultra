# ULTRA-00198: Insights — the Kraft (Kotlin/JS) GUI, on par with the Vue one

> **Status (2026-11-10): IN PROGRESS.** PAUSED 2026-10-19 (maintainer: budget precaution before the 0.114.0 release) — started as track T4 on `wt/insights-kraft` (worktree `/opt/dev/peekandpoke/ultra-wt/insights-kraft`). Done so far, UNVERIFIED, committed as WIP `24e745bf`: the four endpoint declarations moved to a commonMain `InsightsApiClient`, the API models moved to commonMain, the server mounts them. Next on resume: re-take the generated-TS baseline FROM MASTER (the implementer's copy was not kept), regenerate on the branch, `diff -r` (must be empty or explained), run the module tests, then the pages. Created 2026-08-24. **Maintainer decision: yes, and the two should be mostly on par.**
>
> **Part of:** ULTRA-00253 (Insights epic)
>
> **DECIDED 2026-11-09 (maintainer):** parity with the old GUI on both Vue and Kraft, as far as possible. Query values are shown (ULTRA-00171). The git version and environment id go in the list page header and the detail Overview. Kraft uses its existing Semantic UI icons and the `prismjs` addon. On resume: carry the one WIP commit `24e745bf` over to master and verify it there instead of rebasing a branch 448 commits behind.

> **2026-11-10:** ULTRA-00301 DONE (one insights switch for the whole app; the GUI talks to the API on `api.*`): unblocked.
>
> **DECIDED 2026-11-10 (maintainer):** the graphs get a Kraft vis-network addon ("a Kraft addon for the graph is
> generally a good thing"); raw JSON views are pretty JSON through `PrismJson`, not a collapsible tree ("Yes to
> PrismJson").
>
> **Status 2026-11-10 (evening): BUILT, tests green, review gate next.** Open question for the maintainer: per-record
> links from the request history (below).

**Plan:** ULTRA-00153 (frontend sdk vue contributors)
**Counterpart:** ULTRA-00190 (insights vue tabs) — the Vue implementation and its full
gap inventory against the old kotlinx.html GUI.
**Security-critical:** yes. Same data, same attacker-controlled strings, same superuser-only floor.

## This amends a standing decision — deliberately

The plan's decision table says **"Vue only, no second target"** and **"kotlinx.html renderers are
DELETED, not ported"**. This task is a second target for the same view, so the decision is amended
rather than quietly contradicted: **the ops views ship for BOTH Kraft and Vue while Kraft frontends
exist.** Recorded here and in the plan so nobody later "corrects" one of them back.

Note what is NOT amended: the old *kotlinx.html server-rendered* GUI stays deleted. This is a Kraft SPA
page, like the rest of `funktor/inspect/src/jsMain`, not a revival of `InsightsGuiTemplate`.

## There has never been a Kraft insights UI

Verified 2026-08-24. The old one was **server-rendered kotlinx.html** — `InsightsGuiRoutes` mounted ktor
routes that rendered `InsightsGuiTemplate`. `funktor/inspect/src/jsMain` is the Kraft ops UI and covers
cluster, introspection and logging; it has **no insights pages at all**.

So this is a new page, not a port of Kraft code. The two things that DO exist as inputs:

- `funktor/insights/reference/` — the old renderers, verbatim, plus `TAB-SPECS.md`.
- The Vue implementation, which already made every layout decision once.

## Progress (2026-11-10)

- [x] **Step 1, the WIP carried over** (not rebased; `24e745bf` re-done on master by hand, since the models and API
      changed a lot after it): `InsightsApiClient` in commonMain with all five endpoints (incl. `GetServerInfo`,
      ULTRA-00190), the models and graph models moved to commonMain (`InsightsServerInfo.of` stays jvm, as an
      extension), `InsightsApi` mounts the client's declarations, `InsightsApiFeature`'s KDoc corrected. **Verified:**
      the generated TS SDK regenerated and byte-identical to the one before (`diff -r`), vueVerify 99/99, insights,
      funktor-all green, `funktor:inspect` compiles for JS.

- [x] **Step 2, the addons** (`b9060d6f`, `33c05401`): `kraft/addons/visnetwork` (vis-network 10.1.4; the component
      loads the library itself when the app registered no addon, so the ops UI needs nothing from its host app),
      mutants V1-V5 red. Prism: `PrismAql`, a new `code` re-highlights. **Found and fixed on the way:** every lazily
      loaded Prism language and plugin failed to resolve (the imports lacked `.js`, which webpack requires from an ES
      module), in every app: only Prism's built-in languages ever highlighted. Mutants P1-P4 red.
- [x] **Step 3, the pages** in `funktor/inspect/src/jsMain/kotlin/insights/`: `InsightsUi` (optional in
      `FunktorInspectUi(insightsApi = ...)`, mounted at `/_/funktor/inspect/insights`, a card on the overview),
      `slices.kt` (the readers, a port of `slices.ts`), `graphs.kt` (networks, options, the graph cache per UI),
      list page, detail page, ten tabs with the old GUI's Semantic UI icons. **Verified:** `InsightsPagesSpec` 6/6 in
      ChromeHeadless against a MockEngine server; mutants: header value and list path as markup, unreadable vault
      shown raw, withheld ignored, no graph cache, no reload on a new record, legacy plan shown: all red. Legacy query
      text has two guards (reader and tab); each single mutant survives, the double mutant is red. **The fake server
      found a real bug:** inside `jsObject<dynamic> { }` the name `NetworkKind` resolved as a property of the JS
      object (undefined), so every graph threw; the per-kind values are now computed outside.

## Two loose ends from the removal, fix them here

- [x] `funktor-demo/adminapp/.../AdminAppConfig.kt:11` — `insightsDetailsBaseUrl` still points at
      `/_/insights/details/`, a route that no longer exists. Either repoint it at this page or delete it.
      **Deleted** (nothing read it); the demo admin passes `insightsApi = InsightsApiClient(Apis.config)` (`api.*`)
      and has an "Insights" entry in its admin menu.
- [ ] `funktor/inspect/.../DevtoolsRequestHistoryPage.kt:59-61` — a disabled link whose comment says it
      "returns when the Vue insights page can" be linked to. With this page it links internally instead.
      **Not possible as it stands:** `ApiResponse.Insights` carries no record reference, since the record is written
      after the response. A per-record link needs the server to name the record (bucket and file) at call setup and
      `ApiResponse.Insights` to carry it: a wire change in `ultra:remote`, so the maintainer's call. The comment now
      says so.

## What to build

The client is generated for TypeScript but **not** for Kotlin — `InsightsApiFeature` deliberately ships
no hand-written `ApiClient` (its KDoc explains why). So decide first:

- [x] DECIDED 2026-10-19 (maintainer): **yes — a Kotlin `InsightsApiClient` in `funktor/insights` commonMain**, in the
      `AuthApiClient` pattern: the four endpoint declarations move onto it, the server mounts them (so the generated TS
      client reads the same single source), and `InsightsApiFeature`'s "no hand-written ApiClient" KDoc is corrected.
      Was: **Does Kraft get a hand-written `InsightsApiClient`,** in the pattern of the other funktor API
      clients, or does that reopen the "codegen replaces hand-written clients" direction? This is the
      one real design question in the task; everything after it is rendering.

Then, mirroring the Vue structure so the two stay comparable:

- [x] List page — paged `listRecords`, columns Recorded / Method / Path / Status / Duration, with the
      status and duration tone bands (red >300ms, yellow >150).
- [x] Detail page — Overview facts, the Database and Runtime strips, prev/next, and a tab per slice.
- [x] Ten tabs: `request`, `response`, `user`, `routing`, `runtime`, `log`, `template`, `vault`,
      `kontainer`, `app-config`.
- [x] Mount into `mountFunktorInspect`, which is where the rest of the Kraft ops UI lives.

## Carry these across — they are not Vue-specific

The Vue implementation learned these the hard way; a fresh Kraft port will hit every one.

- [x] **Escaping.** Every string in a slice is recorded verbatim from an unauthenticated request.
      kotlinx.html escapes text by default — the danger is `unsafe { }`, which is the exact equivalent of
      `v-html` and must not appear in these pages. The old GUI's `inlineScript` helper used it; do not
      revive that pattern for content.
- [x] ~~**`vault`'s `vars` must NOT be rendered.**~~ **Reversed by ULTRA-00171** (maintainer 2026-11-09: "query
      values: show them again"): values are shown except where the server withheld them; legacy slices still show
      no text, and an unreadable vault slice has no raw view. Was: Bind values include the token or activation code *being
      looked up*. Blocked on ULTRA-00171 (query vars in insights) (deferred by the
      maintainer 2026-08-24, still open). **And therefore no raw-slice dump and no JSON fallback on that
      tab either** — a viewer for the object defeats suppressing one field of it.
      **Incomplete since the Vue gate (2026-08-24, `18046b40`):** bind values also reached the Database tab through
      Mongo's `query` text and through Arango's EXPLAIN output (`queryExplained`), not only through `vars`. Treat all three
      as unsafe to render until the Vue task's "S1 at the source" item lands, and follow the shape `VaultCollector` records
      after it (ULTRA-00190 (insights vue tabs), "Review record").
- [x] **Every tab degrades rather than throws.** Records outlive the code that wrote them; read
      defensively, exactly as `slices.ts` does, and fall back to a JSON tree.
- [x] **`template`'s null render time is the NORMAL case**, not an error — it is null on every API request.
- [x] **`app-config` displays application config.** `Redacted<T>` covers the four known framework
      secrets; an app's own secret in a plain `String` is still shown. Say so in the UI, as the Vue tab
      does — not only in a comment.
- [x] **Resolved 2026-11-10:** the Kraft readers are a port of the Vue `slices.ts` as ULTRA-00190 left it (current
      wire shapes, both kontainer shapes, both `createdAt` shapes). Was: ~~The wire traps from `TAB-SPECS.md`: `method`/`status` are objects, `uri` is path-only,
      `userId` is a flat string, kontainer `createdAt` is epoch **seconds** as a double.~~
      **STALE — do not copy this list** (marked 2026-10-19; the original is struck through, not rewritten). `TAB-SPECS.md`
      was distilled from a Jackson-era record, and the Slumber switch changed at least kontainer `createdAt` (now an object,
      no longer an epoch-seconds double) and the user `isAnonymous` / `isSystem` booleans (functions on `UserRecord`, never
      emitted; the `_type` discriminator replaces them) — found by the Vue gate, `18046b40`. The Vue task's own to-do is to
      re-derive the spec from a current record, so the other entries (`method`/`status`, `uri`, `userId`) are unverified too.
      The authority for this port is the wire contract track T2 is writing in
      ULTRA-00197 (insights graphs and static slices), where the kontainer slice is being reshaped (that doc had no such
      section when this note was written). Build against that and keep this box open until it exists.

## Sequencing

**After the Vue gate PASSES.** The gate has run, and it FAILED: `/feature-review` on the Vue implementation, 2026-08-24
(record: ULTRA-00190 (insights vue tabs), "Review record"). It found a live secret disclosure — the Database tab rendered a
session token through Mongo's `query` text and Arango's EXPLAIN output (`18046b40`) — a backend CRITICAL (the vault slice
could not be written at all; fixed in `e448c64d`), and stale wire claims. The frontend findings are fixed; as of 2026-10-19
what still blocks a PASS is that task's "Blocking the gate" list: S1 at the source, I4, re-deriving TAB-SPECS from a current
record, and a re-run. This port will copy the Vue design decisions — including the security ones above — so it still waits
for that PASS: reviewing after both would mean finding the same defect twice, in two languages.

**Also after ULTRA-00197 (insights graphs and static slices)**, or at least aware of it: that task
changes the kontainer slice's wire shape and adds two endpoints. Building this against the fat shape
means rewriting the kontainer tab twice.

## Verification

- [x] Tests in `kraft/core-tests` or the module's own `jsTest` via `TestBed.preact { }` — real browser
      behaviour, not mocks.
- [x] **An escaping test with a real payload**, mirroring the Vue harness: a slice containing
      `<img src=x onerror=…>` must render as text. Mutation-test it.
- [x] A `vault` slice carrying `vars` must not put the value on the page — mutation-tested by
      reinstating a dump and confirming it goes red.

## Review record

Gate `/feature-review`, change set = file set of `38b70eba`, `b9060d6f`, `33c05401`, `134acab5` plus the fix batch
(base: master before `38b70eba`).

**Round 1 — level 1 (`reviewer-high`), blind, 2026-11-10. NOT CLEAN (1 MAJOR, found by two charters).**

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `insights/KontainerTab.kt:182`, `VaultTab.kt:107`, `graphs.kt` (+ `VisNetworkComponent.kt:64`) | Graph data and options rebuilt per render; a sort click or "Database Graph Data" calls `setData`/`setOptions`, the graph is laid out again (impl + domain) | **fix** | Confirmed by experiment first: a `setData` spy saw 1 call per sort click. Fixed: `NetworkGraph` builds its options once; both tabs keep the built graph until kind or loaded graph changes. Mutants F1-F3 red (spy counts `setData` and `setOptions`) |
| 1 | MINOR | `InsightsPagesSpec.kt:254` | Unknown-collector test passed on the stale Request tab (impl) | **fix** | Waits for the Request content to go, asserts the highlighted `"note"` property in the panel; mutant F4 (unknown key renders nothing) red |
| 1 | MINOR | `InsightsPagesSpec.kt` | Most tabs, the database graph, failure states untested (impl) | **fix (part)** | Added: database graph fetched once per UI and kept on re-render (F6 red), template null render time, older shapes. Remaining tabs' degradation untested: maintainer list |
| 1 | MINOR | `InsightsListPage.kt:115` | Failed page load has no way out (impl) | **fix** | Retry button, and Previous when past page 1 |
| 1 | MINOR | `VaultTab.kt:180`, `slices.kt:326`, `render.kt:46` | Three pretty `Json` instances, one per render (impl) | **fix** | One `prettyJson` in `slices.kt` |
| 1 | MINOR | `VisNetworkComponentSpec.kt:103` | Problem-box assertion after unmount cannot fail (impl) | **fix (part)** | Asserted while mounted instead; a failing-creation case is not added (vis-network logs invalid options rather than throwing): maintainer list |
| 1 | MINOR | `slices.kt`, `render.kt`, `VaultTab.kt` | SURPLUS: `readKontainerGraph`, `isNullOrJsonNull`, `collapsed(open)`, `networkGraph(onReady)`, `Tone.color()` unused (impl, domain) | **fix** | Removed; grep for the names is empty |
| 1 | MINOR | `InsightsUi.kt:42` + demo `webpack.js:24` | Reload of a record page (`….json`) fails on the dev server: dot rule (domain) | **fix** | Demo admin `historyApiFallback = { disableDotRule: true }` |
| 1 | MINOR | `Prism.kt:207`, `PrismJsInternals.kt:30` | Prism loads in auto mode (highlights every `language-*` of the page); output replaces preact-owned DOM (domain) | **maintainer list** | Pre-existing addon behaviour; part 1 unverified. A fix (manual mode, own child element) is an addon change with no deterministic test here |
| 1 | MINOR | `InsightsPagesSpec.kt` | No test for older shapes (domain) | **fix** | Full kontainer slice with epoch-seconds `createdAt` (F5 red), Monko `json` entry (F7 red) |
| 1 | MINOR | `InsightsUi.kt:40` | Default `InsightsRoutes` mount = the API base path (domain) | **fix** | Default `/_/funktor/inspect/insights` |
| 1 | MINOR | `kotlin-js-store/yarn.lock` | vis-network not in the committed lock (security) | **maintainer list** | The lock is regenerated at release (`yarnLockAutoReplace`); version pinned exactly |
| 1 | MINOR | `PrismJsInternals.kt:49` | Theme CSS from cdnjs (with SRI) (security) | **maintainer list** | Pre-existing addon behaviour |
| 1 | MINOR | `slices.kt:345`, sorts, `FunktorInspectRoutes.kt:4` | 4-space vs Vue's 2-space values indent; bare word parsed as JSON; code-point sort vs `localeCompare`; import order (impl) | **fix (imports) / maintainer list (rest)** | Display-only differences |

Probed CLEAN in round 1 (all three charters): every reader against `slices.ts` and the current collectors; no `unsafe {}`;
Prism's innerHTML path takes a text node; vis-network string titles go to `innerText`; route params re-encoded; the
auth floor and `requireActiveInsights` unchanged; legacy and withheld vault data never shown; 32k highlight cap.

## History

- 2026-10-21: numbered ULTRA-00198 (was `docs/tasks/20260824-insights-kraft-gui.md`).
- 2026-11-10: IN PROGRESS.
- 2026-11-10: addons (`b9060d6f`, `33c05401`) and the pages built; review gate next.
