@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.jsTestDeps

plugins {
    kotlin("multiplatform")
    id("io.kotest")
    id("com.google.devtools.ksp")
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
                api(Deps.Npm { threeJs() })
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
