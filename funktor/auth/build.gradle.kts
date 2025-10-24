@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version Deps.Ksp.version
    id("io.kotest")
    id("com.vanniktech.maven.publish")
}

val FUNKTOR_GROUP: String by project
val VERSION_NAME: String by project

group = FUNKTOR_GROUP
version = VERSION_NAME

kotlin {
    js {
        browser {
        }
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(Deps.KotlinX.serialization_core)
                implementation(Deps.KotlinX.serialization_json)

                implementation(Deps.KotlinLibs.uuid)

                implementation(project(":ultra:common"))
                implementation(project(":ultra:security"))
                implementation(project(":ultra:slumber"))

                implementation(project(":karango:addons"))

                implementation(project(":funktor:messaging"))
                implementation(project(":funktor:rest"))
            }
        }

        commonTest {
            dependencies {
                Deps.Test { commonTestDeps() }
            }
        }

        jsMain {
            dependencies {
                implementation(project(":kraft:semanticui"))
                implementation(project(":kraft:addons:jwtdecode"))
            }
        }

        jsTest {
            dependencies {
                Deps.Test { jsTestDeps() }
            }
        }

        jvmMain {
            dependencies {
                implementation(Deps.Ktor.Client.core)
                implementation(Deps.Ktor.Client.content_negotiation)
                implementation(Deps.Ktor.Common.serialization_kotlinx_json)

                implementation(Deps.JavaLibs.Google.api_client)

                implementation(project(":karango:core"))
                implementation(project(":funktor:core"))
            }
        }

        jvmTest {
            dependencies {
                Deps.Test { jvmTestDeps() }
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
