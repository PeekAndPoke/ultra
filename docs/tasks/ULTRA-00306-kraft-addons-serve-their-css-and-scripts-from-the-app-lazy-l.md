# ULTRA-00306: Kraft addons: serve their CSS and scripts from the app, lazy-loaded

> **Status (2026-11-11): IN PROGRESS.** Implemented; review gate next. From ULTRA-00198's gate. The maintainer on 2026-11-11: "We should be able to
> serve assets locally, but not be compiling them into the bundle. It must be still possible to lazy-load them at
> runtime." Earlier: "we should serve all css or other assets locally. How would this work when we publish the
> kraft addon and use it in another project?"

**Security-critical:** no (it removes a third-party origin from the pages)

## Why this exists

- `kraft/addons/prismjs/src/jsMain/kotlin/PrismJsInternals.kt:61-89`: the theme and plugin CSS come from cdnjs
  (1.28.0, with SRI), while the JS comes from the npm package `prismjs` through webpack.
- `kraft/addons/pdfjs/src/jsMain/kotlin/PdfJs.kt:55-83`: pdf.js 2.16.105 and its worker come from cdnjs.
- Other addons: to check in step 1.

## Design (to confirm in a spike, step 1)

Webpack 5 **asset modules**: `new URL("prismjs/themes/prism.min.css", import.meta.url)` makes webpack copy the file
from `node_modules` into the app's output, under a hashed name, and gives back its URL. Nothing is compiled into the
bundle; the addon still adds the `<link>` (or starts the worker) only when it is first needed, as now.

How it works for a published addon: the addon declares the npm package (`api(Deps.Npm { prismjs() })`, as now), so
the package lands in every consumer's `node_modules`; the consumer's own webpack build emits the files. No consumer
configuration, no CDN, the same origin as the app (so a strict CSP needs no CDN host).

pdf.js: `import('pdfjs-dist')` (webpack splits it into its own chunk, loaded on first use) and
`workerSrc = new URL('pdfjs-dist/build/pdf.worker.min.mjs', import.meta.url)`.

Risks the spike settles:
1. Kotlin/JS emits `js("...")` into its own module format; whether webpack accepts `import.meta.url` there (the IR
   compiler's default module kind is not ES). Fallback: a small `.mjs` file in the addon's `jsMain/resources`
   that exports the URLs, imported dynamically.
2. Karma tests: whether the test webpack config serves emitted assets.
3. pdfjs-dist's current major (look it up online) and its API changes from 2.16.

## The work

1. Spike on prismjs's default theme: emit, lazy `<link>`, check in the demo and in a browser test.
2. All prismjs CSS (theme, line numbers, toolbar, inline color); drop cdnjs and the SRI hashes.
3. pdfjs from npm, lazily imported; the worker the same way.
4. Any other addon loading from a CDN.
5. Docs (`addons.astro`, LLM mirror) and CHANGELOG.

## Acceptance

- `grep -rn "cdnjs\|unpkg\|jsdelivr" kraft/addons` is empty.
- The demo's network tab shows the CSS and the pdf worker from the app's origin, requested only when a page needs them.
- Browser tests for prism styles and the pdf viewer green.

## History

- 2026-11-11: created from ULTRA-00198's gate.
- 2026-11-11: implemented. The spike settled all three risks: the addons compile to ES modules (`es2015`), so
  `js("new URL('prismjs/...', import.meta.url)")` works; Karma serves the emitted files; webpack resolves the bare
  package path. Prism: theme, line numbers, toolbar, inline color from `prismjs` (1.30.0), no SRI needed (same
  origin). pdf.js: `pdfjs-dist` 4.10.38 (latest is 6.5.438; 5 and 6 change the render API the viewers and externals
  use, so the newest 4.x; the upgrade is its own task), `import('pdfjs-dist')` and the worker by `new URL`;
  `LibrarySrc`/`librarySource` removed. No other addon loads from a CDN. Tests: `PrismStylesSpec` (red on the CDN:
  "should start with http://localhost:9876"), `PdfJsSpec` (a one-page PDF opened through the worker; red with a
  broken worker URL). The admin app's build emits the four Prism stylesheets as separate files, no cdnjs URL left.
  Docs (`addons.astro`, mirror) and CHANGELOG.
