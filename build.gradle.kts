import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    idea
    kotlin("jvm")
    kotlin("kapt")

    id("org.jetbrains.kotlin.plugin.serialization") version Deps.kotlinVersion apply false
    id("org.jetbrains.dokka") version Deps.dokkaVersion apply false
    id("com.vanniktech.maven.publish") version Deps.mavenPublishVersion apply false
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()

        // KotlinX
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")

        // Repo for KlassIndex (https://github.com/matfax/klassindex)
        maven("https://jitpack.io")

        // Snapshots
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    // add all child projects
    api(project(":common"))
    api(project(":commonmp"))
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
