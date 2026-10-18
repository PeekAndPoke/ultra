@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.kotest")
    id("com.google.devtools.ksp")
    id("com.vanniktech.maven.publish")
}

val ULTRA_GROUP: String by project
val VERSION_NAME: String by project

group = ULTRA_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

kotlin {
    js {
        compilerOptions {
            target.set("es2015")
        }

        browser()
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(Deps.KotlinX.serialization_core)
                // `api`, not `implementation`: JwtPayload.claims is a public JsonObject and
                // JwtClaim.value a public JsonElement, so these types ARE this module's published
                // surface. At implementation scope the POM marks them runtime-only and an external
                // consumer writing `caller.payload.claims["x"]` fails to compile. Not caught in-repo,
                // because funktor:rest api-exposes kotlinx-json transitively.
                api(Deps.KotlinX.serialization_json)

                api(project(":ultra:common"))
                implementation(project(":ultra:slumber"))
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
                implementation(kotlin("reflect"))
                implementation(project(":ultra:kontainer"))
                api(Deps.JavaLibs.password4j)
            }
        }

        jvmTest {
            dependencies {
                jvmTestDeps()
            }
        }
    }
}

tasks {
    configureJvmTests()
}

mavenPublishing {
}
