@file:Suppress("PropertyName")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

val logback_version: String by project
val kotlintest_version: String by project

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    api(kotlin("reflect"))

    implementation(project(":commonmp"))

    testImplementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotlintest_version")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotlintest_version")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

apply(from = "./../maven.publish.gradle.kts")
