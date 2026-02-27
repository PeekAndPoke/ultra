@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    kotlin("kapt")
    id("com.google.devtools.ksp") version Deps.Ksp.version
    id("com.vanniktech.maven.publish")
}

val MUTATOR_GROUP: String by project
val VERSION_NAME: String by project

group = MUTATOR_GROUP
version = VERSION_NAME

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
            kotlin.srcDir("src/examples")

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

    // Add kapt dependency for JVM target
    add("kapt", Deps.JavaLibs.Google.auto_service)
    add("kaptTest", Deps.JavaLibs.Google.auto_service)
}

tasks {
    configureJvmTests {
        dependsOn("generateDocs")
    }

    create("generateDocs", JavaExec::class) {
        mainClass.set("de.peekandpoke.mutator.examples.GenerateDocsKt")
        classpath = sourceSets.getByName("jvmTest").runtimeClasspath
    }
}

mavenPublishing {
}
