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

                implementation(project(":commonmp"))
            }
        }

        val commonTest by getting {
            dependencies {
                commonTestDeps()
            }
        }

        val jsMain by getting {
            dependencies {
            }
        }

        val jsTest by getting {
            dependencies {
                jsTestDeps()
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(Deps.jackson_databind)
                implementation(Deps.clikt)

                api(project(":common"))
                api(project(":logging"))
                api(project(":kontainer"))
                api(project(":slumber"))
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
