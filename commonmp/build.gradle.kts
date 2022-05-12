@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

plugins {
    kotlin("multiplatform")
    id("io.kotest.multiplatform") version Deps.Test.kotest_version
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.dokka")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

repositories {
    mavenCentral()
}

kotlin {
    targets {

        js(IR) {
            browser {
//                testTask {
//                    useKarma {
//                        useChromeHeadless()
//                        usePhantomJS()
//                        useChromiumHeadless()
//                        useFirefoxHeadless()
//                    }
//                }
            }
        }

        jvm {
            compilations.all {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }
        }
    }

    // Note that the Kotlin metadata is here, too.

    configure(listOf(targets["metadata"], jvm(), js())) {
        mavenPublication {
            val targetPublication = this@mavenPublication
            tasks.withType<AbstractPublishToMaven>()
                .matching { it.publication == targetPublication }
                .all { onlyIf { findProperty("isMainHost") == "true" } }
        }
    }

    @Suppress("UNUSED_VARIABLE")
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
                api(npm("whatwg-fetch", "3.6.2"))
                api(npm("@js-joda/timezone", "2.12.0"))
                api(npm("@js-joda/core", "5.2.0"))
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

apply(from = "./../maven.publish.gradle.kts")
