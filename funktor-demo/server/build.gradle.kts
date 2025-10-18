@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version Deps.kotlinVersion
    id("com.google.devtools.ksp") version Deps.Ksp.version
    idea
}

val FUNKTOR_DEMO_GROUP: String by project
val VERSION_NAME: String by project

group = FUNKTOR_DEMO_GROUP
version = VERSION_NAME

allprojects {
    repositories {
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        mavenLocal()
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

kotlin {
    js {
        browser {}
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
        @Suppress("OPT_IN_USAGE")
        binaries {
            executable {
                mainClass.set("io.peekandpoke.funktor.demo.server.MainKt")
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":funktor:all"))
            }
        }

        commonTest {
            dependencies {
                commonTestDeps()
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
                Deps.Ktor.Server.full(this)

                implementation(project(":karango:core"))
                implementation(project(":karango:addons"))
                configurations.getByName("kspJvm").dependencies.add(project(":karango:ksp"))

                implementation(Deps.JavaLibs.logback_classic)
            }
        }

        jvmTest {
            dependencies {
                Deps.Ktor.Server.fullTest(this)

                jvmTestDeps()
            }
        }
    }
}

tasks {
    configureJvmTests()
}
