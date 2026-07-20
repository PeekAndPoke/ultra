plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")

    implementation("org.ajoberstar.grgit:grgit-core:5.3.3")

    // For the i18n codegen sources included below. Keep in sync with Deps.JavaLibs.Yaml.snakeyaml.
    implementation("org.yaml:snakeyaml:2.4")

    implementation(gradleApi())
}

// The i18n emitter core lives in :tooling (unit-tested there, self-contained: stdlib + SnakeYAML
// only). buildSrc compiles before all projects and cannot depend on :tooling, so the package is
// source-included here for the I18nPlugin — one source of truth, compiled twice.
kotlin {
    sourceSets.getByName("main").kotlin.srcDir("../tooling/src/main/kotlin/i18n")
}
