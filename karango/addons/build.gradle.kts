@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version Deps.Ksp.version
    id("com.vanniktech.maven.publish")
    idea
}

val KARANGO_GROUP: String by project
val VERSION_NAME: String by project

group = KARANGO_GROUP
version = VERSION_NAME

kotlin {
    js {
        browser {}

    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
        withJava()
    }

    sourceSets {

        commonMain {
            dependencies {
                implementation(Deps.KotlinX.serialization_core)

                implementation(project(":common"))
            }
        }

        commonTest {
            dependencies {
                Deps.Test {
                    commonTestDeps()
                }
            }
        }

        jsMain {

        }

        jsTest {
            dependencies {
                Deps.Test {
                    jsTestDeps()
                }
            }
        }

        jvmMain {
            dependencies {
                api(project(":vault"))
                api(project(":karango:core"))

                configurations.getByName("kspJvm").dependencies.add(project(":karango:ksp"))
            }
        }

        jvmTest {
            dependencies {
                Deps.Test {
                    jvmTestDeps()
                }
            }
        }
    }
}

tasks {
    configureJvmTests()
}
