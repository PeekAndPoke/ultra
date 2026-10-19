@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps
import Deps.Test.nativeTestDeps

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.kotest")
    id("com.google.devtools.ksp")
    id("com.vanniktech.maven.publish")
}

val ULTRA_GROUP = providers.gradleProperty("ULTRA_GROUP").get()
val VERSION_NAME = providers.gradleProperty("VERSION_NAME").get()

group = ULTRA_GROUP
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

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    linuxX64()
    linuxArm64()
    macosArm64()
    mingwX64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(Deps.KotlinX.serialization_core)
                implementation(Deps.KotlinX.serialization_json)

                api(Deps.KotlinX.datetime)

                // api, not implementation: the public Mp* types implement common's ComparableTo
                api(project(":ultra:common"))
            }
        }

        commonTest {
            dependencies {
                implementation(Deps.KotlinX.coroutines_test)
                commonTestDeps()
            }
        }

        jsMain {
            dependencies {
            }
        }

        jsTest {
            dependencies {
                jsTestDeps()
                // No @js-joda/timezone and no zone provider: ultra's zone math runs on Intl
                // (TimezoneEngineJs.kt), so a green suite proves no path needs kotlinx zone data.
            }
        }

        jvmMain {
            dependencies {
            }
        }

        jvmTest {
            dependencies {
                jvmTestDeps()
            }
        }

        nativeMain {
            dependencies {
            }
        }

        nativeTest {
            dependencies {
                nativeTestDeps()
            }
        }
    }
}

tasks {
    getByName("compileKotlinJs").dependsOn(":compileCommonMainKotlinMetadata")

    configureJvmTests()
}

mavenPublishing {
}
