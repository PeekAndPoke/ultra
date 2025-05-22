@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.kotest.multiplatform")
    id("com.vanniktech.maven.publish")
}

val FUNKTOR_GROUP: String by project
val VERSION_NAME: String by project

group = FUNKTOR_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

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
                api(kotlin("reflect"))
                api(Deps.KotlinX.coroutines_core)
                api(Deps.KotlinX.serialization_core)
                api(Deps.KotlinX.serialization_json)

                api(Deps.KotlinLibs.clikt)
                api(Deps.KotlinLibs.uuid)

                api(project(":ultra:common"))
                api(project(":ultra:logging"))
                api(project(":ultra:security"))
                api(project(":ultra:slumber"))
                api(project(":ultra:vault"))
            }
        }

        commonTest {
            dependencies {
                Deps.Test {
                    commonTestDeps()
                }
            }
        }

        jsMain {}

        jvmMain {
            dependencies {
                // include ktor server side components
                api(Deps.Ktor.Server.core)
                api(Deps.Ktor.Server.sessions)
                api(Deps.Ktor.Server.netty)

                // Jackson used for serializing and pretty printing
                api(Deps.JavaLibs.Jackson.annotations)
                api(Deps.JavaLibs.Jackson.datatype_jdk8)
                api(Deps.JavaLibs.Jackson.datatype_jsr310)
                api(Deps.JavaLibs.Jackson.module_kotlin)

                // ultra
                api(project(":ultra:kontainer"))
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

mavenPublishing {

}
