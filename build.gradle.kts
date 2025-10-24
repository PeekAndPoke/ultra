import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version Deps.kotlinVersion apply false
    id("io.kotest") version Deps.Test.kotest_plugin_version apply false
    id("com.google.devtools.ksp") version Deps.Ksp.version apply false
    id("org.jetbrains.dokka") version Deps.dokkaVersion apply false
    id("com.vanniktech.maven.publish") version Deps.mavenPublishVersion apply false
    idea
}

allprojects {
    repositories {
        mavenCentral()
        // KotlinX
        // maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
        // Snapshots
        // maven("https://oss.sonatype.org/content/repositories/snapshots")
        // Local
        // mavenLocal()
    }
}

// Apply to every subproject (skips the root automatically)
subprojects {
    // Derive a stable, unique archive name from the Gradle path:
    // :sub          -> sub
    // :kraft:core   -> kraft-core
    // :funktor:core -> funktor-core
    // :a:b:c        -> a-b-c
    apply<BasePlugin>()

    val nameFromPath = path.removePrefix(":").replace(":", "-")

    extensions.configure<BasePluginExtension> {
        archivesName.convention(nameFromPath)
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        archiveBaseName.convention(nameFromPath)
    }
}

rootProject.plugins.withType<YarnPlugin> {
    rootProject.the<YarnRootExtension>().apply {
        // Don't fail the build when yarn.lock changes (optional but common)
        yarnLockMismatchReport = YarnLockMismatchReport.WARNING
        // Always just regenerate/replace yarn.lock
        yarnLockAutoReplace = true
        // Optional: if a new lock didn't exist before, don't spam about it
        reportNewYarnLock = false
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
                implementation(Deps.KotlinX.coroutines_core)
                implementation(project(":ultra:common"))
                implementation(project(":mutator:core"))
            }
        }

        jvmMain {
            dependencies {
//                implementation(project(":mutator"))
                implementation(project(":funktor:core"))
            }
        }
    }
}
