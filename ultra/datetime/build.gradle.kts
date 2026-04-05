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

val ULTRA_GROUP: String by project
val VERSION_NAME: String by project

group = ULTRA_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

kotlin {
    js {
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
    macosX64()
    macosArm64()
    mingwX64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(Deps.KotlinX.serialization_core)
                implementation(Deps.KotlinX.serialization_json)

                api(Deps.KotlinX.datetime)
            }
        }

        commonTest {
            dependencies {
                implementation(project(":ultra:common"))
                implementation(Deps.KotlinX.coroutines_test)
                commonTestDeps()
            }
        }

        jsMain {
            dependencies {
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
