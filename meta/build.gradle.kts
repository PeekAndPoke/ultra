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

val google_auto_version: String by project
val kotlinpoet_version: String by project
val logback_version: String by project
val kotlintest_version: String by project
val kotlincompiletesting_version: String by project
val diffutils_version: String by project

dependencies {
    api(kotlin("reflect"))

    api(project(":commonmp"))
    api(project(":common"))

    //  code generation  //////////////////////////////////////////////////////////////////////////////////////

    api("com.github.tschuchortdev:kotlin-compile-testing:$kotlincompiletesting_version")
    api("com.github.wumpz:diffutils:$diffutils_version")

    api("com.squareup:kotlinpoet:$kotlinpoet_version")
    api("com.google.auto.service:auto-service:$google_auto_version")
    kapt("com.google.auto.service:auto-service:$google_auto_version")

    //  tests  ////////////////////////////////////////////////////////////////////////////////////////////////

    kaptTest("com.google.auto.service:auto-service:$google_auto_version")

    testApi("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotlintest_version")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotlintest_version")
}

repositories {
    mavenCentral()
    jcenter()
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

apply(from = "./../maven.publish.gradle.kts")

//val dokkaJar by tasks.creating(Jar::class) {
//    group = JavaBasePlugin.DOCUMENTATION_GROUP
//    archiveClassifier.set("javadoc")
//    from(tasks.dokkaHtml)
//}
//
//val sourcesJar by tasks.creating(Jar::class) {
//    archiveClassifier.set("sources")
//    from(sourceSets.getByName("main").allSource)
//}
//
//publishing {
//    publications {
//        create<MavenPublication>("default") {
//            from(components["java"])
//            artifact(dokkaJar)
//            artifact(sourcesJar)
//        }
//    }
//}

