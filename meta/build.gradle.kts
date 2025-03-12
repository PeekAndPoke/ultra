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

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)
}

dependencies {
    implementation(kotlin("reflect"))

    api(project(":common"))

    //  code generation  //////////////////////////////////////////////////////////////////////////////////////

    api(Deps.Ksp.compiletesting)
    api(Deps.Ksp.compiletesting_ksp)
    api(Deps.JavaLibs.diffutils)
    api(Deps.KotlinLibs.kotlinpoet)

    api(Deps.JavaLibs.Google.auto_service)
    kapt(Deps.JavaLibs.Google.auto_service)

    //  tests  ////////////////////////////////////////////////////////////////////////////////////////////////

    kaptTest(Deps.JavaLibs.Google.auto_service)

    Deps.Test {
        jvmTestDeps()
    }
}

tasks {
    configureJvmTests()
}
