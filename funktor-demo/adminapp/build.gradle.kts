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
        compilerOptions {
            target.set("es2015")
        }

        browser {
            testTask {
            }
        }

        binaries.executable()
    }

    sourceSets {
        jsMain {
            dependencies {
                implementation(Deps.Ktor.Client.core)

                implementation(project(":ultra:fixture"))
                implementation(project(":kraft:semanticui"))
                implementation(project(":funktor:all"))

                implementation(project(":funktor-demo:common"))
            }
        }
    }
}
