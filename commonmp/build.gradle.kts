@file:Suppress("PropertyName")

import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val logback_version: String by project
val classindex_version: String by project
val kotlintest_version: String by project
val kotlinx_serialization_version: String by project
val kotlinx_coroutines_version: String by project
val klock_version: String by project

plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.dokka")
    id("com.jfrog.bintray")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

repositories {
    mavenCentral()
    jcenter()
}

kotlin {
    js {
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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinx_serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version")
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
                api("com.soywiz.korlibs.klock:klock-js:$klock_version")
            }
        }

        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation("com.soywiz.korlibs.klock:klock-jvm:$klock_version")
                implementation("org.atteo.classindex:classindex:$classindex_version")
            }
        }

        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                // Testing
                implementation("ch.qos.logback:logback-classic:$logback_version")
                implementation("io.kotest:kotest-assertions-core-jvm:$kotlintest_version")
                implementation("io.kotest:kotest-runner-junit5-jvm:$kotlintest_version")
            }
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    dokka {
        outputFormat = "html"
        outputDirectory = "$buildDir/javadoc"
    }

    getByName("jvmTest", Test::class) {
        useJUnitPlatform { }
    }

    getByName("build") {
        dependsOn("jvmTest")
    }
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Publishing see:
// - https://medium.com/xorum-io/crafting-and-publishing-kotlin-multiplatform-library-to-bintray-cbc00a4f770
// - https://medium.com/@satis.vishnu/kotlin-multiplatform-library-101-a34e906f58c
//
// ////////////////////////////////////////////////////////////////////////////////////////////////////////

fun stringProperty(s: String) = project.findProperty(s) as String?

afterEvaluate {

    project.publishing.publications.filterIsInstance<MavenPublication>().forEach {

        it.groupId = group as String

        if (it.name.contains("metadata")) {
            it.artifactId = project.name
        } else {
            it.artifactId = "${project.name}-${it.name}"
        }
    }
}

bintray {
    user = stringProperty("bintrayUser")
    key = stringProperty("bintrayApiKey")
    publish = true
    override = true

    setPublications(
        *publishing.publications.map { it.name }.toTypedArray()
    )

    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "de.peekandpoke.ultra.${project.name}"
        userOrg = "peekandpoke"
        description = "Common multi-platform helpers und utils"
        setLabels("kotlin")
        setLicenses("MIT")
        desc = description
    })
}
