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

        // Snapshots
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
}

tasks {
    withType<KotlinCompile>().all {
        compilerOptions {
            jvmTarget.set(Deps.jvmTarget)
        }
    }
}
