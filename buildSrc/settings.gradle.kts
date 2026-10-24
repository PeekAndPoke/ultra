/**
 * buildSrc is its own build, so it does NOT inherit the root `gradle.properties` — the file has to be
 * read directly. This is the one place the Kotlin version reaches buildSrc; `build.gradle.kts` takes it
 * from `extra` rather than reading the file a second time.
 *
 * We deliberately do NOT use the `kotlin-dsl` plugin, which would pin the language version to whatever
 * Kotlin is embedded in Gradle (1.8 as of Gradle 8.10) and make buildSrc unable to compile modern Kotlin
 * — including the `ultra:i18n` model sources it source-includes.
 *
 * `java.util.Properties` is written out in full because Gradle compiles `pluginManagement` in isolation,
 * so a file-level import does not reach inside this block.
 */
pluginManagement {
    // Explicitly typed: getProperty returns a platform type, which makes `version` ambiguous.
    val kotlinVersion: String = java.util.Properties()
        .apply { rootDir.resolve("../gradle.properties").inputStream().use { load(it) } }
        .getProperty("kotlinVersion")
        ?: error("kotlinVersion is missing from the root gradle.properties")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        // Gradle's `Action<T>` parameters are `@HasImplicitReceiver`; without this compiler plugin every
        // `tasks.named("x") { dependsOn(y) }` loses its receiver. `kotlin-dsl` configured it for us.
        id("org.jetbrains.kotlin.plugin.sam.with.receiver") version kotlinVersion
    }

    gradle.beforeProject { extra["kotlinVersion"] = kotlinVersion }
}

// Per-user build outputs (MC-00031). Append to the project's settings.gradle.kts.
//
// Two users build in one checkout (the maintainer's IDE as gerk, the agents as claude). Only a file's owner may
// chmod it, and Gradle's copy and sync tasks set file modes, so shared build/ folders break whichever user did
// not create a file ("Could not set file mode 664 on build/js/yarn.lock"). With this block the agents build into
// <root>/.build-claude/<project path> while the IDE keeps build/.
//
// Active only where the gradle property buildRoot is set, i.e. only in user claude's ~/.gradle/gradle.properties:
//   buildRoot=.build-claude
//   org.gradle.projectcachedir=.gradle-claude
// Do NOT move .kotlin/ with kotlin.project.persistent.dir: it resolves a relative value against the daemon's
// working directory and the compile fails; .kotlin/ only holds short-lived session files both users may share.
//
// Also put this block into buildSrc/settings.gradle.kts (create it if missing) and into every included build's
// settings: each is a build of its own, and the root block does not reach it (spross: without it buildSrc/build
// stayed shared, exactly where the maintainer's clean had failed). .gitignore: `.build-claude/` and
// `.gradle-claude/` unanchored, so they cover buildSrc too.
//
// The new layout is one level deeper than build/ (<root>/.build-claude/_root/js/packages/<pkg> vs
// build/js/packages/<pkg>): any script that climbs a fixed number of levels breaks (spross: a webpack.config.d
// file used __dirname + '../../../../' and webpack failed with "2 errors" and printed neither). Grep
// webpack.config.d/ and karma.config.d/ for '../' and search upward for settings.gradle.kts instead.
// Not covered: files a task writes into SOURCE trees or into TRACKED files (klang: the worklet copy into
// src/jsMain/resources; ultra: kotlin-js-store/yarn.lock, docs examples); those need their own fix.
//
// Tested 2026-10-24 on Gradle 9.7.1 + Kotlin 2.4.21 (testbed + a jvm/js(nodejs) multiplatform module): both
// layouts build and test, neither touches the other's build/ or .gradle/, up-to-date state survives alternating
// runs, `clean` in one layout leaves the other's outputs alone.
providers.gradleProperty("buildRoot").orNull?.let { buildRoot ->
    // Only plain local values inside the action: Gradle isolates it and refuses to capture the script object.
    val outRoot = settings.rootDir.resolve(buildRoot)
    gradle.lifecycle.beforeProject {
        val rel = if (path == ":") "_root" else path.removePrefix(":").replace(':', '/')
        layout.buildDirectory.set(outRoot.resolve(rel))
    }
}
