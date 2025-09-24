@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.kotest.multiplatform")
    id("com.vanniktech.maven.publish")
}

val FUNKTOR_GROUP: String by project
val VERSION_NAME: String by project

group = FUNKTOR_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)

    dependencies {
        api(Deps.Ktor.Server.webjars)
        api(Deps.Ktor.Server.html_builder)
        api(Deps.KotlinX.wrappers_css)

        api(project(":ultra:semanticui"))
        api(project(":funktor:core"))

        ////  webjars  //////////////////////////////////////////////////////////////////////////////////////////////

        api(Deps.WebJars.jquery)
        api(Deps.WebJars.prismjs)
        api(Deps.WebJars.fomanticui_css) {
            exclude(group = "org.webjars.npm", module = "jquery")
        }
        api(Deps.WebJars.lazysizes)
        api(Deps.WebJars.visjs)

        ////  tests  ////////////////////////////////////////////////////////////////////////////////////////////////

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
