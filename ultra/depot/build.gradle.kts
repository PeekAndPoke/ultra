@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

val ULTRA_GROUP = providers.gradleProperty("ULTRA_GROUP").get()
val VERSION_NAME = providers.gradleProperty("VERSION_NAME").get()

group = ULTRA_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

dependencies {
    api(project(":ultra:common"))
    api(project(":ultra:datetime"))

    // The file system repository stores a file's metadata as JSON
    implementation(Deps.KotlinX.serialization_json)
    // The encrypting repository waits a moment before it reads a file under way again
    implementation(Deps.KotlinX.coroutines_core)

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
