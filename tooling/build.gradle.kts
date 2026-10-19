@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests
import Deps.Test.jvmTestDeps

plugins {
    kotlin("jvm")
}

val ULTRA_GROUP = providers.gradleProperty("ULTRA_GROUP").get()
val VERSION_NAME = providers.gradleProperty("VERSION_NAME").get()

group = ULTRA_GROUP
version = VERSION_NAME

dependencies {
    implementation(project(":ultra:common"))
    // The i18n key-tree model (LocaleCatalog, I18nModelBuilder) lives there so :ultra:codegen can reach
    // it too — this module is not published, so it cannot be the shared home. `api` because it appears
    // in KotlinEmitter's and I18nChecker's signatures.
    api(project(":ultra:i18n"))
    implementation(Deps.JavaLibs.Yaml.snakeyaml)

    jvmTestDeps()
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)
}

tasks {
    configureJvmTests()
}
