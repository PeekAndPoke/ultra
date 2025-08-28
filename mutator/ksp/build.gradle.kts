@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.google.devtools.ksp") version Deps.Ksp.version
    id("com.vanniktech.maven.publish")
}

val MUTATOR_GROUP: String by project
val VERSION_NAME: String by project

group = MUTATOR_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)

    dependencies {
        implementation(Deps.Ksp.symbol_processing)
        implementation(Deps.JavaLibs.Google.auto_service)
        kapt(Deps.JavaLibs.Google.auto_service)

        implementation(project(":mutator:core"))

        // //  tests  ////////////////////////////////////////////////////////////////////////////////////////////////

        testImplementation(Deps.Ksp.compiletesting_ksp)
        kspTest(project(":mutator:ksp"))

        Deps.Test {
            jvmTestDeps()
        }
    }
}

tasks {
    configureJvmTests()
}

mavenPublishing {

}
