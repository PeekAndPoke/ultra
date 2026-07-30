@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

// Permanent integration test for the I18nPlugin: yaml -> generateI18n -> srcDir -> compile -> runtime.
// Not published.

plugins {
    kotlin("multiplatform")
    id("io.kotest")
    id("com.google.devtools.ksp")
}

val ULTRA_GROUP: String by project
val VERSION_NAME: String by project

group = ULTRA_GROUP
version = VERSION_NAME

apply<I18nPlugin>()

configure<I18nExtension> {
    packageName.set("io.peekandpoke.ultra.tooling.i18nfixture")
    moduleName.set("Fixture")
    fallbackLang.set("en")
    requiredLangs("de")
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

    @Suppress("UNUSED_VARIABLE")
    sourceSets {

        commonMain {
            dependencies {
                api(project(":ultra:i18n"))
            }
        }

        commonTest {
            dependencies {
                commonTestDeps()
            }
        }

        jsTest {
            dependencies {
                jsTestDeps()
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
