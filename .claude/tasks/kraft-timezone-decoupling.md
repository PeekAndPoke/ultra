# Kraft bundle size: decouple the js-joda timezone DB (+ native tz spike)

## Context

Profiling a Kraft hello-world bundle (1,125 KB raw → 146 KB gzip) showed the single biggest
contributor is the **full `@js-joda/timezone` IANA database (~713 KB raw / ~37 KB gzip)** —
loaded into **every** Kraft app because `kraft:core` calls `initializeJsJodaTimezones()`
unconditionally at app init (`kraft/core/src/jsMain/kotlin/index-jsMain.kt:77`), even for apps
that never touch a date. `@js-joda/timezone` is also now officially deprecated.

Goal: stop forcing timezone data on every app; make it opt-in with size/coverage modes; and
later replace bundled data with the browser-native IANA DB (via `Intl`) so the common case ships
**zero** tz data.

Decision (user): **decouple now via an opt-in Kraft addon, then spike a native (Intl) provider**,
gated by the existing `ultra:datetime` JS test suite. kotlinx-datetime 0.8.0 does NOT change the JS
tz backend (still js-joda; Temporal migration is open issue #501), and 0.7.0 moved `Instant`/`Clock`
to `kotlin.time` — so a version bump is a separate concern, out of scope here.

## Findings that shape the design

- `@js-joda/core` (the engine) comes transitively with kotlinx-datetime; only `@js-joda/timezone`
  (the *data*) is opt-in. Removing the data → JS falls back to SYSTEM zone + fixed offset.
- The npm package ships prebuilt slices (no `exports` restriction, so subpath `@JsModule` works):
  full (713 KB), `1970-2030` (130 KB), `10-year-range` (40 KB), `empty` (6 KB).
- In-repo named-zone usage: **only `kraft:examples:fomanticui`** (`Europe/Berlin` in 4 form demos).
  `adminapp` and `hello-world` use none → they shed the full 713 KB with no opt-in.
- `ultra:datetime` test suite leans heavily on named zones + DST (`Europe/Berlin`,
  `Europe/Bucharest`, `US/Pacific`, …) across ~18 specs → the correctness gate for Phase 2.

## Phase 1 — Decouple into opt-in addon — STATUS: DONE (2026-06-24)

- [x] New module `kraft:addons:datetime` (mirrors `kraft:addons:jwtdecode`): owns the
  `@js-joda/timezone` npm dep and exposes per-mode loaders so DCE bundles only the chosen slice:
  `installFullTimezones()`, `installTimezones1970to2030()` (recommended),
  `installTimezones10YearRange()`. Each returns its own `@JsModule` external object.
- [x] Register `:kraft:addons:datetime` in `settings.gradle`.
- [x] `ultra:datetime/build.gradle.kts`: moved `api(Deps.Npm { jsJodaTimezone() })` from `jsMain`
  → `jsTest`. Kept `JsJodaTimeZoneModule.kt` (used by tests; unreachable in prod → DCE'd).
- [x] `kraft:core/index-jsMain.kt`: removed the `initializeJsJodaTimezones()` import + `init{}` call.
- [x] `kraft:examples:fomanticui`: added the addon dep + `installTimezones1970to2030()` in `main()`.
- [x] Verified (measure the `build/kotlin-webpack/...` output — `build/dist/...` is a stale copy
  unless `jsBrowserDistribution` reran):

  | App | tz needs | raw before→after | gzip before→after |
    |---|---|---|---|
  | hello-world | none | 1,125 → 220 KB | 146 → 69 KB |
  | adminapp | none | 2,262 → 1,549 KB | 438 → 401 KB |
  | fomanticui | named zones | now 130 KB 1970–2030 set (not 713 KB full) | 298 KB |

  `:ultra:datetime:jsTest` and `:kraft:addons:datetime:jsTest` green; packed tz tables absent from
  hello-world/adminapp, present-but-smaller in fomanticui.

### Follow-up / cleanup (optional, not blocking)

- `ultra:datetime` still publicly exposes `initializeJsJodaTimezones()` (full loader), now redundant
  with the addon's `installFullTimezones()`. Could be made test-only/internal later (breaking).
- `build/dist` copies of adminapp/fomanticui were stale during measurement (only `jsBrowserProductionWebpack`
  ran, not the distribution copy) — cosmetic; a full `build` regenerates them.

## Phase 2 — Native (Intl) timezone provider — STATUS: DONE (2026-06-25)

- [x] Custom js-joda `ZoneRulesProvider` backed by `Intl.DateTimeFormat`, zero bundled data:
  `ultra/datetime/src/jsMain/kotlin/NativeTimeZones.kt` → `installNativeTimezones()` (pure Intl +
  `@js-joda/core`, no npm dep). instant→offset read directly from Intl; local→offset (DST
  gap/overlap) via binary-search for the nearby transition, then handed to js-joda.
- [x] **Gate met**: `:ultra:datetime:jsTest` runs against the native provider (no
  `@js-joda/timezone`; installed via `jsTest/.../ProjectConfig.kt`) — all 1732 tests pass (named
  zones + DST: Berlin, Bucharest, US/Pacific, New_York, ancient dates). Removed the old
  `JsJodaTimeZoneModule.kt` + spec.
  - Two bugs found & fixed in `offsetSecondsAt`: JS `Date.UTC` remaps years 0–99 → 1900s (broke
    year-39) — use `setUTCFullYear`; sub-second instants skewed the offset — floor to seconds first.
- [x] Addon re-exports `installNativeTimezones()` alongside the js-joda loaders (native recommended).
- [x] `kraft:examples:fomanticui` switched to native → ships 0 KB tz data (1,522→1,397 KB raw /
  298→280 KB gzip), builds clean.
- [ ] Future: revisit when kotlinx-datetime adopts Temporal (issue #501); Temporal is ES2026,
  shipping in Chrome 144 / Firefox 139, Safari stable expected late 2026.

## Phase 3 — Builder-based init (native by default) — STATUS: DONE (2026-06-25)

- [x] New `AppInitializer` marker interface (`kraft/core/.../components/helpers.kt`), parallel to
  `AutoMountedUi`: `initialize()` + `initPriority`. Collected off `appAttributes` by type and run in
  `KraftApp`'s init (before mount), highest priority first. (Chosen over the addons registry and an
  annotation marker — same pattern as `AutoMountedUi`.)
- [x] `kraft:core` installs **native timezone support by default** via a private
  `NativeTimeZoneInitializer` (priority `Int.MAX_VALUE`), added in `Builder.init`. Zero bundle cost;
  removes the per-app "forgot to install" footgun. Provider-conflict worry resolved: exactly one tz
  initializer runs (stored under a private key), and bundled data is only imported when selected.
- [x] Override via `kraftApp { fullTimezones() / timezones1970to2030() / timezones10YearRange() /
  nativeTimezones() }` — `KraftApp.Builder` extensions in the addon. `Builder.timezones(AppInitializer)`
  is the general hook; custom startup tasks implement `AppInitializer` on any app attribute.
- [x] `fomanticui` needs **no** tz setup now (uses the default) — dropped the addon dep + call.
- [x] `TimeZoneInitializerKey` + `NativeTimeZoneInitializer` are `private` (file-local).

### Remaining

- Manual smoke of fomanticui's date forms = final runtime confidence beyond the unit suite.

## Verification commands

- `./gradlew :kraft:examples:hello-world:jsBrowserProductionWebpack` then check the bundle:
  `grep -c "America/Detroit" build/dist/js/productionExecutable/*.js` should be 0; compare gzip size.
- `./gradlew :ultra:datetime:jsTest` — correctness gate.
- `./gradlew :kraft:examples:fomanticui:compileKotlinJs :funktor-demo:adminapp:compileKotlinJs`.
