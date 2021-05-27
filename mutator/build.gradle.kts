@file:Suppress("PropertyName")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.dokka")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("reflect"))

    api(project(":meta"))

    kapt(Deps.google_auto_service)

    // Test /////////////////////////////////////////////////////////////////////////

    kaptTest(project(":mutator"))

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
    useBuildCache = false
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }

    dependsOn("generateDocs")
}

tasks {

    create("generateDocs", JavaExec::class) {
        main = "de.peekandpoke.ultra.mutator.examples.GenerateDocsKt"
        classpath = sourceSets.getByName("test").runtimeClasspath
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

apply(from = "./../maven.publish.gradle.kts")
