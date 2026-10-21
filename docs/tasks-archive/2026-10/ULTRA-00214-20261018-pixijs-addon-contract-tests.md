# ULTRA-00214: kraft/addons/pixijs: contract tests that survive version updates

> **Status (2026-10-19): DONE.** (archived 2026-10-19) — gate PASS after 2 rounds

**Plan:** none (asked by the maintainer, 2026-10-18, after the Klang playback highlight broke on a dependency bump)
**Security-critical:** no

## Why

Ultra 0.111.0 moved the addon's `pixi.js` from 8.17.1 to 8.22.0, and Kotlin 2.4 moved webpack from 5.101.3 to
5.108.1. Together they broke Klang's code-editor playback highlight (an overlay on a Pixi `Application`). The browser
console showed `Failed to initialize PixiJS highlight overlay TypeError: Cannot read properties of undefined (reading 'add')`,
and the page froze while highlights queued up.

Root cause, found in Klang's bundle: webpack 5.108 dropped pixi's side-effect import `import './init.mjs'` in
`pixi.js/lib/app/Application.mjs`. pixi's `package.json` lists `./lib/app/init.*` under `sideEffects` in both
8.17 and 8.22, so webpack, not pixi, changed. That import runs `extensions.add(ResizePlugin)` and
`extensions.add(TickerPlugin)`, so without it `await app.init(...)` succeeds but `app.ticker` is `undefined` and
`resizeTo` does nothing. Klang fixed it on its side with a webpack rule marking every `pixi.js` module as
side-effectful (`klang/webpack.config.d/04-pixi-side-effects.js`).

The existing `kraft/addons/pixijs/src/jsTest/kotlin/PixiJsAddonSpec.kt` did not catch it: "creates and initializes
an Application" only checks that `init()` resolves. Every consumer of the addon that bundles with webpack 5.108 is
exposed, including the kraft examples.

## Spec

Tests in `kraft/addons/pixijs/src/jsTest` (run through the same Kotlin/JS webpack the consumers use, so a bundling
regression shows up here) that pin the fundamentals a consumer relies on:

- [x] The addon loads (`import('pixi.js')`) and `createApplication()`, `createGraphics()`, `createText()` return
      instances.
- [x] After `await app.init(...)`: `app.ticker` is defined, `app.ticker.add(cb)` registers a callback that is
      called once `app.ticker.start()` runs, and `remove` / `stop` work. **This is the row that would have caught
      the 0.111.0 break.**
- [x] `app.renderer` and `app.canvas` exist; `app.stage` accepts `addChild` of a `Graphics`.
- [x] `resizeTo = <element>` resizes the renderer when the element's size changes (the ResizePlugin, same init
      import).
- [x] The init options Klang uses are accepted: `backgroundAlpha = 0`, `antialias`, `preference = "webgl"`,
      `autoDensity`, `resolution`, `width` / `height`.
- [x] `Graphics`: the drawing calls the externals in `src/jsMain/kotlin/js/pixi-definitions.kt` declare (at least
      `clear`, `rect` / `roundRect`, `fill`, `stroke`, `alpha`, `position`, `visible`) run without throwing and
      change the reported bounds where they should.
- [x] `app.destroy(rendererDestroy = true)` tears down without throwing, and a second `createApplication()` +
      `init()` afterwards works (Klang re-creates the overlay on every editor mount).
- [x] A headless-browser caveat is handled: if WebGL is unavailable in CI, the tests fall back to a renderer that
      works there (or skip the GPU-only rows with a clear message), but the ticker and plugin rows always run.
- [x] Optional: one row per declared external in `pixi-definitions.kt` that the property or method exists on the
      real object, so a renamed or removed pixi API fails here instead of at runtime in a consumer.

Acceptance: with pixi.js pinned back to a version where `app/init` is dropped (or with the side-effect import
deliberately stripped, e.g. a webpack rule `sideEffects: false` for pixi in the test config), the ticker row goes
RED; with the current setup it is GREEN.

## Implementation notes

- Klang's usage, for reference: `klang/src/jsMain/kotlin/codemirror/CodeMirrorHighlightBuffer.kt` (create, init
  with the options above, `canvas` appended to a DOM element, `ticker.add` / `stop` / `start`, `Graphics` pool,
  `destroy`).
- **Ship the fix in the addon, so no consumer needs a webpack rule.** A library cannot ship `webpack.config.d`:
  the Kotlin Gradle plugin reads it only from the project being bundled. Two ways, both in
  `kraft/addons/pixijs/src/jsMain/kotlin/pixijs_addon.kt`, next to `import('pixi.js')`:
  1. **Register the plugins explicitly:** `module.extensions.add(module.TickerPlugin, module.ResizePlugin)`.
     Safe to repeat: pixi's `extensions.handleByList` skips an extension already in the list
     (`pixi.js/lib/extensions/Extensions.mjs`, `if (list.includes(extension.ref)) return`). Independent of how any
     bundler treats side effects. Risk: it names two plugins; a future pixi plugin registered by `app/init` would
     be missed, and a renamed export reads `undefined` (guard it).
  2. **Import the side-effect module directly:** `import('pixi.js/app')` as well (pixi's `exports["./app"]` is
     exactly `./lib/app/init.mjs`). A dynamic import target is always bundled and run, so the drop seen here (a
     nested `import './init.mjs'` inside `Application.mjs`) cannot happen to it, and it registers whatever
     `app/init` registers, now and later. Risk: it relies on the subpath export staying, and still on the bundler
     honouring a side-effect-only module (the class of thing that broke).
  Doing both costs two lines and covers each one's risk. Either way, after `init()` check `app.ticker != null` in
  the tests (and possibly in the addon, failing loudly with a message naming this task).
- After the next Ultra release with the fix, Klang removes `klang/webpack.config.d/04-pixi-side-effects.js`.

**Done (2026-10-19), decisions by the maintainer: both remedies; the optional API rows yes.**
- Fix: `kraft/addons/pixijs/src/jsMain/kotlin/pixijs_addon.kt` `registerExtensions` — imports ALL NINE pixi init
  entries whose import webpack 5.108 drops (the class-local `import './init.mjs'` in Application, Graphics, Text,
  BitmapText, HTMLText, Mesh, TilingSprite, NineSliceSprite, ParticleContainer; list grepped from `pixi.js/lib`), and
  registers `TickerPlugin` / `ResizePlugin` explicitly as well. `backgroundAlpha` added to `ApplicationOptions`.
- Tests: `kraft/addons/pixijs/src/jsTest/kotlin/PixiJsContractSpec.kt`, 10 rows. GPU-free row: pixi's
  `Application._plugins` contains both plugins (the root cause itself; `@internal` field, kept on purpose). A row
  renders a `Graphics` and a `Text` and checks their render pipes. WebGL is available in the Karma ChromeHeadless.
- **The break reproduces in ultra's own test bundle, no extra webpack rule needed** (pixi 8.22 + webpack 5.108).
- Klang can delete `klang/webpack.config.d/04-pixi-side-effects.js` after the release (it uses Application,
  Graphics, the ticker and resizeTo — all covered).

## Test evidence

- [x] Before the fix: 5 contract rows RED (plugins, ticker, resizeTo, re-create, API existence); the render-pipes row
      RED against `54c80970` (only `app/init` fixed).
- [x] Final (`9883bd6f`): `:kraft:addons:pixijs:jsBrowserTest` 10/10 contract + 3/3 existing green.
- [x] Mutations (restored + `cmp`): without the `pixi.js/app` import → green (the explicit registration covers it);
      without the explicit registration → green (the import covers it); without both → the same 5 rows RED; dropping
      `pixi.js/graphics` or `pixi.js/text` → the render-pipes row RED (reviewer-confirmed at
      `PixiJsContractSpec.kt:134/135`).

## Review record — the LEDGER (filled by /feature-review)

Change set `54c80970`. Scoped by risk (a few lines in an addon + tests, no input surface): ONE `reviewer-high` with
a combined implementation & domain charter, no security charter.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `pixijs_addon.kt:71-81`, `PixiJsContractSpec.kt` | Webpack 5.108 drops EVERY pixi class-local `import './init.mjs'` (Graphics, Text, Mesh, TilingSprite, BitmapText, HTMLText, NineSliceSprite, ParticleContainer), not only `app/init`; `scene/graphics/init` alone registers the Graphics pipes → a `Graphics` renders nothing, later redraws throw. Masked in Klang by its webpack rule; the task told Klang to delete it. No test row rendered | fix | CONFIRMED by experiment: new row "Graphics and Text actually render: their render pipes are registered" RED against `54c80970`. Fix: import all nine class-local init entries (`pixi.js/app`, `/graphics`, `/text`, `/text-bitmap`, `/text-html`, `/mesh`, `/sprite-tiling`, `/sprite-nine-slice`, `/particle-container`) — the list grepped from `pixi.js/lib` |
| 1 | MINOR | `PixiJsContractSpec.kt:77-90` | Ticker row cannot fail for `start`/`remove`/`stop` individually (`autoStart` defaults true; remove+stop asserted together) | fix | `autoStart = false`; each step asserted on its own |
| 1 | MINOR | `PixiJsContractSpec.kt:54` | Plugin row reads pixi's `@internal` `Application._plugins` — brittle across versions | fix (documented) | Kept on purpose (root cause, GPU-free); comment says so and points at the behaviour rows |

**Probed and CLEAN** (reviewer): `pixi.js/app` resolves through pixi's exports to `lib/app/init.mjs`, same
`Extensions` instance; `extensions.add` idempotent (`handleByList` `includes`, pipes/systems `handleByNamedList`);
plugin order irrelevant; no race (registration before the facade exists); `rendering/init`, `spritesheet/init` and
`browserAll` are eager (imported from side-effectful modules); resize row deterministic; cleanup OK.

Fix batch: `:kraft:addons:pixijs:jsBrowserTest` contract 10/10 + existing 3/3 green.

Round 2 (`reviewer-xhigh`, two-phase): zero CRITICAL/MAJOR. Phase-1 MINOR "six of the nine imports serve classes the
facade cannot create" WITHDRAWN in phase 2 (scope decision, no factual error). MINOR "task record out of date for
`9883bd6f`" → fixed (this section restated). All FIXED entries confirmed holding.

**Verdict: gate PASS** (round 2 clean).

## History

- 2026-10-21: numbered ULTRA-00214 (was `docs/tasks-archive/2026-10/20261018-pixijs-addon-contract-tests.md`).
