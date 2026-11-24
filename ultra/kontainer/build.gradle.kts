@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
    id("me.champeau.jmh") version Deps.Jmh.plugin_version
}

val ULTRA_GROUP = providers.gradleProperty("ULTRA_GROUP").get()
val VERSION_NAME = providers.gradleProperty("VERSION_NAME").get()

group = ULTRA_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(Deps.KotlinX.coroutines_core)

    api(project(":ultra:common"))
    implementation(project(":ultra:reflection"))

    // Tests /////////////////////////
    testImplementation(project(":tooling"))
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

    register<JavaExec>("runExamples") {
        mainClass.set("io.peekandpoke.ultra.kontainer.examples.IndexKt")
        classpath = sourceSets.getByName("test").runtimeClasspath
        standardInput = System.`in`
    }

    register<JavaExec>("generateDocs") {
        mainClass.set("io.peekandpoke.ultra.kontainer.examples.GenerateDocsKt")
        classpath = sourceSets.getByName("test").runtimeClasspath
    }
}

// Benchmarks (ULTRA-00351): console/wt-gradle.sh :ultra:kontainer:jmh [-PjmhIncludes=<regex>]
// Short iterations (1s) so a full run takes minutes; keep the settings the same for before and after.
jmh {
    jmhVersion = Deps.Jmh.jmh_version
    warmupIterations = 5
    warmup = "1s"
    iterations = 10
    timeOnIteration = "1s"
    fork = 2
    profilers = listOf("gc")
    resultFormat = "JSON"
    includes = listOfNotNull(providers.gradleProperty("jmhIncludes").orNull)
}

mavenPublishing {
}
