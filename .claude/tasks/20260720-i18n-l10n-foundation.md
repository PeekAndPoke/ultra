# i18n / l10n foundation — design decisions

**Status:** DESIGN AGREED — ready to cut into implementation tasks (design captured 2026-07-20)
**Plan:** umbrella; feeds email templates, form validation, and server error channels
**Security-critical:** no (but interacts with `.claude/tasks/20260720-exception-disclosure-architecture.md` — see §Interactions)

## Goal

A type-safe, IDE-friendly, translator-tool-friendly i18n/l10n foundation for funktor-as-SaaS-starter.
Motivating downstream: financial products for German customers → translation **and** locale-aware
formatting (money, numbers, dates) both matter.

Target developer experience:

```kotlin
// injected at app root, subscribable like AuthState; carries selected lang + fallback lang + resolver
val i18n by subscribingTo(I18n.stream)

i18n.app.hello(firstName = "Claude", lastName = "Code")   // forced named params
i18n.forms.minLength(length = 5)
```

## State of the world (fleet scan, 2026-07-20)

Greenfield — **no i18n anywhere**. No message bundles, no locale field on any user model, no
`Accept-Language` handling (`funktor-demo/.../server.kt:48` is a CORS allow-list entry nothing reads),
no i18n dependency in any `package.json` or Gradle file. Nothing to migrate off.

Surfaces to eventually cover, measured:

| Area | Size | Location |
|---|---|---|
| Email templates | 2 templates, ~100 words | `funktor/auth/src/jvmMain/kotlin/AuthRealm.kt:68-136` (kotlinx.html builder code) |
| Validation messages | ~28 framework defaults + ~10 ad-hoc | kraft rule library `kraft/core/src/jsMain/kotlin/forms/validation/` |
| Server error text | 15 `.withError` + 8 `AuthApi` handlers | `funktor/auth/.../api/AuthApi.kt`, `funktor/saas/.../OrgsApi.kt`, … |

Helpful existing facts:
- `ApiResponse.messages` is effectively **write-only** — the frontend already discards server text
  and shows its own client strings (`funktor/auth/.../LoginController.kt:515,527,559`). "Server returns
  codes, client translates" is mostly formalizing what the code already does by accident.
- Forced-named-param emission (the `_: Nothing? = null` trick) already exists in the Dart client
  generator (`funktor/rest/.../codegen/DartFunctionWithNamedParameters.kt:59`) — **reference only, do
  not reuse that code**, it's the proof the compiler hack works.
- `ultra/common/.../Placeholders.kt` already implements `{{name}}` (DoubleCurly) substitution.
- Reactive plumbing to copy: `ResponsiveController` / `AuthState` — a `Stream<T>` registered as an app
  attribute, consumed via `by subscribingTo(...)` (`kraft/core/.../utils/ResponsiveController.kt`,
  `funktor/auth/.../AuthState.kt`).

## Ground decisions (AGREED)

### D1 — Bake in, no resource files (for now)

Generate Kotlin source; do **not** load resource catalogs at runtime. Forced by KMP: no
`commonMain/resources` exist in this repo and Kotlin/JS has no synchronous resource API, so a
common `loadCatalog()` would have to be `suspend` — unusable on the synchronous render path
(`kraft/core/.../forms/FormFieldComponent.kt:82`). Verified: all resource reading in the repo is
JVM `ClassLoader`-based and JVM-only.

### D2 — Publish-once per module; compose via extension properties; NO regeneration

Each module generates and publishes **its own namespace once**. `kraft/core` ships `KraftFormsI18n`;
the app ships `AppI18n`. Nobody regenerates anyone else's namespace → no duplicate-class error, no
`compileOnly` stub trick needed. Composition is Kotlin extension properties on the **`I18nTranslate`**
sub-root (see D10 — NOT directly on `I18n`, to keep the root closed):

```kotlin
val I18nTranslate.forms: KraftFormsI18n get() = KraftFormsI18n(this)
val I18nTranslate.app:   AppI18n        get() = AppI18n(this)
```

`compileOnly`-stub + app-regenerates was considered and **rejected**: viable on JVM (servlet-api
pattern) but historically rough on Kotlin/JS-KMP, and unnecessary once nobody regenerates.

### D3 — Override is runtime resolver data, not codegen

The `I18n` instance carries selected lang, fallback lang, resolver, and an ordered list of installed
catalogs. Accessors resolve by key at call time. An app overrides a framework string by putting the
**fully-qualified key** in its own catalog; the app catalog is installed last and wins. Adding a
language the framework never shipped works the same way (per-`(lang, key)`, not per-class). All data
compiled in → zero resource loading, JS-safe.

Kraft's own catalogs **install by default** ("no magic, sensible defaults").

### D4 — Async write / sync read; language switch is `suspend`

- **Read** (`i18n.forms.minLength(...)`) is always synchronous — required by the render path.
- **Switch** (`suspend fun setLang(lang)`) is suspending from v1. Today it completes instantly
  (all baked in); later it becomes the seam where per-language catalogs are `fetch()`ed before the
  stream emits — **no call site or accessor changes**.
- **Fallback language is always baked-in and synchronous** — the safety net when a lazily-loaded
  language is absent or missing a key. Also answers "catalog not loaded yet": render fallback until
  the suspend completes. We accept that the fallback language ships its full map always.

### D5 — Codegen output split: accessor-surface vs catalog-data

Two independent generated artifacts, even though both are compiled in today:
1. **Accessor surface** — function signatures + keys + placeholder params. Tiny, always compiled in.
2. **Catalog data** — the translated strings per language. The weight. Compiled-in `object` today;
   swappable for a fetched asset per language later (behind D4's suspend switch), with the fallback
   language always baked.

This makes "switch to lazy loading at scale" a pure data-delivery change with zero call-site churn.
Rationale: baking 100 langs × 1000s of keys hurts startup parse/alloc, not just transfer — even
compressed. Baking is right for the handful of langs an app targets; lazy loading is the scale answer.

### D6 — Accessors are non-inline top-level extension functions

```kotlin
class KraftFormsI18n(internal val i18n: I18n)                 // marker/receiver
val I18n.forms: KraftFormsI18n get() = KraftFormsI18n(this)
fun KraftFormsI18n.minLength(length: Int, `_`: Nothing? = null) =   // top-level, NON-inline
    i18n.resolve("kraft.forms.minLength", mapOf("length" to length))
```

- **Not inline**: an inline body copied across 1000s of call sites grows output and locks the body
  into consumers (binary-compat hazard on regenerated code).
- **Top-level extension, non-inline**: the Kotlin/JS IR backend DCE's unused non-inline top-level
  declarations, so uncalled accessors are stripped without inline. (Confirm with a bundle-size
  spot-check during implementation.)
- DCE eliminates unused *accessors*, not unused *strings*: catalog data is a reachable map and ships
  wholesale per language. String-level size is controlled by D4/D5 lang-level lazy loading, not DCE.

### D7 — File format: YAML, i18next conventions, `{{var}}`

One file per language, side-by-side (`messages.en.yaml`, `messages.de.yaml`), nested to mirror the
namespace tree. i18next conventions so translator tools (**i18n Ally** / VS Code, **Easy I18n** /
JetBrains) light up. `{{var}}` interpolation (i18next default, and already matches
`Placeholders.kt` DoubleCurly). Plurals via `_one` / `_other` suffixes. A JSON Schema drives IDE
completion + placeholder validation.

```yaml
# kraft/core/src/commonMain/i18n/messages.de.yaml
forms:
  invalidValue:    "Ungültiger Wert"
  minLength_one:   "Muss mindestens {{length}} Zeichen lang sein"
  minLength_other: "Muss mindestens {{length}} Zeichen lang sein"
```

### D8 — Fallback language is the sole API-surface source; ship a key checker

- **Codegen reads ONLY the fallback/main language** to decide which keys → functions exist. A
  translator adding a key only in `de.yaml` can never introduce a function.
- A **checker** (Gradle verification task) diffs every non-fallback language against the fallback and
  reports: **missing** keys, **superfluous** keys, and **placeholder mismatches**. Warn by default;
  fail under CI / `-Pi18n.strict`.
- **Placeholder rule** — `placeholders(non-default) ⊆ placeholders(default)`. The signature's params
  come from the default language only, so its placeholders are always a superset of every other
  language's and the resolver always has a value. Therefore:
  - a non-default language **introducing** a placeholder not in the default → **error** (no param
    exists; would leak literal `{{foo}}` to the user or throw),
  - a non-default language **omitting** a default placeholder → **allowed** (arg is computed and
    passed but unused in substitution — legitimate, e.g. German phrasing that drops a count);
    surfaced as an **info notice**, not a failure.
  - For plural keys the params are the **union** of placeholders across the default's `_one`/`_other`
    variants; each non-default variant must be a subset of that union (same rule, applied per form).

### D9 — Deferred (planned, default impl now, filled later)

Formatting lives under `I18n.format` / `I18nFormat` (D10) — the namespace is reserved now so nothing
moves when the impl is filled in.

- **Locale-aware date/time**: `MpDateTimeFormatter` is hardcoded English (`monthNames = arrayOf(...)`).
  Needs a `kotlinx-datetime` bump (newer version has the locale machinery). Follow-up task.
- **Money / number formatting**: no `Money`/`Currency` type or number formatting beyond `toFixed`
  exists. `I18nFormat` ships with a default (non-localized) impl now; real CLDR-backed formatting
  filled later. Important for the planned German-finance products.

### D10 — `I18n` root splits into `translate` (open) and `format` (closed)

`I18n` exposes exactly two sub-roots so the extensible surface never reaches the root and can't
collide with framework names:

```kotlin
class I18n(val lang: Lang, val fallback: Lang, /* resolver, catalogs */) {
    val translate: I18nTranslate get() = I18nTranslate(this)   // OPEN: module namespaces extend here
    val format:    I18nFormat    get() = I18nFormat(this)      // CLOSED: framework formatters (D9)
}
class I18nTranslate(internal val i18n: I18n)
class I18nFormat(internal val i18n: I18n)
```

- **Why:** translation namespaces are an open extension surface (any module adds `forms`, `app`, …).
  Hanging them off a distinct receiver type (`I18nTranslate`) makes a clash with `format`/`translate`
  structurally impossible, not a naming convention. `I18n`'s root stays closed (two members).
- **Formatting** is framework-owned/closed for v1 (`format.date(...)`, `format.currency(...)`,
  `format.number(...)`); may be opened to module extensions later the same way if needed.
- **Component shortcuts** — delegate providers that resolve the app-context `I18n` (registered as an
  app attribute at bootstrap, like `ResponsiveController`), map to the sub-root, and `subscribingTo`:

  ```kotlin
  val t      by Translations   // provideDelegate → subscribingTo(i18n.stream.map { it.translate })
  val format by Formatting     // provideDelegate → subscribingTo(i18n.stream.map { it.format })
  t.forms.minLength(length = 5)
  format.currency(amount)
  ```

  `provideDelegate`'s `thisRef` is the component. kraft defines `subscribingTo` twice (class vs
  functional components), so the helpers likely need a matching pair — mirror the existing split.
- **Re-render correctness**: `setLang` (D4) must emit a **fresh immutable `I18n` snapshot**, so
  `map { it.translate }` yields a distinct value each switch and no `distinct` operator can suppress
  the update.

## Module layout

- **New `funktor:i18n` KMP module** — the runtime types: `I18n` (state + stream + resolver),
  `I18nCatalog`, `MessageResolver`, formatting interfaces with default impls. NOT `ultra:common`
  (declares five native targets, no kotlinx.serialization).
- **Generated code** lands in each module's `build/generated/i18n`, registered on `commonMain` as a
  srcDir. Demo strings have a natural home in `funktor-demo:common` (KMP, commonMain-only, all four
  apps already depend on it).

## Codegen approach

Gradle `DefaultTask` generating Kotlin into `build/generated/i18n`, on `commonMain` srcDir — **not
KSP** (input is a resource file, not Kotlin symbols; common-metadata KSP output is unproven here —
`mutator/core/build.gradle.kts:76` produces zero files). Build the emitter as a fresh, clean printer
(the roadmap already wants a codegen package extracted, `v1-roadmap.md:303`).

**Avoid the two `ExtractExampleCodePlugin.kt` defects:**
- Declare `@InputFiles` (the yaml) + `@OutputDirectory` properly — up-to-date is content-hash based
  (`gradle.properties`), and that plugin's `doFirst`-only shape neither caches nor invalidates.
- Register the srcDir on `commonMain` explicitly (that plugin uses `.firstOrNull()`, which breaks
  under KMP), and `dependsOn` `compileKotlinJvm`, `compileKotlinJs`, **and** `compileKotlinMetadata`.
- Verify wiring from a **clean** build (compile-on-save watcher can mask a missing task dependency).
- Do not use `--rerun-tasks` for verification (`mutator:core` kapt flakiness, root-caused to
  `kotlin("kapt")` on a non-processor KMP module).

## Interactions

- **`.claude/tasks/20260720-exception-disclosure-architecture.md`** proposes
  `interface HasClientMessage { val clientMessage: String }`. That `String` should become a
  key + args (a `TranslatableMessage`-shaped value) so server errors localize on the client, aligning
  with `ApiResponse.messages` being write-only today. Per user: **that task will be updated if i18n
  lands first** — do not implement `clientMessage: String` as a plain string in the meantime.
- **Validation drift to fix opportunistically**: `Slugs.validationError()` and the `validSlug()` form
  rule check the same regex but return different English strings (`funktor/saas/.../Slugs.kt:44` vs
  `kraft/core/.../string_rules_extra.kt`). i18n keys are the natural single source.
- Two `// TODO: how to translate this?` markers already sit at the seam
  (`kraft/core/.../forms/FormFieldComponent.kt:82`, `kraft/semanticui/.../field_input.kt:196`).

## Proposed task cut (dependency order)

1. **`funktor:i18n` runtime module** — `I18n` (state/stream/resolver), `I18nCatalog`,
   `MessageResolver`, `Lang`, formatting interfaces + default impls, `suspend setLang`.
2. **Gradle codegen task** — yaml (fallback-lang-only key discovery) → accessor-surface +
   catalog-data split (D5/D6), proper inputs/outputs, `commonMain` srcDir. Includes the D8 checker.
3. **Kraft forms seam** — route `Rule`/`FormFieldComponent` messages through `I18n`; kraft catalogs
   install by default; kraft ships `messages.*.yaml`.
4. **Server error channel** — codes over the wire; client resolves. Merge into the
   exception-disclosure task per the interaction note.
5. **Email templates** — near-trivial; swap the ~100 words in `AuthRealm.kt` to generated accessors.
6. **(Deferred)** date/number/money locale formatting — separate tasks (D9).

## Spec (foundation acceptance — refined per task above)

- [ ] `funktor:i18n` module exists; `I18n` is a subscribable stream carrying lang + fallback + resolver
- [ ] `suspend setLang` emits a new state on the stream; subscribed components re-render
- [ ] Codegen reads fallback lang only; emits non-inline top-level extension accessors with forced
      named params; accessor-surface and catalog-data are separate outputs
- [ ] Override works via a later-installed app catalog (runtime, no regeneration)
- [ ] Checker reports missing / superfluous keys / placeholder mismatches; fails under `-Pi18n.strict`
- [ ] YAML + JSON Schema; `{{var}}`; `_one/_other` plurals; i18n-tool-recognized
- [ ] Uncalled generated accessors are DCE'd from the JS bundle (spot-checked)

## Test evidence

- [ ] Codegen golden-file test (diff-based, like `funktor/rest` codegen tests)
- [ ] Runtime resolver: override precedence, fallback-lang miss, placeholder substitution
- [ ] `TestBed.preact { }` — a component re-renders on `setLang`
- [ ] Checker unit tests: missing / superfluous / placeholder-mismatch detection
- [ ] Full: `./gradlew :funktor:i18n:jvmTest :funktor:i18n:jsTest <codegen module>:check`
