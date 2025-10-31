plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")

    implementation("org.ajoberstar.grgit:grgit-core:5.3.3")

    implementation(gradleApi())
}
