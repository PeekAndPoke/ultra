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
        browser {
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
                api(project(":kraft:core"))
                api(project(":kraft:semanticui"))
                // addons
                api(project(":kraft:addons:avatars"))
                api(project(":kraft:addons:browserdetect"))
                api(project(":kraft:addons:chartjs"))
                api(project(":kraft:addons:jwtdecode"))
                api(project(":kraft:addons:konva"))
                api(project(":kraft:addons:marked"))
                api(project(":kraft:addons:nxcompile"))
                api(project(":kraft:addons:pdfjs"))
                api(project(":kraft:addons:prismjs"))
                api(project(":kraft:addons:signaturepad"))
                api(project(":kraft:addons:sourcemappedstacktrace"))
            }
        }
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

Docs {
    distributeJsExample(dir = "docs/kraft/examples")
}
