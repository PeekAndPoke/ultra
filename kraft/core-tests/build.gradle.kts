@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.kotest")
    id("com.google.devtools.ksp")
}

val KRAFT_GROUP = providers.gradleProperty("KRAFT_GROUP").get()
val VERSION_NAME = providers.gradleProperty("VERSION_NAME").get()

group = KRAFT_GROUP
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
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kraft:core"))
                implementation(project(":kraft:testing"))
            }
        }

        commonTest {
            dependencies {
                commonTestDeps()
            }
        }

        jsMain {
            dependencies {
            }
        }

        jsTest {
            dependencies {
                jsTestDeps()
            }
        }

        jvmMain {
        }

        jvmTest {
            dependencies {
                jvmTestDeps()
            }
        }
    }
}

tasks {
    configureJvmTests()
}
