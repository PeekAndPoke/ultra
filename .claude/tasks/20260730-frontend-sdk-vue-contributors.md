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

- [ ] ~~Write `<out>/.sdk-manifest.json`~~ — **probably moot, needs the maintainer.** It was proposed
      so a run could delete exactly its predecessor's paths and merely REPORT anything else. But the
      later decision is that the generator owns `<out>/` outright, and `writeTo` wholesale-replaces
      behind the `.funktor-sdk` marker whose own text says "nothing you add survives". The manifest
      would only soften a contract that is already explicit and opted into. Do not build it in a loop.
- [x] **DONE, and this entry was STALE — corrected 2026-08-02.** `--check` already reports a ghost:
      `TsSdkOutput.diffAgainst` walks the directory and flags "on disk but not generated", tested at
      `funktor/codegen/src/test/kotlin/TsSdkGenerateCliCommandSpec.kt:83`. It landed with `d760d9aa`,
      after this line was written. The line survived long enough to be copied into a loop backlog and
      nearly get the feature built twice.

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

- [x] **DONE 2026-08-02** — `TsSdkRegistry`, collected during emit and rendered after, because an
      aggregate is by definition not any one contributor's to write. Keyed inserts; a duplicate path
      is a hard error naming both contributors and both components. Routes sort by path and nav sorts
      by `(order, path)`, so the emitted file is stable — `--check` compares content, and an unstable
      order would report drift that is not real.
      `TsMountEmitter` renders `mount.ts`: `routes`, `navItems`, `mountAll(target)`.
      **`MountTarget` is structural**, so a `vue-router` `Router` satisfies it without the SDK — or
      `ultra:codegen`'s verification toolchain — depending on Vue. Components are emitted as LAZY
      `() => import('./pages/...')`, so an unvisited page stays out of the entry chunk.
      The builder cross-checks that every registered component was actually emitted; it is the only
      place the registry and the output plan are both visible.
- [ ] Config is a registry target, **not** a plain file emit, precisely because it is
      N-contributors-into-one-file. Still open — `TsSdkRegistry` currently carries routes and nav
      only; adding an aggregate is a data class plus a `Scope` method.

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

## Auth is the FIRST contributor of this kind — build it first (2026-07-31)

Full write-up: `.claude/tasks/20260731-sdk-auth-integration.md`. Why it belongs at the head of this
document's queue rather than somewhere in it:

**Auth is the first contributor that ships STATE AND BEHAVIOUR, not just an API client.** Every
mechanism this doc proposes — shared runtime modules, aggregation registries, components shipped from
a jar, `requires(...)` — is exercised by it. Building it first gives those designs a real user;
building it later means retrofitting them around whatever the earlier contributors happened to need.

What is already free: the auth API client is **generated today** (`authClient.ts`, all twelve
endpoints, from the demo run), and JWT injection is already the documented transport-wrapper idiom
(`runtime/http.ts:50-58`) — the same shape the Kotlin side uses via a Ktor `defaultRequest` closure.
The gap is the session state machine, i.e. the TypeScript counterpart of `AuthState` (488 lines,
`funktor/auth/src/jsMain/kotlin/AuthState.kt`).

**One finding that changes the design: do NOT port `AuthState`'s persistence.** It writes the whole
session — JWT included — to `localStorage`, which is a filed, security-critical gap
(`.claude/tasks/20260719-token-storage-hardening.md`): any JS on the origin can exfiltrate the token
and replay it off-machine. A faithful port would re-create a known defect in new code. Storage must be
an injected strategy defaulting to in-memory.

**And it collides with the SSE gap.** `.claude/tasks/20260731-sdk-sse-auth.md` — streams bypass the
transport, so a transport-wrapper token never reaches them. Deciding auth here without deciding that
is how the asymmetry becomes permanent.

## Route access control — pairs with auth (2026-08-01)

`.claude/tasks/20260801-sdk-route-access-control.md`, and its Kotlin counterpart
`.claude/tasks/20260801-kotlin-apiacl-naming-alignment.md`.

Generated client members become **callable AND self-describing** — `route()` wraps each one so it
carries its own `method` and `uri`, which is exactly the key `UserApiAccessMatrix` is indexed by. A
TypeScript `ApiAcl` then answers "may this user call this?" so a frontend can hide a button or render
read-only. The shape is verified: a prototype compiles under `--strict --erasableSyntaxOnly` with the
parameter types, the awaited payload type, and destructuring all intact, and the change is purely
additive so existing call sites are untouched.

**It belongs next to auth, not after it.** The two share a question — *which routes are public?* Auth
needs it so a transport wrapper does not attach a stale token to `signIn`; the ACL needs it because
`getMyApiAccess` is itself `authenticated()`, so a logged-out visitor has no matrix and every route,
including "Sign in", reads as denied. Both currently want route auth metadata that
`RestApiTsContributor` discards (`funktor/rest` has `public()` / `authenticated()`,
`auth/AuthRuleBuilder.kt:48,172`). Decide it once, for both.

## WITHDRAWN: `expects<T>(tsName)`

Proposed and deferred 2026-07-30, then made unnecessary the same day: **real TypeScript generics are
emitted** (`.claude/tasks/20260730-codegen-generic-emission.md`, code done `63f9f186`). The proposal
existed solely because monomorphization turned `PageOf<Lock>` into the computed name `PageOfLock`, which a
component author had to guess. With generics the author writes `PageOf<Lock>` — the same text as the
Kotlin — so there is nothing left to guess and nothing to verify. The `vue-tsc` gate remains the backstop
for the ordinary case of a plain wrong name.

## i18n in the SDK

Analysed 2026-07-30. Nothing implemented; the recommendations below are marked as such.

### Two localization mechanisms exist — only one is frontend-facing

| Mechanism | Where | Goes to the frontend? |
|---|---|---|
| YAML catalogs → typed Kotlin accessors (`I18nPlugin`, `buildSrc/src/main/kotlin/I18nPlugin.kt`) | `src/<sourceSet>/i18n/messages.<locale>.yaml` | **Yes** — this is the one |
| Per-locale renderer functions + markdown resources (`EmailTemplate.renderers`, `funktor/messaging/src/jvmMain/kotlin/templates/EmailTemplate.kt:59`) | `funktor/auth/src/jvmMain/resources/i18n/emails/*.md` | No — emails are rendered server-side and stay there |

Do not route email templates through the SDK. They resolve against a locale the server already has
(`EmailTemplate.rendererFor(preferred)`, same file `:97`) and their output is a sent message, not a view.

**Today's frontend-facing catalog is exactly one:** `KraftForms`, 40-odd form-validation strings in
`kraft/core/src/jsMain/i18n/messages.{en,de}.yaml`. Everything else a Vue frontend needs is greenfield —
the strings for the insights/ops components in step 7+. So this is not a migration, and no back-compat
constrains the TS shape.

### Where the TS is emitted: at SDK-build time, from the live catalog

Three options were weighed.

| Option | Verdict |
|---|---|
| **A.** New `TsEmitter` in `:tooling`, plugin emits TS into jar resources, a contributor copies them | Rejected |
| **B.** Builder emits TS from the *live generated catalog object*, via a shared key-tree model | **Recommended** |
| **C.** Ship the YAML as a resource and re-parse it at SDK-build time | Rejected |

C is worst: it puts a YAML parser on the runtime classpath and re-does work the owning module's build
already did.

A is the obvious one and it is nearly right — it matches the `TsRuntime` resource pattern and needs no
module to move. It loses on two counts:

- **Shape skew.** The TS shape is frozen at each module's build time, so an SDK can mix accessors emitted
  by two framework versions. The Kotlin side bounds this with the `I18nCatalog` interface; TS would need
  the same discipline anyway, so this is manageable but not free.
- **The builder only sees opaque files.** It cannot check anything about the keys — in particular the
  cross-module collision below becomes uncheckable, unless each contributor *also declares* its top-level
  namespaces in a registry, which is a derived claim that can drift from the file it describes.

B keeps one TS emitter, in `ultra/codegen` next to all the other TS emission, evolving in lockstep with
the generator. It sees every key, so the checks are real. And it makes **app-defined catalogs work
identically** — an app's own generated catalog object registers the same way and gets the same accessors
and the same checks, which matters for a framework whose point is that apps extend it.

### What B takes — four prerequisites, all small

- [ ] **Catalog enumeration.** `I18nCatalog` answers point queries only (`template(key, locale)`,
      `ultra/i18n/src/commonMain/kotlin/I18nCatalog.kt:12`), so a contributor holding one cannot dump it.
      The generated object already holds the full map privately (`tooling/src/main/kotlin/i18n/KotlinEmitter.kt:42`).
      Add a narrow second interface — `EnumerableI18nCatalog { fun all(): Map<String, Map<String, String>> }` —
      implemented by generated catalogs and `MapI18nCatalog`. Not on `I18nCatalog` itself: a hand-written
      or remote catalog need not be enumerable.
- [ ] **Move the key-tree model into `ultra/i18n`** — see "Where the shared classes live" below.
- [ ] **Frontend-facing catalogs must be JVM-visible.** The SDK builder runs on the JVM and reads a
      classpath. `kraft/core` generates into `jsMain` (`kraft/core/build.gradle.kts:40`,
      `sourceSet.set("jsMain")`), which no JVM artifact carries. Move it to `commonMain` when those
      strings are needed — kraft/core has a `jvm()` target so this is a one-line change. This is a hard
      requirement, not a preference, and it applies to option A equally.
- [ ] **The generated catalog must carry two pieces of metadata:** its `moduleName` and its
      `fallbackTag`. These are the ONLY things the Kotlin pass drops (see "The first pass is lossless"
      below), and the TS emitter needs both — the fallback catalog is the sole source of the API surface
      (D8), and the runtime object cannot currently tell which of its locales that is.
- [ ] **`TsRuntime.Module.I18n`** — a hand-written `ts/runtime/i18n.ts` mirroring `MessageResolver`
      (`ultra/i18n/src/commonMain/kotlin/MessageResolver.kt`): the locale chain
      (`locale → base → fallback → fallback.base`, distinct, `:26`), catalog precedence inner
      (`:53-60`), single-pass `{{name}}` substitution (`:63`), plural suffix lookup with `_other`
      fallback and base-key-as-miss-marker (`:45-51`), plus `Locale.parse`/`base`/`tag`. ~60 lines,
      same shape as the four existing modules in `ultra/codegen/src/main/resources/ts/runtime/`.

### The first pass is lossless — the Kotlin pipeline can feed the TS one

Raised by the maintainer 2026-07-30: *"if we process the catalog on the Kotlin side like we currently do
and can then convert this into a ts contributor, this would be nice. But the first pass drops information
so probably not possible."* Checked — **it does not drop what matters.**

`I18nModelBuilder.build()` is a pure function of the fallback catalog's flat entries: the namespace tree
comes from splitting dotted keys, the placeholder lists from regexing the templates, the plural flags from
the `_one`/`_other` suffixes. Every one of those inputs survives verbatim in the generated object —
`KotlinEmitter.kt:42` bakes `data: Map<String, Map<String, String>>` with **all** locales and **all**
templates, in YAML order (`mapOf` returns a `LinkedHashMap`, so emitted TS is deterministic), and the
Kotlin string escaping (`KotlinEmitter.kt:139`) is a lossless encoding. Re-running the model builder over
the enumerated data reproduces the identical tree.

What IS dropped is build **config**, not content: `fallbackLang` and `moduleName` from `I18nGenConfig`.
Hence the metadata prerequisite above — two members on the generated object. `requiredLocales` and the
checker's findings are also dropped and are correctly irrelevant, being build-time diagnostics.

### Where the shared classes live

Two options were considered for the `:tooling` inaccessibility problem — `:tooling` is **unpublished**
(`tooling/build.gradle.kts`: plain `kotlin("jvm")`, no publish plugin), so a published `ultra:codegen`
cannot depend on it.

**Unifying `:tooling` into `ultra/codegen` is rejected.** `ultra/codegen` is published and depends on
slumber + kotlin-reflect; moving a YAML parser and a *Kotlin* source emitter into it ships build tooling
to every consumer who wanted a TS SDK, and inverts the dependency sense — the i18n Kotlin emitter has
nothing to do with the TS generator.

**Move the model into `ultra/i18n`, and only the model.** DONE 2026-07-30 —
`.claude/tasks/20260730-i18n-model-to-ultra-i18n.md`. The dividing line is *is this a fact about i18n, or
a fact about generating code from it*:

| Moved to `ultra/i18n/…/model/` | Stayed in `:tooling` |
|---|---|
| `I18nNode` / `I18nNamespace` / `I18nMessage` | `YamlCatalogParser` (needs snakeyaml) |
| `I18nModelBuilder` | `KotlinEmitter`, `CodeWriter`, `KotlinNames` |
| `LocaleCatalog` | `I18nChecker` — build-time diagnostics |
| the `{{name}}` pattern, the plural suffixes, the locale-tag grammar | `I18nGenConfig`, `GeneratedFile` — build config |

**A language-level constraint shaped the first attempt, and has since been removed.** buildSrc
source-includes these files and was compiled by the Kotlin embedded in **Gradle** — 8.10 → 1.9.24, with
`kotlin-dsl` pinning the language version to **1.8**. `Locale` uses `@ConsistentCopyVisibility` (Kotlin
2.0), so it could not be compiled into buildSrc at all. That is *why* `normalizeLocaleTag` existed and why
`LocaleCatalog` carries a `String` tag — neither was an oversight. My first attempt "improved"
`LocaleCatalog` to hold a `Locale` and failed to compile.

**Then buildSrc was taken off `kotlin-dsl`** (same day, at the maintainer's request) and Gradle bumped
8.10 → 9.5.0 — the highest KGP 2.4.10 fully supports, deliberately not the newest 9.6.1. buildSrc now
compiles at 2.4.10 and the island is gone, verified by compiling `Locale.kt` inside it. Details and the
non-obvious part of the swap — `kotlin-dsl` also configures the `sam-with-receiver` compiler plugin, and
without it every `Action<T>` lambda in buildSrc loses its receiver — are in the move's task file.

So the `String` tag is now legacy rather than necessary; changing it is a recorded follow-up, not a
constraint. The surviving rule is narrower: **`model/` references nothing outside its own directory**,
enforced by the buildSrc compile and documented at the top of `model/I18nCatalogModel.kt`.

The duplication is removed regardless: `model/` owns `splitLocaleTag`, and both `Locale.parse` and
`normalizeLocaleTag` go through it. One grammar, two callers, pinned by `PluralSuffixParitySpec`.

### Emitted shape

The namespace tree becomes nested objects; the messages become functions taking **one options object**.
That is strictly better than the Kotlin side, whose `vararg forcedNamed: Unit` trick
(`KotlinEmitter.kt:100`) exists only to force named arguments — TS gets that by construction, plus
errors on unknown and missing keys.

```ts
t.forms.minLength({ count: 3 })        // plural: count is a normal member of the options object
t.forms.notEmpty()                     // no placeholders → no argument
t.fixture["greet-user"]({ "first-name": "Sam" })
```

**Quote every key in the emitted object literal.** Then TS reserved words and hyphenated keys need no
name mangling at all (`{ is: ..., "greet-user": ... }` is legal, and bracket access is callable), so no
`TsNames` sibling to `KotlinNames.kt` is needed. Only the per-module export name must be an identifier.
This removes an assumed blocker — the fixture's deliberately hostile keys (`` `is` ``,
`` `greet-user` `` with a `` `first-name` `` placeholder, `tooling/i18n-fixture/.../messages.en.yaml`)
work without special handling.

### Catalog precedence must be DECLARED DATA, not contributor order

The strongest finding here. Two invariants collide:

- The codegen contract deliberately makes contributor order **structurally irrelevant** — every operation
  is a keyed insert (`20260729-ts-sdk-codegen.md`).
- Catalog precedence is **order-dependent**: `I18n.Builder.build()` reverses the install list so the
  last-installed wins, which is how an app overrides a framework string (D3,
  `ultra/i18n/src/commonMain/kotlin/I18n.kt:61`).

Aggregate them by contribution order and whether an override takes effect depends on DI iteration order —
a silent, per-key, per-locale bug. So a registry entry carries an explicit layer (`Framework` < `App`),
and the builder sorts by it. Ties within a layer are a hard error, not a coin flip.

- [ ] Registry entry: `i18n(module, layer, catalog)`. Emission order derives from `layer`, never position.
- [ ] A **top-level namespace claimed by two modules is a hard error naming both.** Kotlin gets this free
      (two `val I18nTranslate.forms` extensions are an ambiguity at the call site); merging TS objects
      would silently drop one. Same semantics, earlier and clearer error.

### i18n is an emit action + a registry, not a contributor kind

Given "a profile selects ROOTS, never contributors" (§1), an i18n *contributor* would have no root to
condition on — a catalog is not a type — so it could not be profiled at all. The fix falls out of the
same rule already applied to pages: **whichever contributor ships a component also ships that
component's strings**, and both are conditional on the same surviving root. One emit condition, so a
component can never ship without its strings or vice versa.

That matters because the failure is quiet: a missing catalog does not error, it renders the raw key
(`MessageResolver.resolve` returns `key` on a miss, `:33`). A mis-tagged separate i18n contributor would
show `forms.minLength` in the UI rather than failing the build.

Modules with strings but no components (a shared base catalog) still register directly; those are
framework-layer and always ship.

- [ ] Add a 6th row to the aggregation-registry table in §3: the i18n catalog set + merged accessor root.

### The one wire-format decision — and it needs the maintainer

`Message.text` is a **resolved string** today (`ultra/codegen/src/main/resources/ts/runtime/apiResponse.ts`,
mirroring `ultra/remote/.../ApiResponse.kt`). So the server currently translates. With a Vue SPA that is
wrong in three ways: a language switch cannot re-render an existing message without a round-trip; the
server does not reliably know the user's language (a pre-auth request has only `Accept-Language`, a hint);
and the response becomes locale-dependent, which is a caching loss.

**Recommendation: keys on the wire.** `Message` grows `key` + `args` and the client resolves; `text`
stays as the already-resolved fallback so curl, logs and non-SDK consumers keep working, with the client
preferring `key` when present. Purely additive. Server-rendered output (emails) keeps resolving
server-side — it already does, with its own mechanism, and that stays correct.

Consequence to accept knowingly: every server-sent message key becomes public API, and the framework's
message catalog must then ship in the SDK.

Also needed either way: the generated `HttpTransport` should send the client's locale as a header, so
anything the server *does* resolve matches what the client would have shown.

### Locale chunking — deferrable, and the seam already exists

Baking every locale of every module into one bundle is what the Kotlin side does and is fine at framework
scale, but it is the whole translation corpus in every app. Recommended: **one TS file per locale plus a
lazy `import()`**, with the fallback locale bundled eagerly so nothing can ever render empty. Vite
code-splits this for free.

This costs nothing to defer: the per-locale file layout is the same either way, only the index differs.
And the async seam is already designed on the Kotlin side — `I18nController.setLang` is `suspend`
"the seam where a language's catalog would be fetched before the new snapshot is emitted once lazy
loading lands" (`kraft/core/src/jsMain/kotlin/i18n/I18nController.kt:85`). The Vue composable mirrors it:
`await setLang(locale)`.

### Rules to write down before the first string is rendered

- **A translated string is text, never markup.** Vue's `{{ }}` escapes; `v-html` on a resolved string is
  XSS, and a catalog is translator-editable data. Messages needing a link use component interpolation
  (slots), not `<a>` in the YAML. The email side hit the same problem and solved it with
  `HtmlValueSubstitution` (`funktor/messaging/src/jvmMain/kotlin/templates/`) — the frontend answer is
  slots, not an escaper.
- **Parity applies to translation, not formatting.** `I18nFormat` is a deliberate stub
  (`ultra/i18n/src/commonMain/kotlin/I18nFormat.kt:10`) while TS has `Intl` for free. The TS side will be
  *better* than Kotlin here, so formatted output must never enter the parity corpus.
- **The checker needs no TS counterpart.** `I18nChecker` runs on the parsed catalogs in the owning
  module's build, before either emitter, and the fallback catalog is the sole API surface (D8). The TS
  surface inherits its guarantees unchanged. Good news worth stating so nobody builds a second checker.

### The parity test, and the corpus that already exists

The claim "the TS resolver behaves like the Kotlin one" is the one that must not drift: a divergence
means the frontend shows different text than the server for the same key, silently, in one language only.

`tooling/i18n-fixture` is already the corpus — a KMP module whose accessors are generated, with a spec
pinning six behaviours (language selection, regional override + inheritance, plurals, nested namespaces,
keyword/hyphen keys, fallback chain to `en` for an absent language). Extend it rather than writing a
second corpus:

- [ ] A single declarative expectation table (locale, accessor path, args, expected string) read by
      **both** the Kotlin spec and the node harness in `ultra/codegen/ts-verify/`. Two hand-maintained
      lists would drift, which is the failure this test exists to catch.

### Consolidation the TS mirror forces

DONE 2026-07-30 as part of the model move. Three facts were each declared two or three times, and the TS
runtime would have made it four:

- The placeholder pattern `\{\{([a-zA-Z0-9_-]+)\}\}` — was in `MessageResolver`, `I18nChecker` and
  `I18nModelBuilder`. Widen it in one place only and the checker stops seeing placeholders the resolver
  substitutes: the ERROR it exists to raise ("introduces placeholder not in the fallback") silently stops
  firing. Now `I18nPlaceholders`.
- The plural suffix set — was in `I18nModelBuilder`, `I18nChecker` and `PluralCategory`'s enum values.
  Now `pluralSuffixes`, which still cannot be *derived* from the enum (the enum is outside `model/`), so
  `PluralSuffixParitySpec` pins them equal — a mutation dropping one suffix fails two of its assertions.
- The locale-tag grammar — was in `Locale.parse` and `YamlCatalogParser.normalizeLocaleTag`, the latter
  admitting the duplication in its KDoc. Now `splitLocaleTag`, with both going through it and the spec
  asserting `normalizeLocaleTag(x) == Locale.parse(x).tag` across eight tag shapes.

The TS resolver will consume the same three declarations rather than adding a fourth copy of each.

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

**Where i18n slots in.** The TS emission itself is a prerequisite of **step 7** — the first component with
user-visible text — not of the generator or of the insights API. Two pieces of it belong earlier because
they are cheap now and expensive later:

- the **layer/precedence requirement** goes into step 3, when the registry is designed. Retrofitting it
  means reworking the registry's contract.
- the **`Message` key-vs-text fork** should be settled before step 5 stabilizes the API shape. It is
  additive, so it is not a blocker — but doing it after clients exist means changing them.

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
- [ ] **Does `Message` carry a resolved string or a key + args?** See the i18n section. Recommendation is
      keys with `text` kept as a fallback, but it is a wire-format change and the maintainer's call.
- [ ] Whether the SDK bakes all locales or emits per-locale chunks with a lazy loader. Deferrable — the
      file layout is the same either way, only the index differs.

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
- [ ] i18n: **resolver parity** — one expectation table driving both the Kotlin spec in
      `tooling/i18n-fixture` and the node harness. Must cover the miss marker (unknown key returns the
      key), the `_other` plural fallback, and a regional variant inheriting from its base
- [ ] i18n: two modules claiming the same top-level namespace is a hard error naming both
- [ ] i18n: emission order derives from the declared layer, **not** contributor order — assert by
      registering an app-layer catalog *before* a framework-layer one and checking the app still wins
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
