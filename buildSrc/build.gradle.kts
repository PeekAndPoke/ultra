plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")

    implementation("org.ajoberstar.grgit:grgit-core:5.2.0")

    implementation(gradleApi())
}
