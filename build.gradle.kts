import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        google()
    }

    dependencies {
//        classpath("com.vanniktech:gradle-code-quality-tools-plugin:0.18.0")
    }
}

plugins {
    idea
    kotlin("jvm") version Deps.kotlinVersion
    kotlin("kapt") version Deps.kotlinVersion

    id("org.jetbrains.kotlin.plugin.serialization") version Deps.kotlinVersion apply false
    id("org.jetbrains.dokka") version Deps.dokkaVersion apply false
    id("com.vanniktech.maven.publish") version Deps.mavenPublishVersion apply false
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

/*
codeQualityTools {
    failEarly = true
    xmlReports = true
    htmlReports = true
    textReports = true

    detekt {

    }
}
*/
