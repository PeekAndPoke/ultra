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

dependencies {

    // add all child projects
    api(project(":common"))

    api(project(":meta"))

    api(project(":mutator"))
    kapt(project(":mutator"))
    kaptTest(project(":mutator"))

    api(project(":kontainer"))

    api(project(":security"))

    api(project(":slumber"))
    kapt(project(":slumber"))
    kaptTest(project(":slumber"))
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

