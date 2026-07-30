plugins {
    // Plain kotlin("jvm"), NOT `kotlin-dsl`, and NOT versioned here — see `settings.gradle.kts` for both
    // the version and the reason.
    kotlin("jvm")
    kotlin("plugin.sam.with.receiver")
}

// Restores what `kotlin-dsl` did implicitly: Gradle marks `Action<T>` with `@HasImplicitReceiver`, and
// this is what makes `tasks.register<T>("x") { dependsOn(y) }` resolve `dependsOn` on the task.
samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

/** Resolved in `settings.gradle.kts` from the root `gradle.properties`. */
val kotlinVersion: String by extra

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // KGP on the COMPILE classpath: Deps.kt and I18nPlugin.kt import its DSL types. Applying the plugin
    // above only puts it on buildSrc's own buildscript classpath, which is a different thing.
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

    implementation("org.ajoberstar.grgit:grgit-core:5.3.3")

    // For the i18n codegen sources included below. Keep in sync with Deps.JavaLibs.Yaml.snakeyaml.
    implementation("org.yaml:snakeyaml:2.6")

    // `kotlin-dsl` used to supply both implicitly. Every file here imports `org.gradle.kotlin.dsl.*`
    // (register, configure, provideDelegate, ...), which comes from gradleKotlinDsl().
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
}

kotlin {
    // Must not exceed the JVM that runs Gradle, which loads these classes. Cannot use
    // Deps.jvmTargetVersion — that object is this project's own output.
    jvmToolchain(17)
}

// The i18n emitter core lives in :tooling (unit-tested there, self-contained: stdlib + SnakeYAML
// only). buildSrc compiles before all projects and cannot depend on :tooling, so the package is
// source-included here for the I18nPlugin — one source of truth, compiled twice.
//
// It builds on the catalog model in :ultra:i18n — shared with :ultra:codegen, which is published and so
// cannot depend on :tooling — which comes in the same way. Only the `model` DIRECTORY: the rest of that
// module is not needed here, and a minimal included set keeps this compile fast and its failures obvious.
// (Before `kotlin-dsl` was dropped this was also a hard limit — `Locale` uses a Kotlin 2.0 annotation
// that Gradle's embedded compiler could not read. It is now a choice.)
kotlin {
    sourceSets.getByName("main").kotlin.srcDir("../tooling/src/main/kotlin/i18n")
    sourceSets.getByName("main").kotlin.srcDir("../ultra/i18n/src/commonMain/kotlin/model")
}
