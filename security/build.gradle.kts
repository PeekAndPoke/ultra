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
        browser()
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
//    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation(Deps.kotlinx_serialization_core)
                api(project(":commonmp"))
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
                implementation(kotlin("reflect"))
                implementation(project(":kontainer"))
                api(Deps.auth0_java_jwt)
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
