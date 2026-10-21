# Insights — the Kraft (Kotlin/JS) GUI, on par with the Vue one

**Status:** PAUSED 2026-10-19 (maintainer: budget precaution before the 0.114.0 release) — started as track T4 on
`wt/insights-kraft` (worktree `/opt/dev/peekandpoke/ultra-wt/insights-kraft`). Done so far, UNVERIFIED, committed as WIP
`24e745bf`: the four endpoint declarations moved to a commonMain `InsightsApiClient`, the API models moved to commonMain,
the server mounts them. Next on resume: re-take the generated-TS baseline FROM MASTER (the implementer's copy was not
kept), regenerate on the branch, `diff -r` (must be empty or explained), run the module tests, then the pages.
Created 2026-08-24. **Maintainer decision: yes, and the two should be mostly on par.**
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

## Two loose ends from the removal, fix them here

- [ ] `funktor-demo/adminapp/.../AdminAppConfig.kt:11` — `insightsDetailsBaseUrl` still points at
      `/_/insights/details/`, a route that no longer exists. Either repoint it at this page or delete it.
- [ ] `funktor/inspect/.../DevtoolsRequestHistoryPage.kt:59-61` — a disabled link whose comment says it
      "returns when the Vue insights page can" be linked to. With this page it links internally instead.

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
      looked up*. Blocked on ULTRA-00171 (query vars in insights) (deferred by the
      maintainer 2026-08-24, still open). **And therefore no raw-slice dump and no JSON fallback on that
      tab either** — a viewer for the object defeats suppressing one field of it.
      **Incomplete since the Vue gate (2026-08-24, `18046b40`):** bind values also reached the Database tab through
      Mongo's `query` text and through Arango's EXPLAIN output (`queryExplained`), not only through `vars`. Treat all three
      as unsafe to render until the Vue task's "S1 at the source" item lands, and follow the shape `VaultCollector` records
      after it (ULTRA-00190 (insights vue tabs), "Review record").
- [ ] **Every tab degrades rather than throws.** Records outlive the code that wrote them; read
      defensively, exactly as `slices.ts` does, and fall back to a JSON tree.
- [ ] **`template`'s null render time is the NORMAL case**, not an error — it is null on every API request.
- [ ] **`app-config` displays application config.** `Redacted<T>` covers the four known framework
      secrets; an app's own secret in a plain `String` is still shown. Say so in the UI, as the Vue tab
      does — not only in a comment.
- [ ] ~~The wire traps from `TAB-SPECS.md`: `method`/`status` are objects, `uri` is path-only,
      `userId` is a flat string, kontainer `createdAt` is epoch **seconds** as a double.~~
      **STALE — do not copy this list** (marked 2026-10-19; the original is struck through, not rewritten). `TAB-SPECS.md`
      was distilled from a Jackson-era record, and the Slumber switch changed at least kontainer `createdAt` (now an object,
      no longer an epoch-seconds double) and the user `isAnonymous` / `isSystem` booleans (functions on `UserRecord`, never
      emitted; the `_type` discriminator replaces them) — found by the Vue gate, `18046b40`. The Vue task's own to-do is to
      re-derive the spec from a current record, so the other entries (`method`/`status`, `uri`, `userId`) are unverified too.
      The authority for this port is the wire contract track T2 is writing in
      `20260824-insights-graphs-and-static-slices.md`, where the kontainer slice is being reshaped (that doc had no such
      section when this note was written). Build against that and keep this box open until it exists.

## Sequencing

**After the Vue gate PASSES.** The gate has run, and it FAILED: `/feature-review` on the Vue implementation, 2026-08-24
(record: ULTRA-00190 (insights vue tabs), "Review record"). It found a live secret disclosure — the Database tab rendered a
session token through Mongo's `query` text and Arango's EXPLAIN output (`18046b40`) — a backend CRITICAL (the vault slice
could not be written at all; fixed in `e448c64d`), and stale wire claims. The frontend findings are fixed; as of 2026-10-19
what still blocks a PASS is that task's "Blocking the gate" list: S1 at the source, I4, re-deriving TAB-SPECS from a current
record, and a re-run. This port will copy the Vue design decisions — including the security ones above — so it still waits
for that PASS: reviewing after both would mean finding the same defect twice, in two languages.

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
