@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

plugins {
    base
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.kotest")
    id("com.google.devtools.ksp")
    id("com.vanniktech.maven.publish")
}

val KRAFT_GROUP: String by project
val VERSION_NAME: String by project

group = KRAFT_GROUP
version = VERSION_NAME

// This affects only the **filename**, not Maven coordinates
base {
    archivesName.set("kraft-core")
}

Docs {
    useEmptyJavadoc()
}

kotlin {
    js {
        browser {
            testTask {
//                useKarma {
//                    useChrome()
//                    useChromeHeadless()
//                    useChromiumHeadless()
//                    useChromeCanaryHeadless()
//                    useFirefoxHeadless()
//                }
            }
        }
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    sourceSets {
        commonMain {
            dependencies {
                api(Deps.KotlinX.coroutines_core)
                implementation(Deps.IDE.jetbrains_annotations)
                api(Deps.KotlinX.html)

                api(project(":ultra:common"))
                api(project(":ultra:html"))
                api(project(":ultra:streams"))
            }
        }

        commonTest {
            dependencies {
                commonTestDeps()
            }
        }

        jsMain {
            dependencies {
                api(Deps.KotlinX.wrappers_extensions)
                api(Deps.KotlinX.serialization_json)

                // Preact VDOM engine
                api(Deps.Npm { preact() })
                // JWT decode
                api(Deps.Npm { jwtDecode() })
            }
        }

        jsTest {
            dependencies {
                jsTestDeps()
            }
        }

        jvmMain {
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
