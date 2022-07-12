plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")

    implementation(gradleApi())
}
