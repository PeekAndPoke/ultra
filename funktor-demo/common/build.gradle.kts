@file:Suppress("PropertyName")

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val GROUP = "io.peekandpoke.funktor_demo"
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

kotlin {
    js {
        browser {
        }
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {}

    sourceSets {
        commonMain {
            dependencies {
                implementation(Deps.KotlinX.serialization_core)
            }
        }
    }
}
