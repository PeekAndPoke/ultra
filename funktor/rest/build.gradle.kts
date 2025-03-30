@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.kotest.multiplatform") version Deps.Test.kotest_plugin_version
    id("com.vanniktech.maven.publish") version Deps.mavenPublishVersion
    idea
}

val FUNKTOR_GROUP: String by project
val VERSION_NAME: String by project

group = FUNKTOR_GROUP
version = VERSION_NAME

kotlin {
    js {
        browser {}
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":funktor:core"))
            }
        }

        commonTest {
            dependencies {
                Deps.Test { commonTestDeps() }
            }
        }

        jsMain {
            dependencies {
            }
        }

        jsTest {
            dependencies {
                Deps.Test { jsTestDeps() }
            }
        }

        jvmMain {
            dependencies {
                api(Deps.Ktor.Server.auth)
                api(Deps.Ktor.Server.auth_jwt)
                api(Deps.Ktor.Server.sse)
            }
        }

        jvmTest {
            dependencies {
                Deps.Test { jvmTestDeps() }

                implementation(Deps.JavaLibs.diffutils)
            }
        }
    }
}

tasks {
    configureJvmTests()
}
