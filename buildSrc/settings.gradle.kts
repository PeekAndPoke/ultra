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
