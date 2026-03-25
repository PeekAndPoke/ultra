---
name: update-kraft-examples
description: Updates the three external Kraft example projects to match the current Ultra/Kraft version. Use when releasing a new version or upgrading Kotlin.
user-invocable: true
argument-hint: "[version] (optional — defaults to current version from gradle.properties)"
---

# Update Kraft Example Projects

Updates the three Kraft example repositories to match the current Ultra/Kraft release:

- `/opt/dev/peekandpoke/kraft-example-helloworld/`
- `/opt/dev/peekandpoke/kraft-example-remote/`
- `/opt/dev/peekandpoke/kraft-example-router/`

## Steps

### 1. Read current versions from the ultra repo

Read these files for the authoritative versions:

- `/opt/dev/peekandpoke/ultra/gradle.properties` → `VERSION_NAME` (Ultra/Kraft version)
- `/opt/dev/peekandpoke/ultra/buildSrc/src/main/kotlin/Deps.kt` → `kotlinVersion`, `Ksp.version`
- `/opt/dev/peekandpoke/ultra/gradle/wrapper/gradle-wrapper.properties` → Gradle version
- `/opt/dev/peekandpoke/ultra/settings.gradle` → current module list (to know which `ultra:*` packages exist)

### 2. Update each example project

For each of the three projects, update these files:

#### `buildSrc/build.gradle.kts`

- Update `org.jetbrains.kotlin:kotlin-gradle-plugin` version to match `kotlinVersion`
- **CRITICAL**: This MUST match the Kotlin version in Deps.kt or everything breaks

#### `buildSrc/src/main/java/Deps.kt`

- `kotlinVersion` → match ultra's `Deps.kotlinVersion`
- `Ksp.version` → match ultra's `Deps.Ksp.version`
- `ultra_version` → match ultra's `VERSION_NAME` from gradle.properties
- `Ultra` object → ensure all current `ultra:*` packages are listed (check settings.gradle for the module list)

#### `gradle/wrapper/gradle-wrapper.properties`

- `distributionUrl` → match ultra's Gradle wrapper version

### 3. Clean and build each project

For each project:

```bash
cd /opt/dev/peekandpoke/kraft-example-[name]
rm -rf buildSrc/build .gradle build kotlin-js-store
./gradlew kotlinUpgradeYarnLock
./gradlew jsBrowserProductionWebpack
```

The build MUST succeed with zero compilation errors.

### 4. Commit

For each project, commit with message:

```
Upgrade to Kotlin [version] and Ultra/Kraft [version]
```

### 5. Do NOT push

Let the user push after testing in the browser.

## Version Locations Quick Reference

| What                | Where                                                      |
|---------------------|------------------------------------------------------------|
| Kotlin version      | `ultra/buildSrc/src/main/kotlin/Deps.kt` → `kotlinVersion` |
| KSP version         | `ultra/buildSrc/src/main/kotlin/Deps.kt` → `Ksp.version`   |
| Ultra/Kraft version | `ultra/gradle.properties` → `VERSION_NAME`                 |
| Gradle version      | `ultra/gradle/wrapper/gradle-wrapper.properties`           |
| Module list         | `ultra/settings.gradle` → all `include()` entries          |

## Common Pitfalls

1. **buildSrc Kotlin version mismatch** — the `kotlin-gradle-plugin` in `buildSrc/build.gradle.kts` MUST match
   `Deps.kotlinVersion`. Two different Kotlin versions = everything breaks.
2. **Yarn lock stale** — always run `kotlinUpgradeYarnLock` after updating dependencies.
3. **New packages** — when ultra:common is split into new modules, add them to the `Ultra` object in Deps.kt.
4. **KSP version** — KSP releases independently from Kotlin. Check the KSP releases page for compatible versions.
5. **Router uses KSP + Mutator** — the router project has additional KSP dependencies that the other two don't.
