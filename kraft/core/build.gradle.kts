@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

plugins {
    kotlin("multiplatform")
    id("io.kotest.multiplatform") version Deps.Test.kotest_plugin_version
    id("org.jetbrains.kotlin.plugin.serialization") version Deps.kotlinVersion
    id("org.jetbrains.dokka") version Deps.dokkaVersion
    id("com.vanniktech.maven.publish") version Deps.mavenPublishVersion
}

val KRAFT_GROUP: String by project
val VERSION_NAME: String by project

group = KRAFT_GROUP
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
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    sourceSets {
        commonMain {
            dependencies {
                api(Deps.KotlinX.coroutines_core)
                api(project(":ultra:common"))
                api(project(":kraft:semanticui"))

                implementation(Deps.IDE.jetbrains_annotations)
            }
        }

        commonTest {
            dependencies {
                commonTestDeps()
            }
        }

        jsMain {
            dependencies {
                api(Deps.KotlinX.wrappers_extensions)
                api(Deps.KotlinX.serialization_json)

                // Preact VDOM engine
                api(Deps.Npm { preact() })
                // JWT decode
                api(Deps.Npm { jwtDecode() })
            }
        }

        jsTest {
            dependencies {
                jsTestDeps()
            }
        }

        jvmMain {
        }

        jvmTest {
            dependencies {
                jvmTestDeps()
            }
        }
    }
}

tasks {
    configureJvmTests()
}
