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
        implementation(Deps.KotlinX.coroutines_core)
        implementation(Deps.JavaLibs.Jackson.module_kotlin)

        api(platform(Deps.KotlinLibs.MongoDb.mongodb_driver_bom))
        api(Deps.KotlinLibs.MongoDb.mongodb_driver_kotlin_coroutine)
        api(Deps.KotlinLibs.MongoDb.mongodb_bson_kotlinx)

        implementation(project(":ultra:common"))
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
