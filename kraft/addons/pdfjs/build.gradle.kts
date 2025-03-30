@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.jsTestDeps

plugins {
    kotlin("multiplatform")
    id("io.kotest.multiplatform")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

val KRAFT_GROUP: String by project
val VERSION_NAME: String by project

group = KRAFT_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

kotlin {
    js {
        browser {
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kraft:core"))
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
    }
}
