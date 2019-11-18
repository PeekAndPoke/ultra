import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//buildscript {
//    repositories {
//        mavenCentral()
//        google()
//        gradlePluginPortal() // Required for the Errorprone Gradle Plugin.
//    }
//
//    dependencies {
//        classpath("com.vanniktech:gradle-code-quality-tools-plugin:0.18.0")
//    }
//}

plugins {
    idea
    kotlin("jvm") version "1.3.60"

    // Scans and Security: //////////////////////////////////////////////////////////////////
    `build-scan`
    // See: https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html
    id("org.owasp.dependencycheck") version "5.2.4"
    // See: https://github.com/CycloneDX/cyclonedx-gradle-plugin
    id("org.cyclonedx.bom")
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    api(kotlin("stdlib-jdk8"))

    // add all child projects
    compile(project(":ultra"))
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
