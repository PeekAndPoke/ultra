---
name: karakum-setup
description: Use when someone asks to set up Karakum, generate Kotlin externals from TypeScript .d.ts files, convert a TypeScript library to Kotlin, or wrap a new npm package with typed externals.
argument-hint: [npm package name]
---

## What This Skill Does

Sets up Karakum (TypeScript-to-Kotlin externals generator) in a Kotlin/JS Gradle module so it can generate Kotlin
external declarations from an npm package's `.d.ts` files. Encodes the hard-won lessons from the pixi.js pilot so
future setups skip the trial-and-error.

## Context & Critical Gotchas

**Karakum is alpha-quality.** It works well for small/simple TypeScript libraries but struggles with complex ones
(unhandled cross-file imports, Partial<T>, kotlin-wrappers types leak through). Always evaluate the generated output
before committing to it. Have a fallback plan for manual externals.

**Version compatibility (critical):**

- `1.0.0-alpha.105` (and newer) requires **JVM 21**
- `1.0.0-alpha.50` works with **JVM 17** — use this if your project is on Java 17 (most are)
- Check the project's `jvmToolchain(...)` in the root `build.gradle.kts` before choosing a version

**Config is NOT Gradle DSL.** Karakum is configured via a separate `karakum.config.json` file. The Gradle plugin only
exposes `configFile` and `extensionSource` — all real settings live in the JSON.

**Token replacements use angle brackets:**

- `<nodeModules>` → `<project-root>/build/js/node_modules` (Kotlin/JS centralized node_modules)
- `<buildSrc>` → the Gradle buildSrc directory (NOT useful for output paths)
- `<packageNodeModules>` → per-package node_modules

**Karakum doesn't follow `export *`.** If you point it at a library's barrel `index.d.ts`, it'll generate one nearly
empty file. You must glob ALL `.d.ts` files under the library's lib directory.

**Granularity choice:**

- `"top-level"` → file name conflicts on anything non-trivial ("There are conflicts in file names")
- `"file"` → creates one .kt per .d.ts, mirroring TS structure (100s-1000s of files for big libs)
- `"bundle"` → single output file

**Output path:** Karakum writes to `<projectDir>/<output>`, NOT `<projectDir>/build/<output>`. Use a relative path
like `"generated/karakum"` — it lands in `kraft/addons/yourlib/generated/karakum/`, not under build/.

## Steps

1. **Verify JVM version.** Check the project's `build.gradle.kts` for `jvmToolchain(17)` or `jvmToolchain(21)`.
   Choose Karakum version accordingly:
    - JVM 17 → `1.0.0-alpha.50`
    - JVM 21+ → latest (check Gradle Plugin Portal)

2. **Add the library's npm dep** to `buildSrc/src/main/kotlin/Deps.kt` in the `Npm` section:
   ```kotlin
   // https://www.npmjs.com/package/$1
   fun KotlinDependencyHandler.$1() = npm("$1", "<version>")
   ```

3. **Apply the Gradle plugin** in the target module's `build.gradle.kts`:
   ```kotlin
   plugins {
       kotlin("multiplatform")
       id("io.github.sgrishchenko.karakum") version "1.0.0-alpha.50"
       // ... other plugins
   }
   ```

4. **Create `karakum.config.json`** next to `build.gradle.kts`. Start with this minimal config
   (see `example-config.json` for a fuller template):
   ```json
   {
     "libraryName": "$1",
     "libraryNameOutputPrefix": true,
     "inputRoots": "<nodeModules>/$1/lib",
     "input": "<nodeModules>/$1/lib/**/*.d.ts",
     "ignoreInput": ["<nodeModules>/$1/lib/**/*.min.d.ts"],
     "output": "generated/karakum",
     "packageJson": "<nodeModules>/$1/package.json",
     "granularity": "file"
   }
   ```
   Adjust `inputRoots`/`input` paths to match the library's actual structure (some use `dist/`, `types/`, or root).

5. **Wire up the Gradle file.** Add to the module's `build.gradle.kts`:
   ```kotlin
   // Tell Gradle about the Karakum config file
   karakum {
       configFile.set(layout.projectDirectory.file("karakum.config.json"))
   }

   // Register generated externals as a Kotlin source dir
   // (Karakum writes to <projectDir>/generated/karakum, NOT build/)
   kotlin.sourceSets.named("jsMain") {
       kotlin.srcDir(layout.projectDirectory.dir("generated/karakum"))
   }

   // Make compilation depend on generation
   tasks.named("compileKotlinJs") {
       dependsOn(tasks.named("generateKarakumExternals"))
   }
   ```

6. **Add the npm dep** to the module's `jsMain` dependencies:
   ```kotlin
   jsMain {
       dependencies {
           api(Deps.Npm { $1() })
       }
   }
   ```

7. **Run the generator:**
   ```
   ./gradlew :kraft:addons:$1:generateKarakumExternals --rerun
   ```
   Look for these log lines:
    - `Source files count: N` — how many .d.ts files Karakum found
    - `Covered nodes: N` vs `Uncovered nodes: N` — coverage ratio
    - `Target file: ...` — where output is written

8. **Evaluate the output.** `cd` into `generated/karakum/` and check:
    - How many `.kt` files were generated: `find generated/karakum -name "*.kt" | wc -l`
    - How many unhandled imports: `grep -r "unhandled import" generated/karakum | wc -l`
    - Whether `Partial<T>`, `js.core.Void`, `js.array.ReadonlyArray` appear (these will cause compile errors)

9. **Try to compile:**
   ```
   ./gradlew :kraft:addons:$1:compileKotlinJs
   ```
   If it crashes with `UninitializedPropertyAccessException: lateinit property firType not initialized` — the Kotlin
   compiler is choking on Karakum's output. This is common with complex libraries.

10. **Decide: keep or fall back.**
    - **Keep Karakum** if output compiles cleanly or needs minor fixes you can automate via config
      (`moduleNameMapper`, `importInjector`, `nameResolvers`, `packageNameMapper`).
    - **Fall back to manual externals** if: 100s of unhandled imports, compiler crashes, TS-specific syntax leaks.
      Write a minimal type-only `.kt` file covering just the API you need. Keep `karakum.config.json` as a
      reference for future retries when Karakum matures.

## Output Format

When done, report:

- Karakum version chosen and why (JVM compat)
- Source files count / covered / uncovered
- Whether the generated code compiles
- Your recommendation (keep Karakum output, or fall back to manual externals)

## Notes

- **Don't commit `generated/karakum/` if output is broken.** Add it to `.gitignore` during the pilot phase, only
  commit once you've decided Karakum is viable for that library.
- **Never edit generated files by hand** — they get overwritten. Use Karakum config (plugins, nameResolvers,
  importInjector) to shape the output.
- **For complex libraries** (large surface area, heavy generics, TypeScript utility types): manually-written
  externals are usually faster and more maintainable than fighting Karakum config.
- **Full Karakum config schema** lives at `build/js/node_modules/karakum/build/configuration/configuration.d.ts`
  after `kotlinNpmInstall` runs — read that file to see all available options.
- **JVM 21 requirement for newer versions** is a real blocker — check before upgrading.
