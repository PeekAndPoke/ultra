@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps
import Deps.Test.nativeTestDeps

plugins {
    kotlin("multiplatform")
    // For `Redacted<T>`, whose kotlinx serializer sits on the class itself — the runtime dependency
    // alone is not enough, `.serializer()` needs the compiler plugin.
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

    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    mingwX64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("reflect"))

                // For `Redacted<T>`, whose kotlinx serializer must sit on the class itself. See
                // `.claude/tasks/20260731-redacted-and-jackson-removal.md`.
                implementation(Deps.KotlinX.serialization_core)
            }
        }

        commonTest {
            dependencies {
                commonTestDeps()

                // Redacted<T>'s codec is only worth anything if it is exercised through a real format
                implementation(Deps.KotlinX.serialization_json)
            }
        }

        jsMain {
            dependencies {
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
