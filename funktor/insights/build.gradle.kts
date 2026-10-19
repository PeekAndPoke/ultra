@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests


plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.kotest")
    id("com.google.devtools.ksp")
    id("com.vanniktech.maven.publish")
}

val FUNKTOR_GROUP = providers.gradleProperty("FUNKTOR_GROUP").get()
val VERSION_NAME = providers.gradleProperty("VERSION_NAME").get()

group = FUNKTOR_GROUP
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



                api(project(":funktor:core"))
                api(project(":funktor:cluster"))
                api(project(":funktor:rest"))
            }
        }

        jvmTest {
            dependencies {
                // Drives the collectors through a real ApplicationCall — the redaction call sites are
                // otherwise untested, and a policy that is never invoked redacts nothing.
                implementation(Deps.Ktor.Server.Test.host)

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
