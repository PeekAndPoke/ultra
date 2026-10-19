@file:Suppress("PropertyName")

plugins {
    kotlin("jvm")
}

val ULTRA_GROUP = providers.gradleProperty("ULTRA_GROUP").get()
val VERSION_NAME = providers.gradleProperty("VERSION_NAME").get()

group = ULTRA_GROUP
version = VERSION_NAME

dependencies {
    implementation(kotlin("reflect"))
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)
}

tasks {
}
