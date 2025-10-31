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
                implementation(Deps.KotlinX.html)

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
                // sending simple email via smtp using
                implementation(Deps.JavaLibs.ApacheCommons.email)
                // AWS SES https://aws.amazon.com/sdk-for-java/
                implementation(Deps.JavaLibs.Aws.ses)
                // Sendgrid https://github.com/sendgrid/sendgrid-java
                implementation(Deps.JavaLibs.Sendgrid.sendgrid_java)

                // For senders that use apis directly, like SendGrid
                implementation(Deps.Ktor.Client.core)
                implementation(Deps.Ktor.Client.cio)
                implementation(Deps.Ktor.Client.content_negotiation)
                implementation(Deps.Ktor.Common.serialization_jackson)

                implementation(project(":karango:core"))
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

mavenPublishing {
}
