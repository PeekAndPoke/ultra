@file:Suppress("PropertyName")

// JMH benchmarks for ultra/kontainer (ULTRA-00351, ULTRA-00353). Not published: no maven publish plugin.
// Run: console/wt-gradle.sh :ultra:kontainer-benchmarks:jmh [-PjmhIncludes=<regex>]

plugins {
    kotlin("jvm")
    id("me.champeau.jmh") version Deps.Jmh.plugin_version
}

val ULTRA_GROUP = providers.gradleProperty("ULTRA_GROUP").get()
val VERSION_NAME = providers.gradleProperty("VERSION_NAME").get()

group = ULTRA_GROUP
version = VERSION_NAME

dependencies {
    jmhImplementation(project(":ultra:kontainer"))
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)
}

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
