@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    // Test fixtures only: a `TypedApiEndpoint` needs a `KSerializer` for its response, so realistic
    // route fixtures need @Serializable response models.
    kotlin("plugin.serialization")
    id("com.vanniktech.maven.publish")
}

val FUNKTOR_GROUP: String by project
val VERSION_NAME: String by project

group = FUNKTOR_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

// A SEPARATE module from `funktor:rest`, deliberately. Code generation is a dev-time concern and
// `funktor:rest` is a runtime dependency of every production server — bundling the generator there is
// the mistake Phase 0 of the plan undid for the Dart generator.
//
// Plain `kotlin("jvm")` rather than multiplatform: this reads `ApiFeature` route graphs through JVM
// reflection and writes files. There is nothing here a JS target could run.
dependencies {
    implementation(kotlin("reflect"))

    api(project(":ultra:codegen"))
    // For ApiFeature / ApiRoute / CodeGenHints. Brings ":funktor:core" (TypedRoute, UriPattern)
    // transitively, since funktor:rest declares it `api`.
    api(project(":funktor:rest"))

    // Tests /////////////////////////
    Deps.Test {
        jvmTestDeps()
    }
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)
}

tasks {
    configureJvmTests()
}

mavenPublishing {
}
