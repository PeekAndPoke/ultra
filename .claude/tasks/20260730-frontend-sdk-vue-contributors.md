# Frontend SDK — Vue components and pages via codegen contributors

**Status:** TODO — design agreed 2026-07-30, refined same day (config ownership + profiles), not started
**Plan:** `.claude/tasks/20260729-ts-sdk-codegen.md` (extends its contributor model)
**Security-critical:** no (dev-time generator). The insights auth floor it unblocks IS security-critical
and is tracked separately.

## Why

Kraft frontends are slow to iterate on — a CSS change costs seconds. The decision (2026-07-30,
maintainer) is to move frontends to Vue + Vite, and to have **the framework generate the whole
frontend SDK on the fly**: API clients *and* prebuilt components and pages. Target is Vue only.

**Nothing is published to npm.** Components ship as classpath resources inside the funktor modules and
are emitted by the same run that emits the client they call — so the two can never be version-skewed.

## Decisions taken (2026-07-30, maintainer)

| Decision | Note |
|---|---|
| Vue + Tailwind, Vue only | No second target |
| No npm publishing | Full SDK generated on the fly, per app |
| No backward compat | Nothing needs to keep working |
| No design compat | Free to look however Vue + Tailwind allow |
| No graph-library compat | Current libs are not sacred |
| kotlinx.html renderers are DELETED, not ported | Read them as the spec for what each view needed, then delete. Do not keep them compiling |
| The insights **bar** goes away entirely | A borrowed Symfony idea that has not aged well and was barely used. A console log covers the need. Removes the insights↔staticweb cross-dependency |
| Framework ships only non-customizable views | Logs, locks, workers, depot. Ops/b2b/b2b2c views belong in their own frontends — code as close to the leaves as possible |
| Collector data stays an **open envelope**, no sealed hierarchy | A sealed hierarchy would block app-defined collectors |
| Every collector brings its own TS contributor | Providing its own tab + icon + content |
| The codegen design is NOT frozen | Config-file emission is in scope; new contributor kinds/actions are fine |
| Config emission is bounded by ownership | Refined 2026-07-30. The generator owns `<out>/` outright and writes nothing outside it. **No file-merging mechanism** — see the refinement below |
| Profiles select roots, not contributors | Refined 2026-07-30. No tree-shaking stage — that is the walker run twice. See §1 |

## The core problem: import resolution in hand-written resources

A hand-written `.vue` resource contains import statements:

```
import type { GlobalLockEntry } from '???/models'
import { GlobalLocksApi }      from '???/clients/GlobalLocksApi'
```

`???` cannot be relative, because the file's final location is chosen by whichever `out.file(path, …)`
emits it — a relative import would encode an assumption about where it lands, and a *shared* component
emitted at two different depths would need two different import texts in one source file.

Three options were considered:

- **Freeze the layout** so uniform depth makes relative imports correct by construction. Free, but the
  layout becomes contract and changing it later means touching every resource file.
- **Rewrite `@sdk/…` specifiers at emit time** to the correct relative path. Location-independent and
  needs nothing from the app, but it is a text transform on TS/Vue source — including dynamic
  `import()` and avoiding CSS `@import` in `<style>` blocks.
- **A real path alias** (`@sdk/*`). Rejected initially because it needs entries in the app's
  `tsconfig.json` and `vite.config.ts` — files the generator does not own.

### DECIDED: the path alias, because config emission is in scope

Once the generator may emit config, the alias wins outright: **the import text in the resource file is
byte-identical to the import text in the emitted file.** Nothing is transformed, so nothing can be
transformed wrongly, and the whole emit-time-rewrite layer disappears.

What makes it work is that the same alias name resolves to different roots per context:

| Context | `@sdk` resolves to |
|---|---|
| Dev harness (authoring) | generated output, with live resource dirs overlaid |
| Consuming app | the generated output dir |

Config stays non-invasive by living **inside the SDK dir the generator owns**: `<out>/tsconfig.json`
carrying the `paths` mapping, plus an alias fragment the app spreads into its own `vite.config.ts`. The
app's one-time wiring is two lines; the generator never edits a file it does not own.

- [ ] Make the alias name configurable (`--alias`), defaulting to something unlikely to collide.
- [ ] A missing alias fails loudly (`cannot resolve @sdk/models`) rather than silently — acceptable.
      Optionally detect a sibling `vite.config.*` and warn when the alias is absent.

### Refined 2026-07-30: an ownership boundary, NOT a file-merging mechanism

Raised by the maintainer: regenerating the SDK must not destroy hand-made changes in the app it
generates into — so "maybe we need mechanisms to modify existing files".

**Decision: do not build merging.** The concern is right; merging is the wrong answer to it.

- `vite.config.ts` is TypeScript containing arbitrary expressions. There is no sound structural edit —
  only text munging, which fails silently on anything unusual.
- `tsconfig.json` is JSONC. Any parse/serialise round-trip drops comments and reformats, which *is*
  destroying the user's file, just less visibly.
- A merge is wrong-and-quiet by construction. That is the failure mode this whole plan exists to
  remove (see "Hard error on unmapped types" in the locked decisions).

**The rule instead — a boundary, not a mechanism:**

| Location | Generator may |
|---|---|
| under `<out>/` | own it outright: create, overwrite, and delete its own prior output |
| anywhere else | never write. Emit a fragment, and *verify* the app wired it |

This makes "regenerating destroys my edits" impossible by construction rather than by careful merging:
files under `<out>/` are not meant to be edited (they carry a do-not-edit header), and files outside it
are never touched.

**The real gap the concern exposes is STALE OUTPUT, not merging.** Wholesale replacement without
deletion leaves ghosts — a renamed client keeps compiling from its old file, so nothing fails. Blind
deletion of `<out>/` is worse: it eats a user file that wandered in.

- [ ] Write `<out>/.sdk-manifest.json` listing every path the run produced. The next run deletes
      exactly the paths its predecessor wrote and no others. A file present on disk but absent from the
      previous manifest is left alone and reported.
- [ ] `--check` extends to stale files: output on disk that the current run no longer produces is a
      difference, same as a changed file. `TsSdkOutput.diffAgainst` (`sdk/TsSdkOutput.kt:61`) currently
      only walks planned entries, so it cannot see a ghost. Fix that when the manifest lands.

**Two write modes cover every "app-owned file" case without merging:**

- [ ] `out.scaffold(path, content)` — writes only when the path does not exist; **never** overwrites.
      This is how an app-owned file gets seeded (a starting `vite.config.ts`, an example page) and
      then belongs to the app forever.
- [ ] `out.requires(description, check)` — verifies app-side wiring is present and fails with the exact
      lines to paste. The alias check above is the first instance.

Note `scaffold` is the one write mode whose output is NOT reproducible from the model, so it must be
excluded from `--check` and from the manifest — otherwise the second run reports the user's own edits
as drift.

## What `ultra/codegen` needs added

Four additions. The first three are required; the fourth is a proposal only (see below).

### 1. Profiles / tagging

Nothing filters today: `TsSdkBuilder(contributors)` takes the whole list
(`ultra/codegen/src/main/kotlin/sdk/TsSdkBuilder.kt:61`). `CodeGenHints.tags` exists but has **zero
call sites** — `tag(` appears exactly twice in the repo, both in its own declaration
(`funktor/rest/src/jvmMain/kotlin/docs/CodeGenHints.kt:29`). It is vestigial from the Dart gen.

- [ ] A `TsSdkProfile` passed to the builder. Each app's generate run selects its profile.
- [ ] **Write down that profiles are not access control.** The endpoint still exists and is still
      gated server-side. Profiles matter for bundle size and, more substantively, to avoid
      **disclosing the admin API's shape** — paths, param names, response fields — inside a
      customer-facing JS bundle. That is information disclosure, not authorization.

#### DECIDED 2026-07-30: a profile selects ROOTS, never contributors — and there is no tree-shaking stage

Two alternatives were weighed by the maintainer: filter contributors, or build the full model and then
tree-shake it down to a profile.

**Tree-shaking is rejected — it is the walk, run twice.** `TypeWalker` already computes a transitive
closure from a set of entry points; that IS tree-shaking, done at the only moment the entry points are
known to be authoritative. Building the complete model first and shaking it afterwards discards the
first result and yields nothing extra: the model is a pure function of its roots, so no fact about the
full model can constrain the reduced one. Bundlers tree-shake because they are handed a finished graph
and cannot choose its entry points. Here we choose them.

**Filtering contributors is rejected — it silently severs claim from emit.** A contributor does several
jobs at once. `MpDateTimeTsContributor` both CLAIMS the Mp types and SHIPS `runtime/datetime.ts`
(`contributors/MpDateTimeTsContributor.kt:44-58`), and those travel by different mechanisms:
`models.ts` emits `import … from <claim.importFrom>` for every used claim
(`ts/TsModelEmitter.kt:43-45`), while the file at that path arrives via the contributor's `emit`. They
agree today only because one class owns both halves. Excluding such a contributor from
`contribute`+`emit` while keeping `claimTypes` — the natural implementation, since the claims registry
must stay complete — emits the import and never writes the file. Broken output from a valid config,
invisible until `tsc`.

**So: every contributor always runs every phase; the profile narrows the ROOT SET.** Consequences,
all of which fall out rather than needing code:

- A claim whose type is no longer reached simply lands outside `usedClaims`, so `models.ts` does not
  import it and the conditional emit that ships its runtime module correctly ships nothing. The
  datetime contributor is already written this way — it is the pattern, not a special case.
- Type-level filtering needs no work at all: the walker is root-driven, so restricting roots restricts
  the model by construction.
- Contributor SELECTION becomes as structurally irrelevant as contributor ORDER already is — the same
  property the phased contract buys, extended to profiles.

- [ ] The predicate is over roots (features, routes, pages), expressed via `CodeGenHints.tags` for
      routes. A contributor that emits pages makes that emit conditional on its own roots surviving —
      a model query, exactly like the datetime contributor's `usedClaims` check.

#### The one check that survives from the contributor-filtering analysis

Independent of profiles, and worth doing now: a claim declares `importFrom`, but nothing verifies that
anyone actually emits a file there. A contributor can claim `importFrom = "./runtime/foo"` and forget
to emit `runtime/foo.ts`; the failure surfaces as a module-resolution error inside generated output
rather than as something naming the contributor.

- [ ] Validate that **every used claim's relative `importFrom` corresponds to a planned output path**.
      Relative specifiers only — a bare specifier (`zod`) is an external package, not our file. Cheap,
      and it closes the claim↔emit gap generally rather than per-contributor.

### 2. Shared / idempotent emission

`TsSdkOutput.add` hard-errors on *any* duplicate path, even byte-identical content
(`sdk/TsSdkOutput.kt:37`). `TsRuntime`'s KDoc already documents the workaround as a rule — *"a
generator that needs a shared module must be the single one asking for it"* (`ts/TsRuntime.kt:43`) —
which is fine for four runtime modules and one contributor, and will not survive N Vue contributors
that all want `components/JsonTree.vue`.

- [ ] `out.shared(path, content)`: identical content dedupes; **differing** content is a hard error
      naming both contributors.

### 3. Aggregation registries

`models.ts` is coherent only because the *builder* owns it, not a contributor
(`sdk/TsSdkBuilder.kt:103`). There is currently no way for two contributors to write **into one file**.
Needed for at least five:

| Aggregate | Contributors declare |
|---|---|
| Router table | `route(path, component)` |
| Nav / menu | `navItem(label, icon, route)` |
| Insights tab table | `insightsTab(key, label, icon, component)` |
| `tsconfig.json` / vite alias fragment | path entries |
| npm requirements manifest | `requiresNpm(name, range)` |

- [ ] A phase-3.5 registry — the emit-phase analogue of `TsTypeClaims`. Contributors declare keyed
      entries; the builder renders the aggregate. Keyed inserts, so contributor order stays
      structurally irrelevant.
- [ ] Config is a registry target, **not** a plain file emit, precisely because it is
      N-contributors-into-one-file.

### 4. `out.vue(...)` — the "raw contributor" action

```kotlin
class InsightsVueContributor : TsSdkContributor {
    override val name = "funktor:insights:vue"

    override fun contribute(roots: TsSdkRoots) {
        // the tab's data must be in the model even though no endpoint returns it directly
        roots.root(typeOf<KontainerCollector.Data>(), "insights:kontainer")
    }

    override fun emit(context: TsSdkEmitContext) {
        context.out.vue(
            resourceDir = "vue/insights",
            files = listOf("KontainerTab.vue", "components/ServiceTable.vue"),
            to = "pages/insights",
        )
        context.registry.insightsTab(
            key = "kontainer", label = "Kontainer", icon = "cubes",
            component = "pages/insights/KontainerTab.vue",
        )
    }
}
```

- [ ] **An explicit file list, never classpath directory scanning.** Scanning is how a stale file ships.
      A test asserts the declared list matches the directory contents — an unlisted file compiles fine
      in the dev harness and is simply absent from the generated output, which is the silent failure.

## The dev harness

Since nothing is published, the dev loop for the components themselves must live in-repo — and it is the
same fast loop that motivates the whole exercise, so it has to be good.

- [ ] One Vite app in the repo. Generated parts come from a real generate run; hand-written parts resolve
      to the resource dirs so edits hot-reload without re-running Gradle.
- [ ] Prefer a **`--dev` generate mode that symlinks** hand-written files instead of copying them: the
      generated tree *is* the dev tree, one alias, one layout, live editing. Production and dev generate
      then differ only in copy-vs-link. Windows needs developer mode for symlinks.

## The gate — `vue-tsc` over assembled output

Shipping `.vue` files as opaque jar resources is the *"claims are trusted, never verified"* trap from
`20260729-ts-sdk-codegen.md`, one level up: a resource containing
`import type { X } from '@sdk/models'` is a claim about generated output that nothing checks.

- [ ] `vue-tsc --noEmit` over a **fully generated** SDK, built exactly like the existing
      `:ultra:codegen:tsVerify`: self-contained dir, own `package.json` + `pnpm-lock.yaml` + `.npmrc`
      with pinned pnpm and node, fixtures regenerated every run with nothing generated checked in,
      wired into `check`, failing loudly rather than skipping when the toolchain is absent.
- [ ] Must exist **before the first component ships**, not after.
- [ ] Useful forcing function: the gate's own pinned lockfile becomes the canonical list of what
      framework components depend on, so adding a dependency is a deliberate reviewed act.

With the alias decided, this gate no longer has to catch path arithmetic — only semantic mismatches,
i.e. a hand-written page referencing a generated name that changed.

## Third-party libraries (graphs)

No npm emission means the generator cannot install anything.

- [ ] `requiresNpm(name, range)` → emit a manifest and, when a `package.json` is findable, check it and
      fail with the exact `pnpm add` line. Never silent.
- [ ] Prefer not needing it: plain SVG + Vue covers the kontainer graph and vault timings, and the
      current libs are explicitly not sacred.

## Two latent bugs this design escalates

Both in `TsSdkOutput.Scope.resource()` (`sdk/TsSdkOutput.kt:86`) — currently harmless, both bad once
resources become the primary shipping mechanism for an entire frontend.

- [x] `this::class.java.classLoader` is **`ultra:codegen`'s** loader, not the contributor's. Works on a
      flat Gradle classpath; breaks under any isolating loader. A contributor shipping its own resources
      must load from its own — capture `contributor::class.java.classLoader` in the scope.
      **FIXED 2026-07-30** in the review round; `scopeFor` now takes the loader.
- [x] ~~`.bufferedReader()` uses the **platform default charset**.~~ **THIS WAS WRONG** — checked
      2026-07-30 against kotlin-stdlib 2.4.10: `InputStream.bufferedReader(charset: Charset =
      Charsets.UTF_8)` (`jvmMain/kotlin/io/IOStreams.kt:87`). The default already IS UTF-8. Only a bare
      `InputStreamReader(stream)` takes the platform default, and the code does not do that. No action.
      Left visible rather than deleted because this claim was believed by three reviewers and the
      coordinator purely because it was written down here — a cited line number is not a verification.

## Insights specifics

### The tab registry replaces a sealed hierarchy — and loses no type safety

- **Envelope**: `{ collectors: [{ key, data }] }`, `data` opaque. TS sees `Record<string, unknown>`.
- **Per tab**: the collector's `Data` class is a root its own contributor declares, so `models.ts` gets a
  **generated zod schema** for it, and the tab component parses its own slice with that schema.

Open envelope for extensibility, generated schema for safety inside each tab, and an app-defined
collector behaves identically to a built-in one. Strictly better than a discriminated union, which would
force the framework to enumerate every collector.

- [ ] `templateKey` currently derives from the class FQN (`InsightsCollectorData.kt:15`). The frontend
      will hardcode these keys, so a package rename silently orphans a tab. Make the key an **explicit
      declared string**.

### Slumber-compat blockers in the current collector data

The persisted format is Jackson (`InsightsMapper`); an API response is Slumber (`RestCodec`). These do
not agree. Found by reading:

| Where | Problem |
|---|---|
| `AppConfigCollector.Data(val info: Any, val config: Any)` | `Any` — Slumber cannot type it, codegen hard-fails |
| `CollectorData(val cls: String, val data: Map<*, *>)` | star projection |
| `RequestCollector.Data.fullUrl` | computed property: Jackson emits it, `DataClassSlumberer` (ctor params only) does not — silent shape difference between stored file and API |
| `HttpMethod`, `HttpStatusCode` | ktor types; need claims or mapping to String/Int |
| `java.time.LocalDateTime` / `Instant` | Slumber has codecs (`builtin/datetime/javatime/`) but they are **custom**, so codegen needs a `JavaTimeTsContributor` with claims and a parity test. Does not exist yet |
| `UserCollector.Data.user: UserRecord` | sealed hierarchy — works, but pulls the whole auth user model into the ops type graph |

### What deleting the renderers buys

Dropping `renderBar`/`renderDetails` removes `api(project(":ultra:semanticui"))` and
`api(project(":funktor:staticweb"))` from `funktor/insights/build.gradle.kts:72,76` outright — the
staticweb decoupling, as a side effect. Killing the bar additionally removes `InsightsBarTemplate`,
`InsightsGuiTemplate`, `InsightsRenderer`, both `*WebResources`, `InsightsGui`, `InsightsGuiRoutes` and
all five JS/CSS assets under `resources/assets/funktor/insights/`.

- [ ] Check what still calls `getRequestDetailsUrl()` / `getRequestDetailsUri()` on
      `RequestMetricsProvider` before removing them.
- [ ] `InsightsFull`'s hardcoded URI filter (`impl/InsightsFull.kt:33`) excludes `/insights/bar` and
      `/insights/details`; it must exclude the new API route instead.

## PROPOSAL, not scheduled: `expects<T>(tsName)`

**Do not build this yet.** Decision 2026-07-30: build it only if misnaming actually becomes a recurring
problem. Recorded so the idea is not re-derived.

Generated TS names are computed, not literal — monomorphization means `PageOf<Lock>` emits as
`PageOfLock` (`20260729-ts-sdk-codegen.md`, "Generics"). A hand-written component importing
`PageOfLock` is guessing at that, and a wrong guess surfaces as a TypeScript error inside generated
output rather than as something naming the contributor.

The proposal is to let a contributor declare the dependency and have the builder verify it:

```kotlin
claims.expects<GlobalLockEntry>(tsName = "GlobalLockEntry")
```

failing with *"funktor:insights:vue expects TS type `GlobalLockEntry`; the model emits
`GlobalLockEntryV2`"*.

It is exactly `TsTypeClaims` inverted — a claim says *"I own this Kotlin type, here is its TS name"*;
this says *"I depend on this Kotlin type appearing under this TS name"* — so it could share the
registry. **Until then the `vue-tsc` gate is the backstop**: cheaper, loud, but the error lands a layer
away from the cause.

## Ordering

This task depends on `20260729-ts-sdk-codegen.md` Phase 2 (`RestApiTsContributor` + CLI) being done.

1. **Gate the existing insights GUI** — independent, small, do it now so the Vue rewrite is not on a
   security deadline. Keep the kotlinx.html GUI alive but superuser-gated; deleting it now leaves no
   insights UI at all until Vue lands.
2. Codegen Phase 2 (existing task).
3. **The additions above** — profiles, shared emission, registries, `out.vue`, config emission. Before
   any Vue is written: they determine what a Vue contributor looks like.
4. **Split insights data from rendering**, delete the renderers, fix the Slumber-compat problems.
5. **Insights `ApiRoutes`, superuser-only** — `ApiRoutes("insights", authFloor = { isSuperUser() })`,
   the `IntrospectionApi` pattern (`funktor/inspect/.../IntrospectionApi.kt:20`).
6. **Vue foundations** — the dev harness in both modes, the `vue-tsc` gate, the manifest test, and the
   customization contract applied to one throwaway component. Everything after this is repetitive; this
   is where decisions lock.
7. **One vertical slice** — logging or locks, **not** insights. Insights has the most complex renderers
   (kontainer graph, vault profiling) and is the worst first slice.
8. **The bulk** — remaining inspect sections, then insights detail views, then delete
   `funktor/inspect/src/jsMain` (51 files, 5,165 lines of Kraft) and the 33 hand-written API client and
   model files in `funktor/inspect/src/commonMain`.
9. **staticweb cleanup**, once nothing renders server-side HTML.

Note `funktor/cluster` has **no frontend at all** — it is already nine pure `ApiRoutes` classes. The
cluster ops UI is `funktor/inspect/src/jsMain`, mounted by `funktor-demo/adminapp` via
`mountFunktorInspect(ui)` (`nav.kt:86`). Its `commonMain` clients are exactly what codegen replaces, so
the rewrite has a deletion payoff.

## Still open

- [ ] **The customization contract.** Prebuilt components are overwritten on every generate, so an app
      cannot restyle by editing them. Slots + props, CSS custom properties for theming, generated thin
      wrappers the app owns, or an explicit "eject" mode. Needed before component #1 — it shapes how
      every component is authored. Partly narrowed by the decision that the framework ships only
      non-customizable views.
- [ ] **Self-contained styling.** A component injected into an arbitrary app cannot assume a UI
      framework is installed. With Tailwind chosen, settle whether components assume the app has
      Tailwind configured (and its content globs cover the SDK dir) or ship fully scoped CSS.
- [ ] Whether `funktor-demo` eventually becomes a starter template with its own DX story. Further out.

## Test evidence

- [ ] Unit: profile filtering; `out.shared` dedupe vs conflict; registry aggregation is order-independent
      (needs **two** root-supplying contributors — one cannot catch order bugs, see
      `20260729-ts-sdk-codegen.md` "Order-independence had a hole")
- [ ] Unit: a profile that drops the last root reaching a claimed type produces an SDK that neither
      imports nor ships that claim's runtime module — the property that replaces contributor filtering
- [ ] Unit: a claim whose relative `importFrom` nobody emits is a validation error naming the claimant
- [ ] Unit: stale-output deletion — a path written by run 1 and not by run 2 is removed; a file on disk
      that neither run produced is left alone and reported
- [ ] Unit: `scaffold` writes when absent, does NOT overwrite when present, and is excluded from
      `--check` (otherwise the user's own edits report as drift)
- [ ] Manifest test: every `.vue` in a resource dir is declared by a contributor
- [ ] `vue-tsc --noEmit` over a fully generated SDK, as a Gradle gate wired into `check`
- [ ] Mutation-test the gate itself — an always-green gate is worthless
- [ ] Full test command(s) run + green: `...`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

## Follow-ups

- [ ] **DOCS task** — required on archive. Adds a public extension point (Vue contributors, profiles,
      registries) and a new consumer-facing wiring step (the alias). Docs-site page + LLM mirror.
