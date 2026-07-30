import java.util.Properties

plugins {
    `kotlin-dsl`
}

/**
 * The one place the Kotlin version is written down is the root `gradle.properties`.
 *
 * buildSrc compiles before every project, so it cannot read `Deps` — that object is its own output.
 * And the root `gradle.properties` is NOT inherited here, so `providers.gradleProperty` comes back
 * empty; the file has to be read directly. Everything else takes the same property through the
 * normal Gradle mechanism.
 */
val kotlinVersion: String = Properties()
    .apply { file("../gradle.properties").inputStream().use { load(it) } }
    .getProperty("kotlinVersion")
    ?: error("kotlinVersion is missing from the root gradle.properties")

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

    implementation("org.ajoberstar.grgit:grgit-core:5.3.3")

    // For the i18n codegen sources included below. Keep in sync with Deps.JavaLibs.Yaml.snakeyaml.
    implementation("org.yaml:snakeyaml:2.6")

    implementation(gradleApi())
}

// The i18n emitter core lives in :tooling (unit-tested there, self-contained: stdlib + SnakeYAML
// only). buildSrc compiles before all projects and cannot depend on :tooling, so the package is
// source-included here for the I18nPlugin — one source of truth, compiled twice.
kotlin {
    sourceSets.getByName("main").kotlin.srcDir("../tooling/src/main/kotlin/i18n")
}
