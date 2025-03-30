@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
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
        withJava()
    }

    sourceSets {

        commonMain {
            dependencies {
                // add all ktorfx sub-projects
                api(project(":funktor:core"))
                api(project(":funktor:cluster"))
                api(project(":funktor:logging"))
                api(project(":funktor:insights"))
                api(project(":funktor:rest"))
                api(project(":funktor:messaging"))
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
                api(project(":funktor:staticweb"))
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
