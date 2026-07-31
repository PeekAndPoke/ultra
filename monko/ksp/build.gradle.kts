@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("com.vanniktech.maven.publish")
}

val MONKO_GROUP: String by project
val VERSION_NAME: String by project

group = MONKO_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)

    dependencies {
        implementation(Deps.Ksp.symbol_processing)

        // Direct: the processor reads @Slumber.Field, which lives here. It also arrives transitively
        // through :ultra:slumber's api edge, but a source reference should not rely on that.
        implementation(project(":ultra:common"))
        implementation(project(":ultra:slumber"))
        implementation(project(":ultra:vault"))
        implementation(project(":monko:core"))

        // //  tests  ////////////////////////////////////////////////////////////////////////////////////////////////

        testImplementation(Deps.Ksp.compiletesting_ksp)
        kspTest(project(":monko:ksp"))
        testImplementation(project(":ultra:meta"))

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
