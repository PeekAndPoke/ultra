@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

val ULTRA_GROUP: String by project
val VERSION_NAME: String by project

group = ULTRA_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(Deps.KotlinX.coroutines_core)

    api(project(":ultra:common"))

    // Tests /////////////////////////
    Deps.Test {
        jvmTestDeps()
    }
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)

    sourceSets {
        test {
            kotlin.srcDir("src/examples")
        }
    }
}

tasks {
    configureJvmTests {
        dependsOn("generateDocs")
    }

    create("runExamples", JavaExec::class) {
        mainClass.set("de.peekandpoke.ultra.kontainer.examples.IndexKt")
        classpath = sourceSets.getByName("test").runtimeClasspath
        standardInput = System.`in`
    }

    create("generateDocs", JavaExec::class) {
        mainClass.set("de.peekandpoke.ultra.kontainer.examples.GenerateDocsKt")
        classpath = sourceSets.getByName("test").runtimeClasspath
    }
}
