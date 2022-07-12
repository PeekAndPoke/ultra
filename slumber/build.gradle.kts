@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

repositories {
    mavenCentral()
    // Repo for KlassIndex (https://github.com/matfax/klassindex)
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("reflect"))

    api(Deps.klassIndexLib)
    api(Deps.kotlinx_serialization_core)

    api(project(":common"))
    api(project(":commonmp"))

    // Testing
    testImplementation(project(":slumber-test-classes"))
    kaptTest(Deps.klassIndexProcessor)

    Deps.Test {
        jvmTestDeps()
    }
}

kapt {
    useBuildCache = true
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    configureJvmTests()
}
