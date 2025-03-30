@file:Suppress("PropertyName")

import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization") version Deps.kotlinVersion
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
                // addons
                api(project(":kraft:addons:avatars"))
                api(project(":kraft:addons:browserdetect"))
                api(project(":kraft:addons:chartjs"))
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

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

rootProject.plugins.withType<YarnPlugin> {
    rootProject.extensions.findByType<YarnRootExtension>()?.let { ext ->
        ext.lockFileName = "examples-${project.name}.yarn.lock"
    }
}

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
