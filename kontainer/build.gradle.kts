@file:Suppress("PropertyName")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

val logback_version: String by project
val kotlintest_version: String by project
val kotlinx_coroutines_version: String by project

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    api(kotlin("reflect"))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version")

    api(project(":commonmp"))
    api(project(":common"))

    testImplementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotlintest_version")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotlintest_version")
}

kotlin {
    sourceSets {
        test {
            kotlin.srcDir("src/examples")
        }
    }
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }

    dependsOn("generateDocs")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {

    create("runExamples", JavaExec::class) {
        main = "de.peekandpoke.ultra.kontainer.examples.IndexKt"
        classpath = sourceSets.getByName("test").runtimeClasspath
        standardInput = System.`in`
    }

    create("generateDocs", JavaExec::class) {
        main = "de.peekandpoke.ultra.kontainer.examples.GenerateDocsKt"
        classpath = sourceSets.getByName("test").runtimeClasspath
    }

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
//
