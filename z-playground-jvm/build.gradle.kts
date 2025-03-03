@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")

    kotlin("plugin.serialization") version Deps.kotlinVersion
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

repositories {
    mavenCentral()
}

dependencies {
    implementation(Deps.kotlinx_serialization_core)
    implementation(Deps.kotlinx_serialization_json)
    implementation(Deps.kotlinx_coroutines_core)

//    implementation(Deps.ktor_client_core)
    implementation("io.ktor:ktor-client-cio:${Deps.ktor_version}")
    implementation("io.ktor:ktor-client-content-negotiation:${Deps.ktor_version}")
    implementation("io.ktor:ktor-client-core:${Deps.ktor_version}")
    implementation("io.ktor:ktor-client-json:${Deps.ktor_version}")
    implementation("io.ktor:ktor-client-okhttp:${Deps.ktor_version}")

    // project deps
    api(project(":common"))
    api(project(":slumber"))
    api(project(":kontainer"))
}

kapt {
    useBuildCache = true
}

tasks {
    withType<KotlinCompile>().all {
        compilerOptions {
            jvmTarget.set(Deps.jvmTarget)
        }
    }

    configureJvmTests()
}
