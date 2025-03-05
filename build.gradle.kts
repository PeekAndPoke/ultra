buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("multiplatform")

    id("org.jetbrains.kotlin.plugin.serialization") version Deps.kotlinVersion apply false
    id("io.kotest.multiplatform") version Deps.Test.kotest_plugin_version apply false
    id("org.jetbrains.dokka") version Deps.dokkaVersion apply false
    id("com.vanniktech.maven.publish") version Deps.mavenPublishVersion apply false
}

allprojects {
    repositories {
        mavenCentral()
        // KotlinX
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
        // Snapshots
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        // Local
        mavenLocal()
    }
}

kotlin {
    js {
        browser {}

        binaries.executable()
    }

    jvmToolchain(17)

    jvm {

    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(project(":common"))
            }
        }

        jvmMain {
            dependencies {
//                implementation(project(":mutator"))
            }
        }
    }
}
