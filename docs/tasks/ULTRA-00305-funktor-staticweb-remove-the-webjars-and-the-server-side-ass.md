# ULTRA-00305: Funktor staticweb: remove the webjars and the server-side asset plumbing

> **Status (2026-11-11): IN PROGRESS.** The maintainer: "delete staticweb entirely, go ahead" (2026-11-11). Out-of-scope finding of ULTRA-00198's gate. The maintainer on 2026-11-11: "remove
> all webjars this project and funktor-demo does not need... should be most if not all of them. This might also mean
> that we can remove the plumbing code for loading webjar assets into templates. Try to clean up as much code-surface
> as possible in static-web. These are old ideas that no longer hold today."

**Security-critical:** no (it removes the prismjs 1.29.0 webjar, which lacks the CVE-2024-53382 fix)

## Why this exists

`funktor/staticweb` (about 970 lines) serves server-rendered pages with assets from webjars: jquery, prismjs,
fomantic-ui-css, lazysizes, visjs (`funktor/staticweb/build.gradle.kts:34-42`, all `api`). Its parts:
`resources/` (`WebResources`, `WebResourceGroup`, `WebJarAssetLocator`, a prismjs resource group), `templating/`
(`SimpleTemplate`, images), `semanticui/templates/` (two page templates), `flashsession/`.

Who uses it, read 2026-11-11:
- `funktor/all` adds it (`build.gradle.kts:77`) and installs it (`funktor.kt:36`, `funktorStaticWeb()`).
- The demo server installs ktor `Webjars` at `/vendor` (`funktor-demo/server/src/main/kotlin/server.kt:36`).
- `funktor/insights/reference/gui/*`, which is deleted anyway (ULTRA-00190 step 10).
- Nothing else in this repo or the demo. The frontends (Kraft, Vue) bring their assets through npm.
- `Deps.WebJars` also lists glidejs and fomantic-ui-js.
- Docs: `staticweb.astro`, the funktor sidebar, `site.ts`, mentions in `funktor/index.astro` and `logging.astro`.

## Decide before implementing

1. **Remove the whole module** rather than only its webjars. Recommendation: yes. Without the webjars, `resources/`
   and both templates have nothing to load; `templating/` and `flashsession/` have no user. This is the
   expensive-to-reverse decision; git keeps the code.
2. Then check `ultra/html` and `ultra/semanticui`: still used by `kraft/core`, `kraft/semanticui` and
   `funktor/inspect`, so they stay unless that check says otherwise.

## The work

1. Delete `funktor/insights/reference/` first (ULTRA-00190 step 10), so nothing else reads the module.
2. Delete `funktor/staticweb`, its `settings.gradle.kts` entry, `funktor/all`'s dependency and `funktorStaticWeb()`.
3. Demo server: drop `install(Webjars)` and the `ktor-server-webjars` dependency.
4. `Deps.WebJars` and `Deps.Ktor.Server.webjars`: delete what nothing uses.
5. Docs: remove `staticweb.astro`, its sidebar entry and mirror; reword the mentions. CHANGELOG: **Breaking**.
6. Compile sweep of everything that depended on `funktor:all`; the demo server boots and serves its frontends.

## Acceptance

- `grep -rn "webjars\|staticweb\|WebResources" --include=*.kt --include=*.kts` over the repo and the demo is empty
  (apart from the CHANGELOG).
- Full suite green (one at a time, machine-wide); demo server starts.

## History

- 2026-11-11: created from ULTRA-00198's gate, scope widened by the maintainer.
- 2026-11-11: implemented. `funktor/insights/reference/` deleted (ULTRA-00190 step 10) except `TAB-SPECS.md`, still
  the wire reference for `slices.ts`, moved to `funktor/insights/TAB-SPECS.md`; the `VUE-REF` pointers removed.
  `funktor/staticweb` deleted with its settings entry, `funktor/all`'s dependency and `funktorStaticWeb()`;
  `Deps.WebJars` and `Deps.Ktor.Server.webjars` deleted (also from `Ktor.Server.full`); the demo server's
  `install(Webjars)` removed. Docs: `staticweb.astro`, its sidebar entry, index card, logging link, `site.ts`
  artifact and `llms.txt` line removed; docs build green (114 pages). `ultra/html` and `ultra/semanticui` stay
  (Kraft core, Kraft semanticui, funktor inspect). Compile sweep green: funktor-all (main and tests), insights,
  demo server and the four demo frontends.
- 2026-11-11: funktor-all 283/283 green. The demo server's tests stop at boot on `${AWS_SES_ACCESS_KEY}` unresolved (the maintainer's pending local `keys.env.conf` entry), so "demo server starts" is not verified yet.
