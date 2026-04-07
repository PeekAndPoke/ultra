@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps
import Deps.Test.nativeTestDeps

plugins {
    kotlin("multiplatform")
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
                api(project(":ultra:common"))
                api(project(":ultra:datetime"))

                implementation(Deps.KotlinX.coroutines_core)
            }
        }

        commonTest {
            dependencies {
                implementation(Deps.KotlinX.coroutines_test)
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
