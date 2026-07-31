@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version Deps.Ksp.version
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
        implementation(kotlin("reflect"))
        implementation(Deps.KotlinX.serialization_json)
        implementation(Deps.KotlinX.coroutines_core)

        api(platform(Deps.KotlinLibs.MongoDb.mongodb_driver_bom))
        api(Deps.KotlinLibs.MongoDb.mongodb_driver_kotlin_coroutine)
        api(Deps.KotlinLibs.MongoDb.mongodb_bson_kotlinx)

        // `api`: MongoDbConfig.connectionString is a REQUIRED Redacted<String> constructor parameter, so
        // ultra:common is part of this module's published surface — an external consumer cannot even
        // construct a MongoDbConfig without it. See the matching note in karango/core.
        api(project(":ultra:common"))
        implementation(project(":ultra:datetime"))
        implementation(project(":ultra:reflection"))
        implementation(project(":ultra:kontainer"))
        implementation(project(":ultra:log"))
        implementation(project(":ultra:slumber"))
        implementation(project(":ultra:vault"))

        // //  tests  ////////////////////////////////////////////////////////////////////////////////////////////////

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
