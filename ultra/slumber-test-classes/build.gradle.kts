@file:Suppress("PropertyName")

plugins {
    kotlin("jvm")
}

val ULTRA_GROUP: String by project
val VERSION_NAME: String by project

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
