import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        google()
//        gradlePluginPortal() // Required for the Errorprone Gradle Plugin.
    }

    dependencies {
//        classpath("com.vanniktech:gradle-code-quality-tools-plugin:0.18.0")
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
    }
}

plugins {
    idea
    kotlin("jvm") version "1.4.10"
//    kotlin("js") version "1.4.10"
//    kotlin("multiplatform") version "1.4.10"
    kotlin("kapt") version "1.4.10"

    // Scans and Security: //////////////////////////////////////////////////////////////////
    // `build-scan`
    // See: https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html
//    id("org.owasp.dependencycheck") version "5.3.2"
    // See: https://github.com/CycloneDX/cyclonedx-gradle-plugin
//    id("org.cyclonedx.bom")

    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("org.jetbrains.dokka") apply false
    id("com.jfrog.bintray") apply false
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        // Repo for KlassIndex (https://github.com/matfax/klassindex)
        maven("https://jitpack.io")
    }
}

dependencies {
    // add all child projects
    api(project(":ultra"))
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

//codeQualityTools {
//    failEarly = true
//    xmlReports = true
//    htmlReports = true
//    textReports = true
//
//    detekt {
//
//    }
//}
