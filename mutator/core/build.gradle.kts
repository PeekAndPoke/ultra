@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    kotlin("kapt")
    // Kotest's test discovery on JS
    id("io.kotest")
    id("com.google.devtools.ksp") version Deps.Ksp.version
    id("com.vanniktech.maven.publish")
}

val MUTATOR_GROUP = providers.gradleProperty("MUTATOR_GROUP").get()
val VERSION_NAME = providers.gradleProperty("VERSION_NAME").get()

group = MUTATOR_GROUP
version = VERSION_NAME

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

kotlin {

    js {
        compilerOptions {
            target.set("es2015")
        }

        browser {
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
                Deps.Test {
                    commonTestDeps()
                }
            }
        }

        // The specs live in src/sharedTest, compiled once per platform: they call the mutators KSP generates for
        // the commonTest domain classes, which commonTest cannot see under K2 (KSP has no commonTest target).
        jsTest {
            kotlin.srcDir("src/sharedTest/kotlin")
            // The mutator processor does not run in kspJsTest: next to Kotest's JS spec discovery its generated
            // files force a second KSP round, and Kotest 6.2.6's processor then fails in finish() ("PSI has
            // changed since creation"). The JS tests compile the JVM run's domain mutators instead: the processor
            // has no platform branch, and both outputs were byte-identical on 2026-11-13. JS gets the mutators of
            // the package io.peekandpoke.mutator.domain only, so a @Mutable test fixture lives in commonTest, in that
            // package, as common code; anywhere else the JS compile fails with an unresolved reference.
            // Until Kotest is fixed, no JS compilation runs the mutator processor (ULTRA-00144).
            kotlin.srcDir(
                files(layout.buildDirectory.dir("generated/ksp/jvm/jvmTest/kotlin/io/peekandpoke/mutator/domain"))
                    .builtBy("kspTestKotlinJvm")
            )

            dependencies {
                Deps.Test {
                    jsTestDeps()
                }
            }
        }

        jvmTest {
            kotlin.srcDir("src/sharedTest/kotlin")
            kotlin.srcDir("src/examples")

            dependencies {
                implementation(project(":tooling"))
                Deps.Test {
                    jvmTestDeps()
                }
            }
        }
    }
}

dependencies {
    kspCommonMainMetadata(project(":mutator:ksp"))

    add("kspJvmTest", project(":mutator:ksp"))

    // Add kapt dependency for JVM target
    add("kapt", Deps.JavaLibs.Google.auto_service)
    add("kaptTest", Deps.JavaLibs.Google.auto_service)
}

tasks {
    configureJvmTests {
        dependsOn("generateDocs")
    }

    register<JavaExec>("generateDocs") {
        mainClass.set("io.peekandpoke.mutator.examples.GenerateDocsKt")
        classpath = sourceSets.getByName("jvmTest").runtimeClasspath
        // The examples read KSP output from the build directory, which differs per user (MC-00031)
        systemProperty("mutator.buildDir", layout.buildDirectory.get().asFile.absolutePath)
    }
}

mavenPublishing {
}
