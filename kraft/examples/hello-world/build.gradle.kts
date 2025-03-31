@file:Suppress("PropertyName")

import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

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
            }
        }
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

Docs {
    distributeJsExample(dir = "docs/kraft/examples")
}

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

// https://stackoverflow.com/questions/72731436/kotlin-multiplatform-gradle-task-jsrun-gives-error-webpack-cli-typeerror-c/72731728
// Fixes webpack-cli incompatibility by pinning the newest version.

rootProject.plugins.withType<YarnPlugin> {
    rootProject.extensions.findByType<YarnRootExtension>()?.let { ext ->
//        ext.resolution("webpack-cli", "^4.10.0")
        ext.lockFileName = "examples-${project.name}.yarn.lock"
    }
}

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
