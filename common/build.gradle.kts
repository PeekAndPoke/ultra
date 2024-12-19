@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
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

    implementation(project(":commonmp"))

    Deps.Test {
        jvmTestDeps()
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(Deps.jvmTarget)
    }
}

tasks {
    configureJvmTests()
}
