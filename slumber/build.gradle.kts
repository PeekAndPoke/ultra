@file:Suppress("PropertyName")

import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.dokka")
    id("com.jfrog.bintray")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

val logback_version: String by project
val classindex_version: String by project
val kotlintest_version: String by project
val klock_version: String by project

repositories {
    mavenCentral()
    jcenter()
    // Repo for KlassIndex (https://github.com/matfax/klassindex)
    maven("https://jitpack.io")
}

dependencies {
    api(kotlin("reflect"))

    api(project(":common"))
    api(project(":common-mp"))

    api("org.atteo.classindex:classindex:$classindex_version")
    kapt("org.atteo.classindex:classindex:$classindex_version")

    // Testing
    testImplementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlintest_version")

    kaptTest("org.atteo.classindex:classindex:$classindex_version")
}

kapt {
    useBuildCache = true
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
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
    override = true
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
