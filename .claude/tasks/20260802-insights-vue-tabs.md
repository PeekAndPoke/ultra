# Insights — the Vue detail page and its collector tabs

**Status:** IN PROGRESS — started 2026-08-02
**Plan:** `.claude/tasks/20260730-frontend-sdk-vue-contributors.md` → "Insights specifics"
**Spec:** `funktor/insights/reference/TAB-SPECS.md` — all ten tabs, wire shapes read from a real record
**Security-critical:** yes. Superuser-only data, rendered from attacker-controlled strings.

## Where this actually starts from

More is done than the plan's "TODO — not started" suggests. Verified on disk 2026-08-02:

| Piece | State |
|---|---|
| REST API — `listRecords`, `getRecord` | DONE, `authFloor = { isSuperUser() }` on the group |
| Generated TS client `funktorInsightsClient.ts` | DONE |
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

Their backlog (`20260802-codegen-loop-handoff.md` §4) has "an insights contributor — the registry's
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

Written up in `.claude/tasks/20260802-insights-to-codegen-handover.md`.

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
`.claude/tasks/20260802-css-contribution-and-app-scaffold.md`; two of their findings land on this task:

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
| `vault` — the `vars` field | **BLOCKED.** `.claude/tasks/20260731-query-vars-in-insights.md` is open and needs a maintainer decision. Bind values include auth lookups — the token or activation code *being looked up*. Build the tab **without rendering `vars`**; everything else in it is fine |
| `vault` — the database graph | **CANNOT be built from a record.** It came from `DatabaseGraphBuilder`, a live kontainer service queried at render time. Needs a decision: its own live endpoint (it describes the schema, not the request) or `VaultCollector` storing `DatabaseGraphModel`. Do not promise the tab includes it |
| `app-config` | **UNBLOCKED 2026-08-02**, was not on 2026-07-31. `Redacted<T>` closed all four known leaks — verified field by field, see the corrected note in TAB-SPECS. Residual, and it must reach the docs: an *app's own* config secret in a plain `String` is still rendered verbatim |
| `kontainer` | Free. Everything the old graph drew is derivable from the record — unlike vault's |
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
- [ ] **3. `runtime`** — the seven-cell strip. Note the old bar and tab deliberately used different units
      (GB to 2dp vs `%d MB`); the tab's is the one to keep. `openFileDescriptors` is `0` on non-Unix, not
      unknown.
- [ ] **4. `vault`** minus `vars` and minus the graph. Vue sums the totals: every one was a private
      `lazy` and is **not in the record**.
- [ ] **5. `kontainer`** — the sortable table first, the graph second. It is 73.6% of a record's bytes
      (137.8 KB average), so this is the one where rendering cost is a real design input.
- [ ] **6. `app-config`** — `JsonTree` twice, with the caveat above stated in the UI, not just the docs.
- [x] **7. The detail page** — DONE 2026-08-02. Tab shell, prev/next, Overview landing section.
      The Database and Runtime strips are NOT inlined yet; they arrive with steps 3 and 4.
      **The tab registry is open by design:** an unregistered key renders through `JsonTree` rather than
      erroring, which is how an app-defined collector appears and how the four unbuilt tabs appear today.
      So the page is complete and usable now, and each later tab is an upgrade rather than an unblock.
- [x] **8. The list page** — DONE 2026-08-02. Paged `listRecords`, and the deleted insights *bar*'s job.
      Duration bands are three, not the old four: olive ("slightly slow") is dropped, because the theme
      carries three tones and a fourth colour earns less than explaining it costs.
- [x] **D-1 turned out not to block these.** The pages import `../models.ts`, `../runtime/*` and
      `../funktorInsightsClient.ts` — relative, and correct as long as the contributor emits `funktor/ui`
      at `<out>/ui/` and these at `<out>/insights/`. That is the plan's "freeze the layout" option, taken
      as an interim. When the `@sdk` alias lands the imports become alias-based and stop depending on
      depth; until then **the layout is a contract the contributor must honour**.
- [ ] **9. The contributor** — `InsightsTsContributor` in `funktor/insights`. Page routes are
      `requiresAuth = true`, **declared not derived**.

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

## What the wire actually looks like — the traps TAB-SPECS already caught

Do not re-derive these from the Kotlin types; they were read out of a real record and several differ:

- `request.method` and `response.status` are **objects**, not strings — ktor's `HttpMethod` serialises as
  `{value}` and `HttpStatusCode` as `{value, description}`.
- `request.uri` is **path only**. The computed `fullUrl` was removed; Vue composes it.
- `user.userId` is a **flat string** — the value class does not survive as an object.
- `kontainer …instances[].createdAt` is epoch **seconds as a double**.
- `log.entries[].text` is already formatted by `LogAppender.format` — render, do not re-parse.

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

**The gap, stated because it is easy to read the green above as more than it is:** SSR renders only the
synchronous state, so **the pages' LOADED state is unverified** — the table rows, the tab shell driven by
a real record, prev/next. Testing it needs a DOM (`happy-dom`/`jsdom`), which is not in the demo app's
`node_modules`, and adding a dependency there is the codegen agent's call. The tabs themselves are fully
exercised because they are pure props-in.

**Owed:** that harness lives in a scratch dir, not in the repo — it proves the code is right today and
guards nothing tomorrow. It needs a permanent home before this task can pass its gate; that is a Vue test
setup decision (vitest in `sdkgen-app`, or a dedicated harness module) which touches the codegen agent's
area, so it is not taken unilaterally. A grep-based guard is NOT a substitute but is a cheap stopgap —
note it must match `v-html=`, not the word, since every one of these files mentions it in KDoc.

## Test evidence

- [ ] `vue-tsc --noEmit` green in `funktor-demo/sdkgen-app` — the only place that proves a generated SDK
      is consumable from a real toolchain.
- [ ] A component test per tab against a **real recorded slice**, not a hand-written fixture.
      `InsightsRecordingSpec` already writes real records to a depot; take the sample from there.
- [ ] **The `v-html` guard is a test, not a review note:** assert that a slice containing
      `<img src=x onerror=alert(1)>` renders as text. Mutation-test it by switching one binding to
      `v-html` and confirming it goes red.
- [ ] e2e: the API half is already covered; this task adds no backend behaviour.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

**Red-team follow-up:** required — `.claude/tasks/20260730-redteam-insights-api.md` already exists for the
API half; extend it with the rendering scenarios (stored XSS via header/UA, secret disclosure through the
config tab) rather than opening a second file.
