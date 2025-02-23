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

    js {
        browser {
        }
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = Deps.jvmTarget.target
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(Deps.kotlinx_coroutines_core)
                implementation(Deps.kotlinx_serialization_core)
                implementation(Deps.kotlinx_serialization_json)

                implementation(Deps.ktor_client_core)

                // We expose kotlinx-datetime as it is needed in many cases, e.g. for TimeZone
                api(Deps.kotlinx_datetime_common)

                // TODO: Remove this dependency
                //       It is still needed for formatting dates
                implementation(Deps.korlibs_time_common)
            }
        }

        val commonTest by getting {
            dependencies {
                commonTestDeps()
            }
        }

        val jsMain by getting {
            dependencies {
                api(Deps.Npm { polyfillFetch() })
                api(Deps.Npm { jsJodaCore() })
                api(Deps.Npm { jsJodaTimezone() })
            }
        }

        val jsTest by getting {
            dependencies {
                jsTestDeps()
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(Deps.classindex)
            }
        }

        val jvmTest by getting {
            dependencies {
                jvmTestDeps()
            }
        }
    }
}

tasks {
    configureJvmTests()
}
