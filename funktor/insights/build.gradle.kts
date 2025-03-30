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

    jvm {}

    sourceSets {
        commonMain {
            dependencies {
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
                implementation(kotlin("reflect"))

                api(Deps.Ktor.Server.html_builder)
                api(Deps.KotlinX.wrappers_css)

                api(project(":kraft:semanticui"))

                api(project(":funktor:core"))
                api(project(":funktor:cluster"))
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
