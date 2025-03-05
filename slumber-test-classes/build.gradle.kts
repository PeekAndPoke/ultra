@file:Suppress("PropertyName")

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

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)
}

tasks {
}
