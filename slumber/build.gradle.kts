@file:Suppress("PropertyName")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.dokka")
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

    api(project(":common"))
    api(project(":commonmp"))

    api(Deps.klassIndexLib)
    api(Deps.kotlinx_serialization_core)

    // Testing
    kaptTest(Deps.klassIndexProcessor)

    Deps.Test {
        jvmTestDeps()
    }
}

kapt {
    useBuildCache = true
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

apply(from = "./../maven.publish.gradle.kts")
