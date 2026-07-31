@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version Deps.Ksp.version
    id("com.vanniktech.maven.publish")
}

val KARANGO_GROUP: String by project
val VERSION_NAME: String by project

group = KARANGO_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)

    dependencies {
        implementation(kotlin("reflect"))
        implementation(Deps.KotlinX.coroutines_core)

        implementation(Deps.KotlinX.serialization_json)

        api(Deps.JavaLibs.ArangoDb.java_driver)

        // `api`: ArangoDbConfig.password is a Redacted<String> in a PUBLIC constructor, so ultra:common
        // is part of this module's published surface. At implementation scope the POM marks it
        // runtime-only and an external consumer cannot write `ArangoDbConfig(password = Redacted("x"))`.
        api(project(":ultra:common"))
        implementation(project(":ultra:datetime"))
        implementation(project(":ultra:reflection"))
        implementation(project(":ultra:kontainer"))
        implementation(project(":ultra:log"))
        implementation(project(":ultra:slumber"))
        implementation(project(":ultra:vault"))

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

mavenPublishing {
}
