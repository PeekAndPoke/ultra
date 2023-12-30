@file:Suppress("PropertyName")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
}

tasks {
    withType<KotlinCompile>().all {
        compilerOptions {
            jvmTarget.set(Deps.jvmTarget)
        }
    }
}
