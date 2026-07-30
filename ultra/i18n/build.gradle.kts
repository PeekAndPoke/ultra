@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

plugins {
    kotlin("multiplatform")
    id("io.kotest")
    // ksp is required by kotest 6 to generate the test runtime (kspTestKotlin*), not a code generator here.
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

    @Suppress("UNUSED_VARIABLE")
    sourceSets {

        commonMain {
            dependencies {
                // No deps yet — substitution is self-contained (single-pass regex). ultra:common
                // returns when locale formatting (D9) needs it.
            }
        }

        commonTest {
            dependencies {
                commonTestDeps()
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
    configureJvmTests()
}

mavenPublishing {
}
