@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

val ULTRA_GROUP: String by project
val VERSION_NAME: String by project

group = ULTRA_GROUP
version = VERSION_NAME

dependencies {
    implementation(kotlin("reflect"))

    api(project(":ultra:common"))
    api(project(":ultra:meta"))

    kapt(Deps.JavaLibs.Google.auto_service)

    // Test /////////////////////////////////////////////////////////////////////////

    kaptTest(project(":ultra:mutator"))

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

    create("generateDocs", JavaExec::class) {
        mainClass.set("de.peekandpoke.ultra.mutator.examples.GenerateDocsKt")
        classpath = sourceSets.getByName("test").runtimeClasspath
    }
}

kapt {
    useBuildCache = true
}
