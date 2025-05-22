@file:Suppress("PropertyName")

import Deps.Test.commonTestDeps
import Deps.Test.configureJvmTests
import Deps.Test.jsTestDeps
import Deps.Test.jvmTestDeps

plugins {
    kotlin("multiplatform")
    id("io.kotest.multiplatform")
    id("com.vanniktech.maven.publish")
}

val KRAFT_GROUP: String by project
val VERSION_NAME: String by project

group = KRAFT_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

kotlin {
    js {
        browser {
//            testTask {
//                useKarma {
//                    useChromeHeadless()
//                    usePhantomJS()
//                    useChromiumHeadless()
//                    useFirefoxHeadless()
//                }
//            }
        }
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {

        commonMain {
            dependencies {
                api(Deps.KotlinX.wrappers_css)
                api(Deps.KotlinX.html)
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
