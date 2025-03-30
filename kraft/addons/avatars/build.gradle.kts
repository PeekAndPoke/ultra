@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.jsTestDeps

plugins {
    kotlin("multiplatform")
    id("io.kotest.multiplatform") version Deps.Test.kotest_plugin_version
    id("com.vanniktech.maven.publish") version Deps.mavenPublishVersion
}

val KRAFT_GROUP: String by project
val VERSION_NAME: String by project

group = KRAFT_GROUP
version = VERSION_NAME

repositories {
    mavenCentral()
}

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
                api(Deps.Npm { minidenticons() })
            }
        }

        jsTest {
            dependencies {
                jsTestDeps()
            }
        }
    }
}
