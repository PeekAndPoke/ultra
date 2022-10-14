plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp") version Deps.kspVersion
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

repositories {
    mavenCentral()
}

dependencies {
    implementation(Deps.kotlinx_serialization_core)
    implementation(Deps.kotlinx_serialization_json)
    implementation(Deps.kotlinx_coroutines_core)

    // project deps
    api(project(":common"))
    api(project(":slumber"))
    api(project(":kontainer"))
    api(project(":meta"))
    ksp(project(":meta"))
}

kotlin {
}
