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
                implementation(Deps.KotlinX.serialization_core)
                implementation(Deps.KotlinX.serialization_json)

                implementation(Deps.KotlinLibs.uuid)

                implementation(project(":ultra:common"))
                // `api` (not `implementation`): UserId appears in this module's PUBLISHED API —
                // AuthSetPasswordRequest.userId, AuthState.Data.Session.tokenUserId, AuthRecord.ownerId,
                // AuthRecordStorage/SessionStore/AuthRealm.refreshToken — so consumers must see the type.
                api(project(":ultra:security"))
                implementation(project(":ultra:slumber"))

                implementation(project(":kraft:core"))

                implementation(project(":funktor:messaging"))
                implementation(project(":funktor:rest"))
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
                implementation(project(":kraft:semanticui"))
                // NOTE: `:kraft:addons:jwtdecode` was dropped here. Claims are decoded by
                // `jwtClaims.kt`, which is synchronous; the addon loads `jwt-decode` through a
                // dynamic import behind an AddonRegistry and so cannot be used from the synchronous
                // `AuthState.readJwt`. Keeping the dependency shipped that npm package into every
                // downstream JS bundle for nothing.
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
                implementation(Deps.Ktor.Client.core)
                implementation(Deps.Ktor.Client.content_negotiation)
                implementation(Deps.Ktor.Common.serialization_kotlinx_json)

                implementation(Deps.KotlinX.html)

                implementation(Deps.JavaLibs.Google.api_client)

                implementation(project(":funktor:core"))
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
