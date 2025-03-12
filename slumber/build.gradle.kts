@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

val ULTRA_GROUP: String by project
val VERSION_NAME: String by project

group = ULTRA_GROUP
version = VERSION_NAME

dependencies {
    implementation(kotlin("reflect"))
    implementation(Deps.KotlinX.serialization_core)
    implementation(Deps.KotlinX.datetime)

    api(Deps.JavaLibs.classindex)

    api(project(":common"))

    // Testing
    testImplementation(project(":slumber-test-classes"))
    kaptTest(Deps.JavaLibs.classindex)

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
