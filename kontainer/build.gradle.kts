@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
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

    implementation(Deps.kotlinx_coroutines_core)

    api(project(":commonmp"))
    api(project(":common"))

    // Tests /////////////////////////
    Deps.Test {
        jvmTestDeps()
    }
}

kotlin {
    sourceSets {
        test {
            kotlin.srcDir("src/examples")
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    configureJvmTests {
        dependsOn("generateDocs")
    }

    create("runExamples", JavaExec::class) {
        main = "de.peekandpoke.ultra.kontainer.examples.IndexKt"
        classpath = sourceSets.getByName("test").runtimeClasspath
        standardInput = System.`in`
    }

    create("generateDocs", JavaExec::class) {
        main = "de.peekandpoke.ultra.kontainer.examples.GenerateDocsKt"
        classpath = sourceSets.getByName("test").runtimeClasspath
    }
}

apply(from = "./../maven.publish.gradle.kts")
