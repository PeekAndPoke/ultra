# Kraft Addons — Testing Strategy

## Context

The Kraft addons (`kraft/addons/*`) show many "unused" warnings on their facade methods. Those
warnings are a coverage signal: the addon *loads* in a test, but its actual API (decode, render,
build, navigate, export…) is never called, so it's effectively untested. This doc defines how to
test each addon and what concrete cases to write.

Current state (audited 2026-06-25):

| Addon                      | Test file? | Depth today                       | Gap                              |
|----------------------------|------------|-----------------------------------|----------------------------------|
| jwtdecode                  | yes        | load + 1 decode                   | edge cases                       |
| marked                     | yes        | load + XSS                        | structure, `use()`, options      |
| avatars                    | yes        | load + type checks                | determinism, params, randomness  |
| browserdetect              | yes        | load + env detect                 | deterministic UA fixtures        |
| chartjs                    | yes        | load + `registerAll`              | builder helpers, component mount |
| pixijs                     | yes        | **decent** (objects, Graphics)    | `createText`, children           |
| threejs                    | yes        | **decent** (objects, scene graph) | more factories, Vector3          |
| signaturepad               | yes        | load only                         | component, export, trim          |
| prismjs                    | yes        | load only                         | plugin `applyTo`, component      |
| datetime                   | yes        | **done** (native tz, full suite)  | —                                |
| **nxcompile**              | **NO**     | —                                 | everything                       |
| **sourcemappedstacktrace** | **NO**     | —                                 | everything                       |
| **pdfjs**                  | **NO**     | —                                 | everything                       |

## Test infrastructure (reuse, don't invent)

- **Runner:** Kotest `StringSpec` on Karma + **headless Chrome** (`js { browser { testTask } }`).
- **Browser harness:** `kraft:testing` → `TestBed.preact(appSetup = { … }, view = { … }) { root -> … }`.
  Mounts a real Kraft app; `root` is a `KQuery<Element>` with `selectCss`, `awaitCss(css, timeoutMs)`,
  `textContent`, `attr`, `classes`, `click`, `typeText`, keyboard/mouse dispatch, etc.
- **Async addon load:** addons register via `appSetup = { addons { fooAddon() } }`; a component reads
  it with `by subscribingTo(addons.foo)` (null → non-null on load). Poll with the `eventually { }`
  helper (copy from `core-tests/.../addons/AddonRegistrySpec.kt` or `JwtDecodeAddonSpec`).
- **Pure unit tests:** for non-DOM logic, a plain `StringSpec` that calls the function directly —
  see `datetime/.../TimezonesSpec.kt`. No `TestBed` needed.
- Test deps per addon: `jsTest { jsTestDeps(); implementation(project(":kraft:testing")) }`.
  (nxcompile & sourcemappedstacktrace currently declare **no** jsTest deps — add them.)

### Headless constraints

- **2D canvas works** (software) → chartjs / signaturepad / pdfjs render paths are testable enough.
- **WebGL is unreliable** headless → pixijs/threejs **object construction & scene-graph mutation
  work**, but the actual `WebGLRenderer.render()` / RAF loop must be skipped (threejs already does).
- **Network/CDN** (pdfjs CDN, sourcemappedstacktrace `.map` fetches, prismjs language/plugin CDN):
  don't assert real fetched output — test the seam (dispatch, options, callback wiring) with fakes.

## The four test seams

1. **Pure unit** — deterministic facade method, no DOM. Load via TestBed (npm lib needs webpack) or
   call directly; assert input→output. (jwtdecode, marked, avatars, browserdetect, nxcompile.)
2. **Pure DOM-helper unit** — create an element, call a pure mutator, assert. (prismjs `PrismPlugin.applyTo`.)
3. **Component render** — `TestBed.preact` mount a Kraft component, drive it, assert DOM/state.
   (chartjs, signaturepad, prismjs `Prism`, pdfjs viewers.)
4. **Object/graph construction** — load addon, build objects, assert properties/graph; skip GPU render.
   (pixijs, threejs.)

## Per-addon strategy

### nxcompile — P0 (no tests)

Seam 1. Add jsTest deps. Load addon, then:

- `compileCode("return 41 + 1").let { it() }` → `42`.
- params/scope: `compileCode("return a + b")` invoked with a context object → sum.
- stdlib reachable: `compileCode("return Math.max(1,2)")` → `2`.
- error: malformed JS surfaces an exception (assert `shouldThrow`).
- sandbox sanity: a var defined in compiled code doesn't leak to global scope.

### sourcemappedstacktrace — P0 (no tests)

Seam 1 + fakes. Add jsTest deps. Real source-map mapping needs served `.map` files — don't chase
that. Instead:

- `mapStackTrace(knownStack, done = { mapped -> … }, options)` → `done` is invoked with an
  `Array<String>` (assert via a `CompletableDeferred`/`eventually`).
- `mapStackTraceCached` passes `cacheGlobally: true` (assert behavior: second call resolves without
  re-fetch — can stub the underlying fn). Document that deep mapping correctness is out of scope.

### pdfjs — P0 (no tests)

Mostly seam 3 + pure bits. Add jsTest deps.

- **Pure:** `PdfSource.Url/Base64/Data` construct; `PdfSource.load()` dispatches to the right path.
- **Scale clamp:** `modifyScale { it * 10 }` and `{ it / 10 }` clamp to `Options.scaleRange` ends.
- **Page nav (PagedPdfViewer):** mount with a tiny inline base64 1–2 page PDF; `gotoNextPage()` /
  `gotoPreviousPage()` clamp to `1..numPages`; `getState()` / `getCurrentPage()` reflect it.
- `getNumPages()` is null before load, correct after (`awaitCss` on a rendered page, then assert).
- CDN load via `ScriptLoader` and canvas pixels → out of scope (network/GPU); use a minimal PDF and
  assert state transitions + `onChange` callbacks, not pixels.

### signaturepad — P1 (load-only today)

Seam 3 (2D canvas works headless).

- Mount `SignaturePad`; `isEmpty()` true initially, `isNotEmpty()` false.
- Simulate a stroke by drawing on the underlying canvas 2D context (or dispatch pointer events via
  KQuery), then `isEmpty()` false and `onChange` fired.
- `clear()` → empty again + `onChange`.
- `export.toPng()` / `toSvg()` return a non-null `FileBase64` with the right data-URL prefix
  (`data:image/png`, `data:image/svg+xml`); `trimmed.toPng()` returns something smaller/!= full.
- `trimCanvas(canvas)` returns a canvas with reduced dimensions for a partially-filled canvas.

### prismjs — P1 (load-only today)

Seam 2 (pure, no CDN) covers most of the value:

- `PrismPlugin.applyTo(pre)` for each subtype on a fresh `<pre>`: `CopyToClipboard` → class
  `copy-to-clipboard` + `dataset.prismjsCopy`; `LineNumbers` → class `line-numbers` +
  `dataset.start` + `white-space: pre-wrap` when `softWrap`; `InlineColor` → class `inline-color`;
  `ShowLanguage` → class `show-language` + `dataset.language`.
- `OptionsBuilder.usePlugin` accumulates plugins.
  Seam 3 (best-effort, may need `awaitCss`): mount `Prism(language, code)` → eventually a `<pre><code>`
  appears; `LanguageLoader` falls back to `plain` for an unknown language without throwing. Don't
  assert exact highlight markup (CDN/version dependent).

### chartjs — P1 (load + registerAll today)

Seam 1 for the builder (pure, high value, kills most unused warnings):

- `ChartJsDataBuilder.nextColor()` cycles the palette and wraps around.
- `numberLabels(5)` → `["1".."5"]`; `numberLabels(2..4)` → `["2","3","4"]`.
- `value(x)` / `value(a,b,c)` wrap into the single/array union.
  Seam 3: mount `ChartJs(chartJsData{…})` with a tiny bar config → a `<canvas>` exists, no throw;
  change props → `shouldRedraw` updates in place (assert no crash + canvas persists). 2D canvas is fine.

### marked — P1 (load + XSS today)

Seam 1, deepen:

- structure: `# H` → contains `<h1>`; `**b**` → `<strong>`; `- a\n- b` → `<ul><li>`; fenced code →
  `<pre><code>`; `[t](u)` → `<a href="u">`.
- XSS already covered; add `javascript:` href / `onerror` attr stripping.
- `markdown2html(md, options)` overload changes output (e.g. a purify config toggle).
- `use(settings)` applies a marked option (e.g. `breaks=true` turns `\n` into `<br>`).

### browserdetect — P1 (env-detect today)

Seam 1 with **deterministic UA fixtures** (the key win — current test only reads the live browser):

- `getParser(chromeUA).getBrowser()` → name "Chrome"; Firefox/Safari/Edge UAs likewise.
- `getOs()` for Windows/macOS/iOS/Android UA fixtures; `getPlatform().type` (desktop/mobile/tablet);
  `getEngine()` (Blink/Gecko/WebKit).
- `forNavigator(fakeNavigator)` with a stubbed `userAgent`.
- `supportsPdf()` / `getMimeTypes()`: environment-dependent → assert type/shape only, or stub navigator.

### avatars — P1 (type-checks today)

Seam 1, deepen:

- **determinism:** `get("alice") == get("alice")`; `get("alice") != get("bob")`.
- SVG shape: contains `<svg`, `viewBox`; `getDataUrl` starts with `data:image/svg+xml`.
- params: `get(name, sat, light)` differs across sat/light values.
- randomness: `getRandom()` produces differing output across calls (allow tiny collision tolerance).

### pixijs — P2 (decent today)

Seam 4, fill gaps: `createText(opts)` builds a Text with the given string; `Graphics` children/
container access; property round-trips (x/y/alpha/visible). Keep `Application.init()` covered (it
resolves headless today) but do not assert rendered pixels.

### threejs — P2 (decent today)

Seam 4, fill gaps: the remaining factories (`Orthographic`/`Perspective` cameras, sphere/plane/
cylinder/torus geometries, ambient/directional/point/hemisphere lights), `Vector3` set/read, scene
graph `add`/`children`/transforms. `WebGLRenderer.render()` + `ThreeJsComponent` RAF loop stay
**skipped** (no GPU headless) — already the case.

### jwtdecode — P2 (decent today)

Seam 1, edge cases: `decodeJwt()` (Object return) on a known token; malformed/empty token →
exception or null per facade contract; a token with extra claims maps fully via `decodeJwtAsMap`.

## Rollout / priority

1. **P0 — create from scratch — DONE (2026-06-25):** `nxcompile`, `sourcemappedstacktrace`, `pdfjs`
   (added jsTest deps to the first two).
    - **nxcompile** ✅ 4 tests (load, sandbox arithmetic, multi-statement control flow, host-global
      isolation). API learning: `@nx-js/compiler-util` `compileCode` is a **statement executor**, not
      an expression evaluator — `evaluateCode(state, tempVars)` runs `with(sandbox){ <code> }` and
      **discards the return value**; it requires a sandbox object and **hides host globals** (`Math`
      is undefined inside). Test pattern: run code that mutates the sandbox, then read it back.
    - **sourcemappedstacktrace** ✅ 3 tests (load, `mapStackTrace`, `mapStackTraceCached`). Asserts the
      `done` callback fires with frames; real source-map resolution (served `.map` files) is out of
      scope.
    - **pdfjs** ✅ `PdfSource` data layer covered (Url/Base64/Data + value equality), pure, no
      CDN/canvas. **Follow-up (deferred):** the CDN-loaded `PdfJs` engine + canvas viewers
      (`ScrollingPdfViewer`/`PagedPdfViewer`: scale-clamp, page-nav, `getNumPages`) need a served PDF
      fixture + network/CDN access — write as an opt-in integration test (mount viewer with a tiny
      inline base64 PDF; assert state transitions, not pixels). Headless 2D canvas is fine; the
      blocker is CDN `ScriptLoader` reliability in CI.
2. **P1 — deepen shallow:** `signaturepad`, `prismjs`, `chartjs` (builder), `marked`, `browserdetect`,
   `avatars`. Biggest warning-reduction per line of test.
3. **P2 — top up:** `pixijs`, `threejs`, `jwtdecode` edge cases.

### Test-harness learnings (apply to remaining addons)

- A facade method that **throws during component render** leaves the DOM stuck on the previous
  output (preact discards the failed re-render) — so a green "loading" usually means the render
  threw, not that the addon failed to load. Wrap risky calls in `try/catch` inside `render`, or only
  call methods you expect to succeed.
- Calling addon facade methods via a `subscribingTo` component inside `TestBed.preact` + `eventually`
  is the reliable pattern (mirrors `AvatarsAddonSpec`). Pure data types (e.g. `PdfSource`) can be
  tested with a plain `StringSpec` — no `TestBed` needed.

## Conventions for the new tests

- One `*AddonSpec.kt` per addon under `src/jsTest/kotlin/`, Kotest `StringSpec`.
- Prefer **seam 1/2 pure tests** (fast, deterministic, kill the unused warnings) before component
  tests; add **seam 3/4** for components and construction.
- For async addon load use the shared `eventually { }` polling helper; for rendered DOM use
  `awaitCss`. Never assert CDN/GPU/network output — assert the Kotlin-side seam.
- Keep deterministic fixtures (known JWT, known UA strings, tiny base64 PDF) as test constants.
