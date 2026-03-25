# Plan: Set ES2015 Target for All Kotlin JS Compilations

## Context

All ~40 Kotlin/JS modules in this monorepo currently compile with the default ES5 target. This means Kotlin generates
prototype-based "classes" instead of real ES2015 `class X extends Y` syntax. ES2015 classes are required for
AudioWorklets, WebComponents, and other browser APIs that check the prototype chain. Setting `target.set("es2015")`
fixes this.

## Approach: Centralized Configuration (single file change)

Instead of editing all ~40 module `build.gradle.kts` files, we add the setting once in the root `build.gradle.kts` via
the existing `subprojects` block, plus the root project's own `js` block.

## Changes

### File: `/opt/dev/peekandpoke/ultra/build.gradle.kts`

**1. Add imports** (after existing imports at top):

```kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
```

**2. Add to `subprojects {}` block** (after the archive naming logic, before closing `}`):

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

- `withPlugin(...)` ensures the block only runs on subprojects that actually apply the multiplatform plugin (safe for
  JVM-only/KSP modules)
- `withType<KotlinJsIrTarget>` selects only JS targets, not JVM

**3. Add `compilerOptions` to the root project's own `js` block** (lines 74-82):

```kotlin
    js {
        compilerOptions {
            target.set("es2015")
        }
        browser {
            testTask {
            }
        }
        binaries.executable()
    }
```

That's it — one file, three small insertions.

## Compatibility Notes

- **Published .klib artifacts are unaffected.** `.klib` is IR (intermediate representation). The ES target only affects
  final JS codegen in the consuming project. Downstream projects can still use their own target setting.
- **Browser support:** ES2015 is supported in all modern browsers (Chrome 51+, Firefox 54+, Safari 10+, Edge 15+). Only
  IE11 would break, but it's EOL.
- **`js("...")` raw blocks audited:** All usages are safe (object literals, Array.isArray, dynamic imports,
  Object.getOwnPropertyNames). None rely on ES5 `this`-binding.

## Verification

1. `./gradlew clean build` — full compile + all tests (JS and JVM)
2. `./gradlew :kraft:core:compileKotlinJs --info 2>&1 | grep -i "es2015"` — confirm flag is applied
3. Inspect a generated JS file under `kraft/core/build/compileSync/js/main/` to confirm `class X extends Y` syntax
4. `./gradlew :kraft:examples:hello-world:jsBrowserDevelopmentRun` — smoke-test an executable example in the browser
