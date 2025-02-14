@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization") version Deps.kotlinVersion
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
            testTask {
//                useKarma {
//                    useChrome()
//                    useChromeHeadless()
//                    useChromiumHeadless()
//                    useChromeCanaryHeadless()
//                    useFirefoxHeadless()
//                }
            }
        }

        binaries.executable()
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
                api(Deps.kotlinx_coroutines_core)
                api(Deps.kotlinx_serialization_core)
                api(Deps.kotlinx_serialization_json)
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
