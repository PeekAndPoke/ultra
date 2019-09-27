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
    `build-scan`
    kotlin("jvm") version "1.3.50"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    api(kotlin("stdlib-jdk8"))

    // add all child projects
    compile(project(":common"))
    compile(project(":meta"))
    compile(project(":kontainer"))
    compile(project(":mutator"))
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
