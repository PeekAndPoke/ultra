@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.jsTestDeps

plugins {
    kotlin("multiplatform")
    id("io.kotest")
    id("com.google.devtools.ksp")
    id("com.vanniktech.maven.publish")
}

val KRAFT_GROUP = providers.gradleProperty("KRAFT_GROUP").get()
val VERSION_NAME = providers.gradleProperty("VERSION_NAME").get()

group = KRAFT_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

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

    sourceSets {
        commonMain {
            dependencies {
                api(project(":kraft:core"))
            }
        }

        commonTest {
            dependencies {
                commonTestDeps()
            }
        }

        jsTest {
            dependencies {
                jsTestDeps()
                implementation(project(":kraft:testing"))
            }
        }
    }
}
