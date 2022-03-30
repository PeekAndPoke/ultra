@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests
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
}

dependencies {
    implementation(kotlin("reflect"))

    api(project(":commonmp"))
    api(project(":common"))

    //  code generation  //////////////////////////////////////////////////////////////////////////////////////

    api(Deps.kotlin_compiletesting)
    api(Deps.diffutils)

    api(Deps.kotlinpoet)
    api(Deps.google_auto_service)
    kapt(Deps.google_auto_service)

    //  tests  ////////////////////////////////////////////////////////////////////////////////////////////////

    kaptTest(Deps.google_auto_service)

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
