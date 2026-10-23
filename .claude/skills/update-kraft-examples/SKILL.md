---
name: update-kraft-examples
description: Updates the Kraft examples repo (helloworld, router, remote) to match the current Ultra/Kraft version. Use when releasing a new version or upgrading Kotlin.
user-invocable: true
argument-hint: "[version] (optional — defaults to current version from gradle.properties)"
---

# Update the Kraft examples

All Kraft examples live in one repo, `/opt/dev/peekandpoke/kraft-examples/`, one folder each (ULTRA-00235). They
build against the PUBLISHED artifacts, so run this only once the release is on Maven Central.

## Steps

### 1. Read the current versions from ultra

| What                | Where                                                      |
|---------------------|------------------------------------------------------------|
| Ultra/Kraft version | `ultra/gradle.properties` → `VERSION_NAME`                 |
| Kotlin version      | `ultra/buildSrc/src/main/kotlin/Deps.kt` → `kotlinVersion` |
| KSP version         | `ultra/buildSrc/src/main/kotlin/Deps.kt` → `Ksp.version`   |
| Ktor version        | `ultra/buildSrc/src/main/kotlin/Deps.kt` → Ktor            |
| Gradle version      | `ultra/gradle/wrapper/gradle-wrapper.properties`           |

### 2. Update `kraft-examples/gradle/libs.versions.toml`

`ultra`, `kotlin`, `ksp` and `ktor` under `[versions]` — the one place for every example. If ultra's Gradle version
moved, update `kraft-examples/gradle/wrapper/gradle-wrapper.properties` → `distributionUrl` too.

### 3. Clean and build

```bash
cd /opt/dev/peekandpoke/kraft-examples
rm -rf .gradle build */build kotlin-js-store
./gradlew kotlinUpgradeYarnLock
./gradlew build
```

The build MUST succeed for all three examples.

### 4. Commit, do NOT push

One commit: `Upgrade to Kotlin [version] and Ultra/Kraft [version]`. The maintainer pushes after testing in the
browser.

## Common pitfalls

1. **Yarn lock stale** — always run `kotlinUpgradeYarnLock` after updating dependencies.
2. **KSP version** — KSP releases independently from Kotlin; take the version ultra builds with.
3. **Router uses KSP + Mutator** — `router/build.gradle.kts` has the `kspJs` dependency the other two don't.
4. **A new example** needs only its folder and one `include()` line in `settings.gradle.kts`; it picks up the
   versions from the catalog.
