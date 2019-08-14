
//buildscript {
//    repositories {
//        mavenCentral()
//        google()
//        gradlePluginPortal() // Required for the Errorprone Gradle Plugin.
//    }
//
//    dependencies {
//        classpath("com.vanniktech:gradle-code-quality-tools-plugin:0.18.0")
//    }
//}

plugins {
    idea
    `build-scan`
    kotlin("jvm") version "1.3.41"
//    id("com.vanniktech.code.quality.tools") version "0.18.0"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishAlways()
}

//codeQualityTools {
//    failEarly = true
//    xmlReports = true
//    htmlReports = true
//    textReports = true
//
//    detekt {
//
//    }
//}
