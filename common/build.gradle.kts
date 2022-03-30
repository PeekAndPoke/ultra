@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

val logback_version: String by project
val kotlintest_version: String by project

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))

    implementation(project(":commonmp"))

    Deps.Test {
        jvmTestDeps()
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    configureJvmTests()
}

apply(from = "./../maven.publish.gradle.kts")
