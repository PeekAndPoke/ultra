# Docs/code sync — remaining items from the 2026-07-28 audit

**Status:** TODO — the BROKEN class is fixed; these are the GAPs and DRIFT left over
**Plan:** none (follow-up to the docs sync audit)
**Security-critical:** no

## Where this came from

A six-agent audit compared every documented library's code, docs pages and LLM mirror. Everything in
the **BROKEN** class (documented API that does not exist or would not compile) is fixed and committed.
What follows is the **GAP** class (real API with no docs) and **DRIFT** (page and mirror disagree).

Two findings are worth remembering as evidence, not just history:

- **The mirrors reproduced errors rather than catching them.** Every Monko defect appeared identically
  in page and mirror, because the mirror was copied from the page instead of checked against code.
- **Name-level checking would not have caught the defects.** `Redirect(Nav.login())` and the `stream()`
  lambda-binding bug both use only real identifiers; they are type errors. Compiling the examples is
  the only check that would have found them.

## Remaining — ranked

### 1. Undocumented behaviour that can crash a user at runtime

- **Monko value-class support is scalar-only.** `unwrapValueClass()`
  (`monko/core/src/main/kotlin/lang/dsl/value_class.kt`) handles String/Number/Boolean-backed value
  classes and throws `IllegalArgumentException` for anything else. A user wrapping an id around a date,
  enum or `Ref` compiles fine and crashes on the first filter. Karango has no such limit (it routes
  through slumber). Both libraries' docs are silent on value classes entirely, and Monko's own
  comparison table frames the two as parallel APIs.

### 2. Shipped API with no documentation

- **Funktor REST:** `ConsistentParam` / caller-binding, two-phase auth evaluation (`AuthPhase`,
  `phase1Denials`), `RouteBootCheck`, `RouteParamsGuard`. The `authFloor` and `forAny/forAll` half is
  now documented; these are not.
- **Funktor auth endpoint table** omits `POST /auth/{realm}/select-org` and
  `POST /auth/{realm}/activate/resend`. The activation prose describes resend without giving its URI.
  `auth.astro` has no endpoint table at all, only the mirror does.
- **Kraft:** form-validation i18n and `suspend`/async rules (`Rule.check`/`getMessage` are suspend and
  built-ins carry `i18nFn`); `kraftApp { i18n(...) }` and the whole `I18nController` / `by Translations`
  surface; `urlParam` / `urlParams` (a fourth state mechanism alongside value/stream/subscribingTo).
- **Slumber:** `@JvmInline value class` support (`ValueClassAwaker`/`ValueClassSlumberer`);
  kotlinx.serialization JSON interop codecs.
- **Kontainer:** `KontainerTracker` (leak/instance tracking) and `KontainerAware`.
- **Vault:** `BatchInsertRepository`, `QueryProfiler`, and the four `vault*` CLI commands.
- **funktor/inspect** — a published module with no doc footprint. Lower priority (admin tooling).

### 3. Page ↔ mirror drift

- **Mutator mirror collapses all six pages into one `Overview` block** — the Gradle/KSP setup and the
  entire "Real-World Examples" page are absent. Worst mirror in the set; wants regeneration.
- **Kontainer mirror** is missing the introspection page (Service Types table, `tools.factory`,
  override-chain walking).
- **Streams mirror** is missing `permanent()` / `permanent(handler)`.
- **Kraft:** the mirror has `threejs` and `ScriptLoader` sections the human page lacks; the human page
  has FormController detail (`formObserver`, `ifValidate`, `numErrors`, programmatic field access) the
  mirror lacks. Both directions.
- **Kraft addons table** omits the real `datetime` addon in both page and mirror.
- **llms.txt** sub-page lists under Karango and Monko omit `aql-functions` and `kontainer-integration`.

### 4. Stale numbers

- Karango claims "131 test files" (`karango/index.astro`, `llms/karango.md`); actual is ~150. Prefer
  deleting the count over updating it — it will rot again.

## Explicitly NOT doing

- A CI structural check — the user declined it for now.
- Compiling doc examples repo-wide. If it happens it should cover only the libraries where a snippet
  compiles standalone (kraft, kontainer, slumber, streams, mutator, datetime, maths, i18n) and skip
  funktor (needs app scaffolding) and karango/monko (need a driver). See
  `buildSrc/src/main/kotlin/ExtractExampleCodePlugin.kt` — it already extracts `<CodeBlock>` regions
  from real compiled Kotlin, but only for the `kraft/examples/*` apps to display their own source, and
  docs-site cannot consume its Kotlin-object output. A JSON emit would be needed to bridge it.
