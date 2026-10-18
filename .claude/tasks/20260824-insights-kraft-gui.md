# Insights — the Kraft (Kotlin/JS) GUI, on par with the Vue one

**Status:** TODO — created 2026-08-24. **Maintainer decision: yes, and the two should be mostly on par.**
**Plan:** `.claude/tasks/20260730-frontend-sdk-vue-contributors.md`
**Counterpart:** `.claude/tasks/20260802-insights-vue-tabs.md` — the Vue implementation and its full
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

## Two loose ends from the removal, fix them here

- [ ] `funktor-demo/adminapp/.../AdminAppConfig.kt:11` — `insightsDetailsBaseUrl` still points at
      `/_/insights/details/`, a route that no longer exists. Either repoint it at this page or delete it.
- [ ] `funktor/inspect/.../DevtoolsRequestHistoryPage.kt:59-61` — a disabled link whose comment says it
      "returns when the Vue insights page can" be linked to. With this page it links internally instead.

## What to build

The client is generated for TypeScript but **not** for Kotlin — `InsightsApiFeature` deliberately ships
no hand-written `ApiClient` (its KDoc explains why). So decide first:

- [ ] **Does Kraft get a hand-written `InsightsApiClient`,** in the pattern of the other funktor API
      clients, or does that reopen the "codegen replaces hand-written clients" direction? This is the
      one real design question in the task; everything after it is rendering.

Then, mirroring the Vue structure so the two stay comparable:

- [ ] List page — paged `listRecords`, columns Recorded / Method / Path / Status / Duration, with the
      status and duration tone bands (red >300ms, yellow >150).
- [ ] Detail page — Overview facts, the Database and Runtime strips, prev/next, and a tab per slice.
- [ ] Ten tabs: `request`, `response`, `user`, `routing`, `runtime`, `log`, `template`, `vault`,
      `kontainer`, `app-config`.
- [ ] Mount into `mountFunktorInspect`, which is where the rest of the Kraft ops UI lives.

## Carry these across — they are not Vue-specific

The Vue implementation learned these the hard way; a fresh Kraft port will hit every one.

- [ ] **Escaping.** Every string in a slice is recorded verbatim from an unauthenticated request.
      kotlinx.html escapes text by default — the danger is `unsafe { }`, which is the exact equivalent of
      `v-html` and must not appear in these pages. The old GUI's `inlineScript` helper used it; do not
      revive that pattern for content.
- [ ] **`vault`'s `vars` must NOT be rendered.** Bind values include the token or activation code *being
      looked up*. Blocked on `.claude/tasks/20260731-query-vars-in-insights.md` (deferred by the
      maintainer 2026-08-24, still open). **And therefore no raw-slice dump and no JSON fallback on that
      tab either** — a viewer for the object defeats suppressing one field of it.
- [ ] **Every tab degrades rather than throws.** Records outlive the code that wrote them; read
      defensively, exactly as `slices.ts` does, and fall back to a JSON tree.
- [ ] **`template`'s null render time is the NORMAL case**, not an error — it is null on every API request.
- [ ] **`app-config` displays application config.** `Redacted<T>` covers the four known framework
      secrets; an app's own secret in a plain `String` is still shown. Say so in the UI, as the Vue tab
      does — not only in a comment.
- [ ] The wire traps from `TAB-SPECS.md`: `method`/`status` are objects, `uri` is path-only,
      `userId` is a flat string, kontainer `createdAt` is epoch **seconds** as a double.

## Sequencing

**After the Vue gate.** `/feature-review` has not yet run on the Vue implementation, and this port will
copy its design decisions — including the security ones above. Reviewing after both would mean finding
the same defect twice, in two languages. See the note in the Vue task.

**Also after `20260824-insights-graphs-and-static-slices.md`**, or at least aware of it: that task
changes the kontainer slice's wire shape and adds two endpoints. Building this against the fat shape
means rewriting the kontainer tab twice.

## Verification

- [ ] Tests in `kraft/core-tests` or the module's own `jsTest` via `TestBed.preact { }` — real browser
      behaviour, not mocks.
- [ ] **An escaping test with a real payload**, mirroring the Vue harness: a slice containing
      `<img src=x onerror=…>` must render as text. Mutation-test it.
- [ ] A `vault` slice carrying `vars` must not put the value on the page — mutation-tested by
      reinstating a dump and confirming it goes red.
