# Plan: Set ES2015 Target for All Kotlin JS Compilations

## Status: BLOCKED

The original plan (simple Gradle config change) does not work. Two blockers were discovered during implementation on
2026-04-04.

## Context

All ~40 Kotlin/JS modules in this monorepo currently compile with the default ES5 target. This means Kotlin generates
prototype-based "classes" instead of real ES2015 `class X extends Y` syntax. ES2015 classes are required for
AudioWorklets, WebComponents, and other browser APIs that check the prototype chain.

## Blockers Found

### Blocker 1: Kotlin/JS IR static factory methods (CRITICAL)

When targeting ES2015, the Kotlin/JS IR compiler generates **static factory methods** instead of real constructors for
classes that extend external classes. This is a compiler design choice: ES2015 requires `super()` to be called before
`this` is accessible, and Kotlin needs to run initialization code between the super call and property assignment.

**Generated code (ES2015 target):**

```js
class PreactLLC extends Component {
  // NO constructor() — Kotlin generates a static factory instead:
  static new_io_peekandpoke_kraft_vdom_preact_PreactLLC_2izx5j_k$(props, context, $box) {
    var $this = createExternalThis(this, Component, [props, context], $box);
    $this.props_1 = props;          // Kotlin property
    $this.component_1 =
  ...
    ;        // Kraft component wrapper
    return $this;
  }
}
```

**Why this breaks Preact:** Preact instantiates components with `new Ctor(props, context)`. With ES2015 classes, this
calls the inherited `Component()` constructor (since there's no `constructor()` override), which sets `this.props` but
NOT the Kotlin-specific `this.props_1` or `this.component_1`. Result: `TypeError: this.props_1 is undefined`.

**Impact:** This affects ALL Kraft components — the entire `PreactLLC` bridge is broken.

**Attempted fixes that failed:**

- `Impl::class.js` — returns the ES2015 class, but `new` doesn't call the Kotlin factory
- JS bridge function wrapping the static factory — breaks ES5 backward compatibility (no static factory in ES5 mode),
  and requires expensive runtime reflection (`Object.getOwnPropertyNames` + `startsWith("new_")`)

### Blocker 2: Konva `import { default }` syntax error

The konva addon uses `@JsModule("konva")` which generates `import { default } from 'konva'` in ES2015 mode. This is
invalid JavaScript — `default` is a reserved keyword and cannot be used as a named import binding. Webpack rejects it
at parse time.

**Impact:** Only affects the addons example (which depends on konva). Could be fixed by migrating konva to the addon
registry pattern, but Blocker 1 is the real showstopper.

## Possible Solutions (not yet attempted)

### Option A: Preact `options` hook (moderate effort)

Preact exposes lifecycle hooks via `preact.options`. The `__b` (before diff) or `_diff` hook fires before component
creation. We could intercept and call the Kotlin static factory there.

**Pros:** Doesn't require compiler changes. Localized to the Preact bridge.
**Cons:** Relies on Preact internals (double-underscore APIs). Fragile across Preact versions. Unclear if the factory
can be called at the right point in the diff cycle.

### Option B: Rewrite PreactLLC without Kotlin class inheritance (high effort)

Instead of `class PreactLLC : preact.Component(...)` in Kotlin, create the Preact component class hierarchy entirely
in JavaScript via `js()` blocks. The Kotlin side would only manage the data/state, not extend the external class.

**Pros:** Fully decoupled from Kotlin's class generation strategy. Works with any target.
**Cons:** Significant rewrite of the Preact bridge. Loses Kotlin type safety at the bridge boundary. Hard to maintain.

### Option C: Wait for Kotlin compiler improvement

The static-factory-instead-of-constructor pattern is a known Kotlin/JS IR limitation. JetBrains may address it in a
future Kotlin version (possibly via `@JsExport` improvements or a new annotation for external-class-compatible
constructors).

**Pros:** Zero effort. Clean solution.
**Cons:** Unknown timeline. May never happen for this specific use case.

### Option D: Selective ES2015 — only for leaf modules without Preact components (low effort)

Apply ES2015 target only to modules that don't use the Preact component bridge (e.g., pure utility libraries, workers,
WebComponent modules). The Kraft core and example apps stay on ES5.

**Pros:** Gets ES2015 where it's needed (AudioWorklets, WebComponents) without breaking anything.
**Cons:** Split configuration. Doesn't achieve the "ES2015 everywhere" goal.

## Original Approach (for reference — does NOT work)

### Centralized Configuration (single file change)

Instead of editing all ~40 module `build.gradle.kts` files, add the setting once in the root `build.gradle.kts` via
the existing `subprojects` block, plus the root project's own `js` block.

### Changes

#### File: `/opt/dev/peekandpoke/ultra/build.gradle.kts`

**1. Add imports:**
```kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
```

**2. Add to `subprojects {}` block:**
```kotlin
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            targets.withType<KotlinJsIrTarget> {
                compilerOptions {
                    target.set("es2015")
                }
            }
        }
    }
```

**3. Add `compilerOptions` to the root project's own `js` block:**
```kotlin
    js {
        compilerOptions {
            target.set("es2015")
        }
  // ...
    }
```

## Compatibility Notes

- **Published .klib artifacts are unaffected.** `.klib` is IR (intermediate representation). The ES target only affects
  final JS codegen in the consuming project.
- **Browser support:** ES2015 is supported in all modern browsers (Chrome 51+, Firefox 54+, Safari 10+, Edge 15+).
- **`js("...")` raw blocks audited:** All usages are safe (object literals, Array.isArray, dynamic imports,
  Object.getOwnPropertyNames). None rely on ES5 `this`-binding.
