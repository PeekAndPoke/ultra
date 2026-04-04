@file:Suppress("PropertyName")

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val GROUP = "io.peekandpoke.kraft.examples"
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

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    sourceSets {
        jsMain {
            dependencies {
                // project deps
                api(project(":kraft:semanticui"))
            }
        }
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

Docs {
    distributeJsExample(dir = "docs/kraft/examples")
}
