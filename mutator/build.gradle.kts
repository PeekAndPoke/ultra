@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.google.devtools.ksp") version Deps.kspVersion
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

    implementation(Deps.ksp)

    api(project(":meta"))

    kapt(Deps.google_auto_service)

    // Test /////////////////////////////////////////////////////////////////////////

    kaptTest(project(":mutator"))
    kspTest(project(":mutator"))

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

kapt {
    useBuildCache = true
}

tasks {
    withType<KotlinCompile>().all {
        compilerOptions {
            jvmTarget.set(Deps.jvmTarget)
        }
    }

    configureJvmTests {
        dependsOn("generateDocs")
    }

    create("generateDocs", JavaExec::class) {
        main = "de.peekandpoke.ultra.mutator.examples.GenerateDocsKt"
        classpath = sourceSets.getByName("test").runtimeClasspath
    }
}
