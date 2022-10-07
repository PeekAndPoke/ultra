@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

plugins {
    kotlin("multiplatform")
    id("io.kotest.multiplatform") version Deps.Test.kotest_plugin_version
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

repositories {
    mavenCentral()
}

kotlin {
//    targets {

        js(IR) {
            browser {
            }
        }

        jvm {
            compilations.all {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }
        }
//    }

    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation(Deps.kotlinx_serialization_core)
                implementation(Deps.kotlinx_serialization_json)
                implementation(Deps.kotlinx_coroutines_core)

                // This is still needed for formatting dates - TODO: remove this dependency
                implementation(Deps.korlibs_klock_common)

                // We expose kotlinx-datetime as it is needed in many cases, e.g. for TimeZone
                api(Deps.kotlinx_datetime_common)
            }
        }

        val commonTest by getting {
            dependencies {
                commonTestDeps()
            }
        }

        js().compilations["main"].defaultSourceSet {
            dependencies {
                api(Deps.Npm { polyfillFetch() })
                api(Deps.Npm { jsJodaCore() })
                api(Deps.Npm { jsJodaTimezone() })
            }
        }

        js().compilations["test"].defaultSourceSet {
            dependencies {
                jsTestDeps()
            }
        }

        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(Deps.klassIndexLib)
            }
        }

        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                jvmTestDeps()
            }
        }
    }
}

tasks {
    configureJvmTests()
}
