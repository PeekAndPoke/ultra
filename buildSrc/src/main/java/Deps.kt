import org.gradle.kotlin.dsl.DependencyHandlerScope

object Deps {
    operator fun invoke(block: Deps.() -> Unit) {
        this.block()
    }

    ////////////////////////////////////////////////////////////////////
    const val kotlinVersion = "1.5.10"
    ////////////////////////////////////////////////////////////////////

    // Publishing //////////////////////////////////////////////////////
    // TODO: Upgrade to 0.15.x -> Beware: configuration changes are necessary
    const val mavenPublishVersion = "0.14.2"
    ////////////////////////////////////////////////////////////////////

    private const val auth0_java_jwt_version = "3.17.0"
    const val auth0_java_jwt = "com.auth0:java-jwt:$auth0_java_jwt_version"

    // TODO: check if we can update to 3.x
    private const val diffutils_version = "2.2"
    const val diffutils = "com.github.wumpz:diffutils:$diffutils_version"

    const val dokkaVersion = "1.4.32"

    private const val google_auto_service_version = "1.0"
    const val google_auto_service = "com.google.auto.service:auto-service:$google_auto_service_version"

    private const val klassIndexVersion = "4.1.0-rc.1"
    const val klassIndexLib = "com.github.matfax.klassindex:library:$klassIndexVersion"
    const val klassIndexProcessor = "com.github.matfax.klassindex:processor:$klassIndexVersion"

    private const val kotlin_compiletesting_version = "1.4.2"
    const val kotlin_compiletesting = "com.github.tschuchortdev:kotlin-compile-testing:$kotlin_compiletesting_version"

    // TODO: check 1.6.+ ... there are some breaking changes though
    private const val kotlinpoet_version = "1.9.0"
    const val kotlinpoet = "com.squareup:kotlinpoet:$kotlinpoet_version"

    private const val kotlinx_coroutines_version = "1.5.0"
    const val kotlinx_coroutines_core =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version"

    private const val kotlinx_serialization_version = "1.2.1"
    const val kotlinx_serialization_core =
        "org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinx_serialization_version"
    const val kotlinx_serialization_json =
        "org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version"

    private const val korlibs_klock_version = "2.1.2"
    const val korlibs_klock_js = "com.soywiz.korlibs.klock:klock-js:$korlibs_klock_version"
    const val korlibs_klock_jvm = "com.soywiz.korlibs.klock:klock-jvm:$korlibs_klock_version"

    private const val slf4j_version = "1.7.30"
    const val slf4j_api = "org.slf4j:slf4j-api:$slf4j_version"

    //// NPM dependencies /////////////////////////////////////////////////////////////////////////

    //// Test dependencies ////////////////////////////////////////////////////////////////////////

    object Test {

        operator fun invoke(block: Test.() -> Unit) {
            this.block()
        }

        private const val logback_version = "1.2.3"
        const val logback_classic = "ch.qos.logback:logback-classic:$logback_version"

        private const val kotest_version = "4.6.0"
        const val kotest_assertions_core_jvm = "io.kotest:kotest-assertions-core-jvm:$kotest_version"
        const val kotest_runner_junit_jvm = "io.kotest:kotest-runner-junit5-jvm:$kotest_version"

        fun DependencyHandlerScope.jvmTestDeps() {
            testImplementation(logback_classic)
            testImplementation(kotest_assertions_core_jvm)
            testImplementation(kotest_runner_junit_jvm)
        }
    }
}

private fun DependencyHandlerScope.testImplementation(dep: String) =
    add("testImplementation", dep)
