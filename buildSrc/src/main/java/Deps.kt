import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.TaskContainerScope
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

@Suppress("MemberVisibilityCanBePrivate")
object Deps {
    operator fun invoke(block: Deps.() -> Unit) {
        this.block()
    }

    // Kotlin ////////////////////////////////////////////////////////////////////////////////////
    const val kotlinVersion = "1.8.21"
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // JVM ///////////////////////////////////////////////////////////////////////////////////////
    val jvmTarget = JvmTarget.JVM_17
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // Dokka /////////////////////////////////////////////////////////////////////////////////////
    // https://mvnrepository.com/artifact/org.jetbrains.dokka/dokka-gradle-plugin
    // Dokka gradle plugin org.jetbrains.dokka
    const val dokkaVersion = "1.8.10" // kotlinVersion
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // Publishing ////////////////////////////////////////////////////////////////////////////////
    // TODO: Check update to 0.25.x version
    // https://search.maven.org/artifact/com.vanniktech/gradle-maven-publish-plugin
    const val mavenPublishVersion = "0.22.0"
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // TODO: Check update to 4.x version
    // https://mvnrepository.com/artifact/com.auth0/java-jwt
    private const val auth0_java_jwt_version = "3.19.4"
    const val auth0_java_jwt = "com.auth0:java-jwt:$auth0_java_jwt_version"

    // https://search.maven.org/artifact/io.github.java-diff-utils/java-diff-utils
    private const val diffutils_version = "4.12"
    const val diffutils = "io.github.java-diff-utils:java-diff-utils:$diffutils_version"

    // https://mvnrepository.com/artifact/com.google.auto.service/auto-service
    private const val google_auto_service_version = "1.0.1"
    const val google_auto_service = "com.google.auto.service:auto-service:$google_auto_service_version"

    // https://jitpack.io/#matfax/klassindex
    // TODO: remove this library and build out own!
    private const val klassIndexVersion = "4.1.0-rc.1"
    const val klassIndexLib = "com.github.matfax.klassindex:library:$klassIndexVersion"
    const val klassIndexProcessor = "com.github.matfax.klassindex:processor:$klassIndexVersion"

    // https://mvnrepository.com/artifact/com.github.tschuchortdev/kotlin-compile-testing
    private const val kotlin_compiletesting_version = "1.5.0"
    const val kotlin_compiletesting = "com.github.tschuchortdev:kotlin-compile-testing:$kotlin_compiletesting_version"

    // https://mvnrepository.com/artifact/com.squareup/kotlinpoet
    private const val kotlinpoet_version = "1.13.2"
    const val kotlinpoet = "com.squareup:kotlinpoet:$kotlinpoet_version"

    // https://kotlinlang.org/docs/releases.html#release-details
    // https://github.com/Kotlin/kotlinx.coroutines/releases
    private const val kotlinx_coroutines_version = "1.7.1"
    const val kotlinx_coroutines_core =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version"
    const val kotlinx_coroutines_core_js =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$kotlinx_coroutines_version"

    // https://kotlinlang.org/docs/releases.html#release-details
    // https://github.com/Kotlin/kotlinx.serialization/releases
    private const val kotlinx_serialization_version = "1.5.1"
    const val kotlinx_serialization_core =
        "org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinx_serialization_version"
    const val kotlinx_serialization_json =
        "org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version"

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
    private const val kotlinx_datetime_version = "0.4.0"
    const val kotlinx_datetime_common = "org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version"

    // https://mvnrepository.com/artifact/com.soywiz.korlibs.klock/klock
    private const val korlibs_klock_version = "4.0.0"
    const val korlibs_klock_common = "com.soywiz.korlibs.klock:klock:$korlibs_klock_version"

    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    private const val slf4j_version = "2.0.7"
    const val slf4j_api = "org.slf4j:slf4j-api:$slf4j_version"

    // // NPM dependencies /////////////////////////////////////////////////////////////////////////

    object Npm {
        operator fun <T> invoke(block: Npm.() -> T): T {
            return this.block()
        }

        // https://www.npmjs.com/package/whatwg-fetch
        fun KotlinDependencyHandler.polyfillFetch() = npm("whatwg-fetch", "3.6.2")

        // https://www.npmjs.com/package/@js-joda/core
        fun KotlinDependencyHandler.jsJodaCore() = npm("@js-joda/core", "5.5.3")

        // https://www.npmjs.com/package/@js-joda/timezone
        fun KotlinDependencyHandler.jsJodaTimezone() = npm("@js-joda/timezone", "2.18.0")
    }

    // // Test dependencies ////////////////////////////////////////////////////////////////////////

    object Test {

        operator fun invoke(block: Test.() -> Unit) {
            this.block()
        }

        // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
        const val logback_version = "1.4.7"
        const val logback_classic = "ch.qos.logback:logback-classic:$logback_version"

        // https://mvnrepository.com/artifact/io.kotest/kotest-common
        const val kotest_plugin_version = "5.6.2"
        const val kotest_version = "5.6.2"

        const val kotest_assertions_core = "io.kotest:kotest-assertions-core:$kotest_version"
        const val kotest_framework_api = "io.kotest:kotest-framework-api:$kotest_version"
        const val kotest_framework_datatest = "io.kotest:kotest-framework-datatest:$kotest_version"
        const val kotest_framework_engine = "io.kotest:kotest-framework-engine:$kotest_version"

        const val kotest_runner_junit_jvm = "io.kotest:kotest-runner-junit5-jvm:$kotest_version"

        fun KotlinDependencyHandler.commonTestDeps() {
            kotlin("test-common")
            kotlin("test-annotations-common")
            implementation(kotest_assertions_core)
            implementation(kotest_framework_api)
            implementation(kotest_framework_datatest)
            implementation(kotest_framework_engine)
        }

        fun KotlinDependencyHandler.jsTestDeps() {
            implementation(kotest_assertions_core)
            implementation(kotest_framework_api)
            implementation(kotest_framework_datatest)
            implementation(kotest_framework_engine)
        }

        fun KotlinDependencyHandler.jvmTestDeps() {
            implementation(logback_classic)
            implementation(kotest_runner_junit_jvm)
            implementation(kotest_assertions_core)
            implementation(kotest_framework_api)
            implementation(kotest_framework_datatest)
            implementation(kotest_framework_engine)
        }

        fun DependencyHandlerScope.jvmTestDeps() {
            testImplementation(logback_classic)
            testImplementation(kotest_runner_junit_jvm)
            testImplementation(kotest_assertions_core)
            testImplementation(kotest_framework_api)
            testImplementation(kotest_framework_engine)
        }

        fun TaskContainerScope.configureJvmTests(
            configure: org.gradle.api.tasks.testing.Test.() -> Unit = {},
        ) {
            listOfNotNull(
                findByName("test") as? org.gradle.api.tasks.testing.Test,
                findByName("jvmTest") as? org.gradle.api.tasks.testing.Test,
            ).firstOrNull()?.apply {
                useJUnitPlatform { }

                filter {
                    isFailOnNoMatchingTests = false
                }

//                testLogging {
//                    showExceptions = true
//                    showStandardStreams = true
//                    events = setOf(
//                        org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
//                        org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
//                    )
//                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
//                }

                configure()
            }
        }
    }
}

private fun DependencyHandlerScope.testImplementation(dep: String) =
    add("testImplementation", dep)
