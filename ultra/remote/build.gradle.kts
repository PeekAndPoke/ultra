@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

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

    sourceSets {
        commonMain {
            dependencies {
                api(project(":ultra:common"))
                api(project(":ultra:model"))

                implementation(Deps.KotlinX.serialization_core)
                implementation(Deps.KotlinX.serialization_json)

                implementation(Deps.Ktor.Client.core)
            }
        }

        jsMain {
            dependencies {
                // Provides the Ktor Js engine so `HttpClient {}` resolves on the browser.
                implementation(Deps.Ktor.Client.js)
            }
        }

        jvmMain {
            dependencies {
                // Provides a default Ktor engine so `HttpClient {}` resolves on the JVM.
                implementation(Deps.Ktor.Client.cio)
            }
        }

        commonTest {
            dependencies {
                commonTestDeps()
                implementation(Deps.Ktor.Client.mock)
            }
        }

        jsTest {
            dependencies {
                jsTestDeps()
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

mavenPublishing {
}
