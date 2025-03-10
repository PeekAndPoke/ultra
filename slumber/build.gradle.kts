@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

dependencies {
    implementation(kotlin("reflect"))
    implementation(Deps.kotlinx_serialization_core)
    implementation(Deps.kotlinx_datetime)

    api(Deps.classindex)

    api(project(":common"))

    // Testing
    testImplementation(project(":slumber-test-classes"))
    kaptTest(Deps.classindex)

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

kapt {
    useBuildCache = true
}
