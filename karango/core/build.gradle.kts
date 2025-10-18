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

        implementation(Deps.JavaLibs.Jackson.annotations)

        api(Deps.JavaLibs.ArangoDb.java_driver)

        implementation(project(":ultra:common"))
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
