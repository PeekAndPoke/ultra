@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests
import Deps.Test.jvmTestDeps

plugins {
    kotlin("jvm")
}

val ULTRA_GROUP: String by project
val VERSION_NAME: String by project

group = ULTRA_GROUP
version = VERSION_NAME

dependencies {
    implementation(project(":ultra:common"))
    implementation(Deps.JavaLibs.Yaml.snakeyaml)

    jvmTestDeps()
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)
}

tasks {
    configureJvmTests()
}
