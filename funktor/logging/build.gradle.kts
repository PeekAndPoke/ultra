@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version Deps.Ksp.version
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
                implementation(project(":kraft:core"))
                implementation(project(":kraft:semanticui"))

                api(project(":funktor:core"))
                api(project(":funktor:rest"))
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
            dependencies {
            }
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
                implementation(Deps.JavaLibs.logback_classic)

                implementation(project(":karango:core"))
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

dependencies {
    add("kspJvm", project(":karango:ksp"))
}

tasks {
    configureJvmTests()
}
