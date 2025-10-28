@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests
import Deps.createVersionFile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version Deps.kotlinVersion
    id("com.google.devtools.ksp") version Deps.Ksp.version
    idea
    application
}

val FUNKTOR_DEMO_GROUP: String by project
val VERSION_NAME: String by project

group = FUNKTOR_DEMO_GROUP
version = VERSION_NAME

createVersionFile()

allprojects {
    repositories {
        mavenCentral()
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

application {
    mainClass.set("io.peekandpoke.funktor.demo.server.MainKt")
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)

    dependencies {
        Deps.Ktor.Server.full(this)

        implementation(project(":funktor:all"))

        implementation(project(":karango:core"))
        implementation(project(":karango:addons"))
        ksp(project(":karango:ksp"))

        implementation(project(":monko:core"))

        implementation(project(":funktor-demo:common"))
    }
}

tasks {
    configureJvmTests()
}
