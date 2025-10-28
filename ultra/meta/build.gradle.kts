@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.vanniktech.maven.publish")
}

val ULTRA_GROUP: String by project
val VERSION_NAME: String by project

group = ULTRA_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)
}

dependencies {
    implementation(kotlin("reflect"))

    implementation(project(":ultra:common"))

    //  code generation  //////////////////////////////////////////////////////////////////////////////////////

    api(Deps.Ksp.compiletesting_ksp)
    api(Deps.JavaLibs.diffutils)

    //  tests  ////////////////////////////////////////////////////////////////////////////////////////////////

    Deps.Test {
        jvmTestDeps()
    }
}

tasks {
    configureJvmTests()
}

mavenPublishing {
}
