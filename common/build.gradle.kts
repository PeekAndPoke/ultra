@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.kotest.multiplatform")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

kotlin {
    js {
        browser {
        }
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(Deps.kotlinx_coroutines_core)
                implementation(Deps.kotlinx_atomicfu)
                implementation(Deps.kotlinx_serialization_core)
                implementation(Deps.kotlinx_serialization_json)

                implementation(Deps.ktor_client_core)

                api(Deps.kotlinx_datetime)

                // TODO: Remove this dependency
                //       It is still needed for formatting dates
                implementation(Deps.korlibs_time)
            }
        }

        commonTest {
            dependencies {
                commonTestDeps()
            }
        }

        jsMain {
            dependencies {
                api(Deps.Npm { polyfillFetch() })
                api(Deps.Npm { jsJodaCore() })
                api(Deps.Npm { jsJodaTimezone() })
            }
        }

        jsTest {
            dependencies {
                jsTestDeps()
            }
        }

        jvmMain {
            dependencies {
                implementation(Deps.classindex)
            }
        }

        jvmTest {
            dependencies {
                jvmTestDeps()
            }
        }
    }
}

tasks {
    getByName("compileKotlinJs").dependsOn(":compileCommonMainKotlinMetadata")

    configureJvmTests()
}
