@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
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
    implementation(Deps.kotlinx_serialization_core)
    implementation(Deps.kotlinx_serialization_json)
    implementation(Deps.kotlinx_coroutines_core)

    // project deps
    api(project(":common"))
    api(project(":slumber"))
    api(project(":kontainer"))
}

kapt {
    useBuildCache = true
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    configureJvmTests()
}
