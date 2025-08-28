@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp") version Deps.Ksp.version
    id("com.vanniktech.maven.publish")
}

val MUTATOR_GROUP: String by project
val VERSION_NAME: String by project

group = MUTATOR_GROUP
version = VERSION_NAME

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
                implementation(kotlin("reflect"))
                api(project(":ultra:common"))
            }
        }

        commonTest {
            dependencies {
                Deps.Test { commonTestDeps() }
            }
        }

        jsTest {
            dependencies {
                Deps.Test { jsTestDeps() }
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
    kspCommonMainMetadata(project(":mutator:ksp"))
    add("kspJvmTest", project(":mutator:ksp"))
    add("kspJsTest", project(":mutator:ksp"))
}

tasks {
    configureJvmTests()
}

mavenPublishing {

}
