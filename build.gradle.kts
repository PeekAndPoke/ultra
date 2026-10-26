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
    kotlin("plugin.serialization") version providers.gradleProperty("kotlinVersion").get() apply false
    id("io.kotest") version Deps.Test.kotest_plugin_version apply false
    id("com.google.devtools.ksp") version Deps.Ksp.version apply false
    id("org.jetbrains.dokka") version Deps.dokkaVersion apply false
    id("com.vanniktech.maven.publish") version Deps.mavenPublishVersion apply false
    idea
}

allprojects {
    apply(plugin = "idea")

    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

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

// The per-user build outputs (MC-00031) are a second copy of every build: keep IntelliJ from indexing them
idea {
    module {
        excludeDirs.add(file(".build-claude"))
        excludeDirs.add(file(".gradle-claude"))
        excludeDirs.add(file("buildSrc/.build-claude"))
    }
}

// Apply to every subproject (skips the root automatically)
subprojects {
    // Derive a stable, unique archive name from the Gradle path and prefix it:
    // :sub          -> ultra-sub
    // :kraft:core   -> ultra-kraft-core
    // :funktor:core -> ultra-funktor-core
    // :a:b:c        -> ultra-a-b-c
    apply<BasePlugin>()

    val prefixedName = path.removePrefix(":").replace(":", "-")

    extensions.configure<BasePluginExtension> {
        archivesName.convention(prefixedName)
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        archiveBaseName.convention(prefixedName)
    }
}

rootProject.plugins.withType<YarnPlugin> {
    rootProject.the<YarnRootExtension>().apply {
        // Don't fail the build when yarn.lock changes (optional but common)
        yarnLockMismatchReportProperty.set(YarnLockMismatchReport.WARNING)
        // Always just regenerate/replace yarn.lock
        yarnLockAutoReplaceProperty.set(true)
        // Optional: if a new lock didn't exist before, don't spam about it
        reportNewYarnLockProperty.set(false)
    }
}

kotlin {
    js {
        compilerOptions {
            target.set("es2015")
        }

        browser {
            testTask {
            }
        }

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
                implementation(project(":monko:core"))

                implementation(Deps.KotlinLibs.faker)

                implementation(Deps.JavaLibs.logback_classic)
            }
        }
    }
}
