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
        compilerOptions {
            target.set("es2015")
        }

        browser {
            testTask {
            }
        }
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    sourceSets {

        commonMain {
            dependencies {
                // `api` (not `implementation`): kotlinx.html is in this module's PUBLISHED API —
                // EmailLayout.render takes a `BODY.() -> Unit` and EmailBody.Html.invoke takes an
                // `HTML.() -> Unit`. An app writing its own layout must see those receivers.
                api(Deps.KotlinX.html)

                api(project(":funktor:core"))
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
                // Builds the MIME message AwsSesSender hands to SendRawEmail. NOT an SMTP transport:
                // mail always goes out through a provider API. This used to arrive transitively via
                // commons-email, which was declared "for smtp" and whose own API nothing ever used.
                implementation(Deps.JavaLibs.JakartaMail.mail)
                // AWS SES https://aws.amazon.com/sdk-for-java/
                implementation(Deps.JavaLibs.Aws.ses)
                // Sendgrid https://github.com/sendgrid/sendgrid-java
                implementation(Deps.JavaLibs.Sendgrid.sendgrid_java)

                // `api` (not `implementation`): Locale is in this module's PUBLISHED API —
                // EmailTemplate.render and EmailLayout.render both name it, so consumers
                // (funktor:auth and any app supplying its own templates) must see the type.
                api(project(":ultra:i18n"))

                // For senders that use apis directly, like SendGrid. No ContentNegotiation/Jackson:
                // SendgridSender posts the JSON that sendgrid-java's own Mail.build() produces.
                implementation(Deps.Ktor.Client.core)
                implementation(Deps.Ktor.Client.cio)

                implementation(project(":karango:core"))
                implementation(project(":monko:core"))
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
    add("kspJvmTest", project(":karango:ksp"))
    add("kspJvm", project(":monko:ksp"))
    add("kspJvmTest", project(":monko:ksp"))
}

tasks {
    configureJvmTests()
}

mavenPublishing {
}
