# Kotlin 2.3.20 SWC Support — What It Is & Whether We Should Adopt It

## Context

The user asked about [KT-80308](https://youtrack.jetbrains.com/issue/KT-80308) /
Kotlin 2.3.20's new SWC support and whether this project would benefit.

**Important clarification first — what SWC support in Kotlin 2.3.20 is NOT:**

- ❌ It does **not** replace webpack. Our webpack pipeline (`webpack.config.d/…`
  customisations in `kraft/examples/*`, `funktor-demo/adminapp`) is untouched.
- ❌ It does **not** replace the Kotlin/JS IR compiler backend.
- ❌ It is **not** a bundler or dev-server change.

**What it actually is:**

SWC is used as an *external transpiler* for the **downlevel step only** — the
stage that converts modern JavaScript (emitted by the Kotlin/JS compiler) into
older browser-compatible JavaScript. Previously the Kotlin/JS compiler did this
itself; now it can delegate that slice of work to SWC (Rust-based, fast).

Activated by a single flag in `gradle.properties`:

```properties
kotlin.js.delegated.transpilation=true
```

Status: **experimental** in 2.3.20. JetBrains plan to make it the default later.
Max ES target is still **ES2015** — same ceiling as today.

## Current state of this repo

- Kotlin **2.3.10** (see `buildSrc/src/main/kotlin/Deps.kt`) — so we need a
  Kotlin upgrade to 2.3.20 before we can even try this.
- **48** Kotlin/JS-targeting modules (ultra core libs, kraft + addons, funktor,
  demos). All consistently configured with `compilerOptions.target = "es2015"`.
- Webpack is used heavily with several custom `webpack.config.d/` dirs.
- No `kotlin.js.*` flags currently set in `gradle.properties`.

## Would we benefit?

**Honest answer: marginally, and not yet.**

Benefits that would actually apply to us:

1. **Modest build-speed gain** on the JS downlevel step. Our bottleneck is
   generally Gradle + IR compile + webpack, not the downlevel stage, so the
   practical delta will be small.
2. **Future-proofing** — this will become the default eventually, so flipping
   it on early surfaces issues while they're cheap to fix.
3. **Unlocks upcoming features** (browserlist DSL, modern syntax in inlined
   `js(...)` blocks) that will land in Kotlin 2.4.x. Not usable today.

Things that do **not** improve right now:

- ES target ceiling stays at ES2015.
- Bundle size / runtime perf of emitted JS is unchanged in real terms (the ~5%
  Wasm-binary and 4.6× string-interpolation wins the release-notes mention are
  Kotlin/Wasm improvements, not Kotlin/JS).
- Webpack config / dev server / HMR — no change.

## Recommendation

**Defer, with a low-cost experiment when we next bump Kotlin.** The feature is
experimental on a version we haven't adopted yet (we're on 2.3.10, target is
2.3.20). The payoff today is small; the real wins arrive with 2.4.x
(browserlist DSL, modern inlined-JS syntax). A premature migration buys us
early-adopter risk across 48 JS modules for a marginal speedup.

## When ready — minimal trial path

1. Bump Kotlin 2.3.10 → 2.3.20 in `buildSrc/src/main/kotlin/Deps.kt`.
    - Verify plugin/stdlib/coroutines/serialization compatibility first.
2. Add to `gradle.properties`:
   ```properties
   kotlin.js.delegated.transpilation=true
   ```
3. Smoke-test on a single JS consumer first (e.g. `kraft/examples/hello-world`)
   before enabling repo-wide:
    - `./gradlew :kraft:examples:hello-world:jsBrowserProductionWebpack`
    - Load the dist in a modern browser and exercise the demo.
4. If OK, run the full JS test suite:
    - `./gradlew jsBrowserTest` across kraft + core-tests.
5. Roll back by removing the flag if anything regresses — it's a single-line
   revert.

## Critical files (reference only, no edits planned in this plan)

- `buildSrc/src/main/kotlin/Deps.kt` — Kotlin version constant.
- `gradle.properties` — where the opt-in flag would live.
- `kraft/examples/hello-world/webpack.config.d/webpack.js` — smoke-test target.
- `kraft/core-tests/` — primary JS test suite for verification.

## Verification

- Prod webpack build succeeds for one module, artifacts load & run in browser.
- `./gradlew jsBrowserTest` all green.
- Bundle size diff (before/after) captured from `build/distributions/*` for a
  representative app to confirm no regression.

## Sources

- [Kotlin 2.3.20 — What's New (official)](https://kotlinlang.org/docs/whatsnew2320.html)
- [KT-80308 on YouTrack](https://youtrack.jetbrains.com/issue/KT-80308)
- [Kotlin 2.3.20 release blog post](https://blog.jetbrains.com/kotlin/2026/03/kotlin-2-3-20-released/)
