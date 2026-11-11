# ULTRA-00303: Docs: the insights pages for Kraft and Vue (replace InsightsGui)

> **Status (2026-11-11): DONE.** Review gate PASS 2026-11-11 (three rounds and a final check; ledger in ULTRA-00304). `insights.astro` rewritten (steps 1-3); `staticweb.astro` waits on ULTRA-00305. The DOCS follow-up of ULTRA-00198 (Kraft insights GUI); the maintainer said "yes please update the docs" on 2026-11-11.
>
> **Part of:** ULTRA-00253 (Epic: Insights)

## Why this exists

The docs still teach the server-rendered `InsightsGui`, which was deleted:
- `docs-site/src/pages/ultra/funktor/insights.astro:42-56`
- `docs-site/src/pages/ultra/funktor/staticweb.astro:53`
- the LLM mirror `docs-site/src/data/llms/*.md` where it repeats them.

The GUI is now an API (`InsightsApiFeature`) plus two frontends: the Kraft pages in `funktor/inspect`
(`FunktorInspectUi(insightsApi = ...)`) and the Vue pages generated into the TypeScript SDK
(`funktor/codegen/src/main/resources/ts/insights/*`).

## Decide before implementing

1. **Screenshots** (asked 2026-11-11): possible. Headless Chrome is on the machine; the insights pages need a
   superuser login, so it takes a small script (playwright via `pnpm dlx`) against a running demo. Recommendation:
   after the maintainer's visual sweep, not before (screenshots taken now would go stale in the sweep), and as a
   script that re-takes them, not hand-made images. Until then: no screenshots.
2. `staticweb.astro` depends on ULTRA-00305 (staticweb removal): if the module goes, the page goes, and this ticket
   only removes its sidebar entry.

## The work

1. `insights.astro`: replace the `InsightsGui` section with "Viewing records": the API switch (one switch for the
   app, ULTRA-00301), mounting the Kraft pages (`FunktorInspectUi(insightsApi = ...)`, `mountFunktorInspect`), the
   Vue pages from the SDK, and what a record page shows.
2. Host apps: their dev server needs `disableDotRule` (webpack devServer `historyApiFallback`) to reload a record
   page, whose path ends in `.json`.
3. The Kraft addons it needs: `prismjs` (manual mode, `PrismAql`) and `visnetwork`, linked to `addons.astro`.
4. LLM mirror in the same commit.

## Acceptance

- No page or mirror names `InsightsGui`, `InsightsRenderer` or `InsightsGuiTemplate`
  (`grep -rn` over `docs-site/src` is empty).
- Every code snippet compiles against master (copied from a compiled source, as in ULTRA-00198's vis-network docs).

## History

- 2026-11-11: created as the DOCS follow-up of ULTRA-00198.
- 2026-11-11: `insights.astro`: "Viewing records" (Kraft and Vue), the dev-server note, the ten collectors, records in the depot. The funktor LLM mirror has no insights section, so nothing to move there. Docs build green (115 pages). Left: `staticweb.astro:44-53`, removed with ULTRA-00305.
- 2026-11-11: `staticweb.astro` removed with ULTRA-00305; no page or mirror names `InsightsGui` any more. All steps done.
- 2026-11-11: review gate PASS (with ULTRA-00304); DONE.
- 2026-11-11: DONE.
