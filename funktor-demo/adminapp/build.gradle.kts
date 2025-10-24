@file:Suppress("PropertyName")

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val GROUP = "io.peekandpoke.funktor_demo"
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

kotlin {
    js {
        browser {
        }

        binaries.executable()
    }

    sourceSets {
        jsMain {
            dependencies {
                implementation(Deps.Ktor.Client.core)
                implementation(Deps.Ktor.Client.content_negotiation)
                implementation(Deps.Ktor.Common.serialization_kotlinx_json)

                implementation(project(":kraft:semanticui"))
                implementation(project(":funktor:all"))

                implementation(project(":funktor-demo:common"))
            }
        }
    }
}
