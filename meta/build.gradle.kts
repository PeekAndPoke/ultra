@file:Suppress("PropertyName")

import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.dokka") version "0.9.18"
    id("com.jfrog.bintray") version "1.8.4"
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
    api(rootProject.project("common"))

    api(kotlin("stdlib-jdk8"))
    api(kotlin("reflect"))

    ////  code generation  //////////////////////////////////////////////////////////////////////////////////////

    compile("com.github.tschuchortdev:kotlin-compile-testing:$kotlincompiletesting_version")
    compile("com.github.wumpz:diffutils:$diffutils_version")

    compile("com.squareup:kotlinpoet:$kotlinpoet_version")
    compile("com.google.auto.service:auto-service:$google_auto_version")
    kapt("com.google.auto.service:auto-service:$google_auto_version")

    ////  tests  ////////////////////////////////////////////////////////////////////////////////////////////////

    kaptTest("com.google.auto.service:auto-service:$google_auto_version")

    testCompile("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlintest_version")
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

    dokka {
        outputFormat = "html"
        outputDirectory = "$buildDir/javadoc"
    }
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            artifact(dokkaJar)
            artifact(sourcesJar)
        }
    }
}

fun stringProperty(s: String) = project.findProperty(s) as String?

bintray {
    user = stringProperty("bintrayUser")
    key = stringProperty("bintrayApiKey")
    publish = true
    setPublications("default")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "de.peekandpoke.ultra.${project.name}"
        userOrg = "peekandpoke"
        description = "Common helpers und utils"
        setLabels("kotlin")
        setLicenses("MIT")
        desc = description
    })
}
