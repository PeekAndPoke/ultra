@file:Suppress("PropertyName")

import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.jfrog.bintray")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

val logback_version: String by project
val kotlintest_version: String by project
val kotlinx_coroutines_version: String by project
val slf4j_version: String by project

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    api(kotlin("reflect"))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version")
    api("org.slf4j:slf4j-api:$slf4j_version")

    api(project(":common"))
    api(project(":kontainer"))

    testImplementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlintest_version")
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
        description = "Logging with the Kontainer in mind"
        setLabels("kotlin")
        setLicenses("MIT")
        desc = description
    })
}
