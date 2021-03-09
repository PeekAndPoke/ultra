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

val logback_version: String by project
val classindex_version: String by project
val kotlintest_version: String by project
val klock_version: String by project
val kotlinx_serialization_version: String by project

repositories {
    mavenCentral()
    jcenter()
    // Repo for KlassIndex (https://github.com/matfax/klassindex)
    maven("https://jitpack.io")
}

dependencies {
    api(kotlin("reflect"))

    api(project(":common"))
    api(project(":commonmp"))

    api(Deps.klassIndexLib)

    api("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinx_serialization_version")

    // Testing
    testImplementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotlintest_version")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotlintest_version")

    kaptTest(Deps.klassIndexProcessor)
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
