@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

val ULTRA_GROUP: String by project
val VERSION_NAME: String by project

group = ULTRA_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

dependencies {
    implementation(kotlin("reflect"))

    api(project(":ultra:common"))
    // Brings ":ultra:reflection" and ":ultra:datetime" in transitively (both declared `api` there).
    // Needed for the type walker (ReifiedKType), polymorphism introspection and the SlumberConfig
    // custom-codec check that guards against silently mis-typed output.
    api(project(":ultra:slumber"))

    // Tests /////////////////////////
    Deps.Test {
        jvmTestDeps()
    }

    // Golden-file comparison of emitted code
    testImplementation(Deps.JavaLibs.diffutils)
    // @SerialName on polymorphic test fixtures — Slumber reads it as a child identifier
    testImplementation(Deps.KotlinX.serialization_core)
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)
}

tasks {
    configureJvmTests()
}

mavenPublishing {
}
