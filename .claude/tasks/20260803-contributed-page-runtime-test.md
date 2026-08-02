# A runtime test for contributed pages — and the global-state question behind it

**Status:** TODO — raised 2026-08-02 after a contributed page shipped broken past a green type-check.
**Plan:** `.claude/tasks/20260730-frontend-sdk-vue-contributors.md`
**Security-critical:** no.

## The gap, demonstrated rather than argued

`InsightsPage` shipped with `props.client is undefined` at run time. Everything was green:
`vue-tsc` clean, `ts-verify` clean, all Kotlin suites clean, and every emitted module served 200
through Vite. The maintainer found it by opening the page.

**Nothing in the pipeline could have caught it**, and that is structural rather than an oversight:

`mountAll` types a route's component as `() => Promise<unknown>` (`TsMountEmitter`), so a component's
props are **erased at the registry boundary**. A page with a required prop type-checks perfectly, and
the router then constructs it with none.

## Two dead ends, both measured — do not repeat them

1. **SSR (`@vue/server-renderer`) is not enough.** `renderToString` runs `setup()` but never
   `onMounted`, and the dereference lives in `onMounted`. Proven vacuous by mutation: an SSR-based
   smoke test **passed** against a deliberately broken `const client = props.client!`.
2. **"It did not throw" is not enough either.** `InsightsListPage.vue:45` wraps its call in
   try/catch and renders the failure as text, so a broken client is a quiet error message in the
   page — not an exception a test can see.

So the assertion has to be **behavioural**: mount with no props, and require that the page ISSUED
its request. A recording transport does that with no server and no flake.

## What it needs

- `happy-dom` + `@happy-dom/global-registrator` (dev only). I installed both, got the shape working,
  then removed them rather than commit a half-finished script.
- **Ordering that bit me:** create the Vite server FIRST on clean Node globals, then
  `GlobalRegistrator.register()`, then restore `globalThis.setTimeout` — happy-dom installs a browser
  `setTimeout` whose handle has no `.unref()`, which Vite calls. And `vue` must be imported AFTER
  registration, or `@vue/runtime-dom` binds to globals that do not exist yet.
- **Where I stopped:** loading an SFC through `server.ssrLoadModule` and mounting it into happy-dom
  failed with `Cannot read properties of undefined (reading 'modules')`. Not diagnosed. Suspect the
  SSR-transformed module wants an HMR/client runtime that is absent; `hmr: false` did not help.
  Building the app and mounting the built bundle may be the shorter path than fighting `ssrLoadModule`.

## The bigger question the maintainer raised (2026-08-02, for discussion)

**Passing the client through props is probably the wrong shape.** A `useClient()` composable was
proposed instead, and it likely subsumes the `provideSdkConfig` fallback added in `e4ee7a8a` — that
was a fix for a live bug, not a considered design, and it should be treated as provisional.

Worth deciding together, because it sets the pattern every future contributed page follows:

- Who owns the client — the app, or the SDK?
- One composable per client (`useInsightsClient()`), or one config accessor plus per-page
  construction (what exists now)?
- Does a contributed page ever get to reach global state directly, or must everything arrive through
  provide/inject so it stays testable and multi-backend-capable?

**Do the test AFTER that decision**, not before — the test asserts how a page gets its client, so
writing it against the provisional shape means writing it twice.
