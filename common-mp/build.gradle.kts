@file:Suppress("PropertyName")

import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("multiplatform")
    id("org.jetbrains.dokka") version "0.9.18"
    id("com.jfrog.bintray") version "1.8.4"
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

val logback_version: String by project
val kotlintest_version: String by project
val kotlinx_serialization_version: String by project
val klock_version: String by project

repositories {
    mavenCentral()
    jcenter()
}

kotlin {
    js()
    jvm {
        val main by compilations.getting {
            kotlinOptions {
                // Setup the Kotlin compiler options for the 'main' compilation:
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$kotlinx_serialization_version")
                api("com.soywiz.korlibs.klock:klock:$klock_version")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        js().compilations["main"].defaultSourceSet {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$kotlinx_serialization_version")
                api("com.soywiz.korlibs.klock:klock-js:$klock_version")
            }
        }

        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinx_serialization_version")
                api("com.soywiz.korlibs.klock:klock-jvm:$klock_version")
            }
        }
    }
}


//dependencies {
//    api(kotlin("stdlib-jdk8"))
//    api(kotlin("reflect"))
//
//    testImplementation("ch.qos.logback:logback-classic:$logback_version")
//    testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlintest_version")
//}

//val test by tasks.getting(Test::class) {
//    useJUnitPlatform { }
//}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    dokka {
        outputFormat = "html"
        outputDirectory = "$buildDir/javadoc"
    }
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
}


afterEvaluate {

    project.publishing.publications.filterIsInstance<MavenPublication>().forEach {

        it.groupId = group as String

        if (it.name.contains("kotlinMultiplatform")) {
            it.artifactId = project.name
        } else {
            it.artifactId = "${project.name}-${it.name}"
        }
    }

//    project.publishing.publications.filterIsInstance<MavenPublication>().forEach {
//        println(it)
//        println(it.name)
//        println(it.artifactId)
//    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Publishing see:
// - https://medium.com/xorum-io/crafting-and-publishing-kotlin-multiplatform-library-to-bintray-cbc00a4f770
// - https://medium.com/@satis.vishnu/kotlin-multiplatform-library-101-a34e906f58c
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////

//publishing {
//    publications {
//        create<MavenPublication>("default") {
//
//
////            print(components.asMap)
//
////            from(components["kotlin"])
////            from(components["js"])
////            artifact(dokkaJar)
////            artifact(sourcesJar)
//        }
//    }
//}

fun stringProperty(s: String) = project.findProperty(s) as String?

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
