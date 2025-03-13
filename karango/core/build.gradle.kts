@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version Deps.Ksp.version
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
    idea
}

val KARANGO_GROUP: String by project
val VERSION_NAME: String by project

group = KARANGO_GROUP
version = VERSION_NAME

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)

    dependencies {
        implementation(kotlin("reflect"))
        implementation(Deps.KotlinX.coroutines_core)

        implementation(Deps.JavaLibs.Jackson.annotations)

        api(Deps.JavaLibs.ArangoDb.java_driver)

        implementation(project(":common"))
        implementation(project(":kontainer"))
        implementation(project(":logging"))
        implementation(project(":slumber"))
        implementation(project(":vault"))

        // //  tests  ////////////////////////////////////////////////////////////////////////////////////////////////

        kspTest(project(":karango:ksp"))

        Deps.Test {
            jvmTestDeps()
        }
    }
}

tasks {
    configureJvmTests()
}
