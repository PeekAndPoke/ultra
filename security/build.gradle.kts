@file:Suppress("PropertyName")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.dokka")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

repositories {
    mavenCentral()
}

kotlin {
    js(LEGACY) {
        browser {
            webpackTask {
                output.libraryTarget = "commonjs2"
            }
        }
        binaries.executable()
    }

    jvm {
        val main by compilations.getting {
            kotlinOptions {
                // Setup the Kotlin compiler options for the 'main' compilation:
                jvmTarget = "1.8"
            }
        }
    }

    // Note that the Kotlin metadata is here, too.

    configure(listOf(targets["metadata"], jvm(), js())) {
        mavenPublication {
            val targetPublication = this@mavenPublication
            tasks.withType<AbstractPublishToMaven>()
                .matching { it.publication == targetPublication }
                .all { onlyIf { findProperty("isMainHost") == "true" } }
        }
    }

    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation(Deps.kotlinx_serialization_core)
                api(project(":commonmp"))
            }
        }

        val commonTest by getting {
            dependencies {
//                implementation("io.kotest:kotest-assertions-shared:$kotlintest_version")
//                implementation("io.kotest:kotest-framework-api:$kotlintest_version")
//                implementation("io.kotest:kotest-runner-junit5:$kotlintest_version")
            }
        }

        js().compilations["main"].defaultSourceSet {
            dependencies {
            }
        }

        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("reflect"))
                api(project(":kontainer"))
                api(Deps.auth0_java_jwt)
            }
        }

        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(Deps.Test.logback_classic)
                implementation(Deps.Test.kotest_assertions_core_jvm)
                implementation(Deps.Test.kotest_runner_junit_jvm)
            }
        }
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    getByName("jvmTest", Test::class) {
        useJUnitPlatform { }
    }
}

apply(from = "./../maven.publish.gradle.kts")
