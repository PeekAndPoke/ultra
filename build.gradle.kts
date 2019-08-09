
plugins {
    `build-scan`
    kotlin("jvm") version "1.3.41"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    api(rootProject.project("proto:package_a"))
    api(rootProject.project("proto:package_b"))
    api(kotlin("stdlib-jdk8"))
}


buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishAlways()
}
