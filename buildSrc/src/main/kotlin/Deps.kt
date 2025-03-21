import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.TaskContainerScope
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

@Suppress("MemberVisibilityCanBePrivate", "ConstPropertyName")
object Deps {
    operator fun invoke(block: Deps.() -> Unit) {
        this.block()
    }

    // Kotlin ////////////////////////////////////////////////////////////////////////////////////
    const val kotlinVersion = "2.1.10"

    object Ksp {
        // https://github.com/google/ksp/releases
        const val version = "2.1.10-1.0.31"
        const val symbol_processing = "com.google.devtools.ksp:symbol-processing-api:$version"

        // https://mvnrepository.com/artifact/com.github.tschuchortdev/kotlin-compile-testing
        private const val compiletesting_version = "1.6.0"
        const val compiletesting = "com.github.tschuchortdev:kotlin-compile-testing:$compiletesting_version"
        const val compiletesting_ksp = "com.github.tschuchortdev:kotlin-compile-testing-ksp:$compiletesting_version"
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////

    // JVM ///////////////////////////////////////////////////////////////////////////////////////
    val jvmTarget: JvmTarget = JvmTarget.JVM_17
    val jvmTargetVersion: Int = jvmTarget.target.toInt()
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // Dokka /////////////////////////////////////////////////////////////////////////////////////
    // https://mvnrepository.com/artifact/org.jetbrains.dokka/dokka-gradle-plugin
    // Dokka gradle plugin org.jetbrains.dokka
    const val dokkaVersion = "2.0.0" // kotlinVersion
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // Publishing ////////////////////////////////////////////////////////////////////////////////
    // https://search.maven.org/artifact/com.vanniktech/gradle-maven-publish-plugin
    const val mavenPublishVersion = "0.30.0"
    // ///////////////////////////////////////////////////////////////////////////////////////////

    object KotlinLibs {
        // https://mvnrepository.com/artifact/com.github.ajalt.clikt/clikt
        private const val clikt_version = "5.0.3"
        const val clikt = "com.github.ajalt.clikt:clikt:$clikt_version"

        // https://mvnrepository.com/artifact/com.github.doyaaaaaken/kotlin-csv
        private const val csv_version = "1.10.0"
        const val csv = "com.github.doyaaaaaken:kotlin-csv:$csv_version"

        // https://mvnrepository.com/artifact/io.github.evanrupert/excelkt
        private const val excelkt_version = "1.0.2"
        const val excelkt = "io.github.evanrupert:excelkt:$excelkt_version"

        // https://mvnrepository.com/artifact/com.benasher44/uuid
        private const val uuid_version = "0.8.4"
        const val uuid = "com.benasher44:uuid:$uuid_version"

        // TODO: get rid of this ... still needed for date formatting
        // https://mvnrepository.com/artifact/com.soywiz/korlibs-time
        private const val korlibs_time_version = "6.0.0"
        const val korlibs_time = "com.soywiz:korlibs-time:$korlibs_time_version"

        // https://mvnrepository.com/artifact/io.github.g0dkar/qrcode-kotlin
        private const val qrcode_version = "4.3.0"
        const val qrcode = "io.github.g0dkar:qrcode-kotlin:$qrcode_version"

        // https://mvnrepository.com/artifact/io.github.serpro69/kotlin-faker
        private const val faker_version = "1.16.0"
        const val faker = "io.github.serpro69:kotlin-faker:$faker_version"

        // https://mvnrepository.com/artifact/com.squareup/kotlinpoet
        private const val kotlinpoet_version = "2.1.0"
        const val kotlinpoet = "com.squareup:kotlinpoet:$kotlinpoet_version"
    }

    object KotlinX {
        // https://github.com/Kotlin/kotlinx.coroutines/releases
        private const val coroutines_version = "1.10.1"
        const val coroutines_core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version"

        // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
        private const val datetime_version = "0.6.2"
        const val datetime = "org.jetbrains.kotlinx:kotlinx-datetime:$datetime_version"

        // https://github.com/Kotlin/kotlinx.serialization/releases
        private const val serialization_version = "1.8.0"
        const val serialization_core =
            "org.jetbrains.kotlinx:kotlinx-serialization-core:$serialization_version"
        const val serialization_json =
            "org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version"

        // https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven/org/jetbrains/kotlinx/kotlinx-html/
        private const val html_version = "0.12.0"
        const val html = "org.jetbrains.kotlinx:kotlinx-html:$html_version"

        // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-css
        private const val wrappers_version = "2025.3.2"
        const val wrappers_css =
            "org.jetbrains.kotlin-wrappers:kotlin-css:$wrappers_version"

        // NOTICE: KEEP the pre.450 as newer versions do NOT support JAVA eight anymore
        // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-extensions
        private const val wrappers_extensions_version = "1.0.1-pre.823"
        const val wrappers_extensions =
            "org.jetbrains.kotlin-wrappers:kotlin-extensions:$wrappers_extensions_version"
    }

    object Ktor {
        // https://kotlinlang.org/docs/releases.html
        // https://github.com/ktorio/ktor/releases
        const val ktor_version = "3.1.1"

        object Server {
            object Test {
                const val host = "io.ktor:ktor-server-test-host:$ktor_version"
            }

            const val auth = "io.ktor:ktor-server-auth:$ktor_version"
            const val auth_jwt = "io.ktor:ktor-server-auth-jwt:$ktor_version"
            const val auto_head = "io.ktor:ktor-server-auto-head-response:$ktor_version"
            const val caching_headers = "io.ktor:ktor-server-caching-headers:$ktor_version"
            const val content_negotiation = "io.ktor:ktor-server-content-negotiation:$ktor_version"
            const val compression = "io.ktor:ktor-server-compression:$ktor_version"
            const val core = "io.ktor:ktor-server-core:$ktor_version"
            const val cors = "io.ktor:ktor-server-cors:$ktor_version"
            const val default_headers = "io.ktor:ktor-server-default-headers:$ktor_version"
            const val hsts = "io.ktor:ktor-server-hsts:$ktor_version"
            const val html_builder = "io.ktor:ktor-server-html-builder:$ktor_version"
            const val host_common = "io.ktor:ktor-server-host-common:$ktor_version"
            const val netty = "io.ktor:ktor-server-netty:$ktor_version"
            const val metrics = "io.ktor:ktor-server-metrics:$ktor_version"
            const val partial_content = "io.ktor:ktor-server-partial-content:$ktor_version"
            const val sessions = "io.ktor:ktor-server-sessions:$ktor_version"
            const val status_pages = "io.ktor:ktor-server-status-pages:$ktor_version"
            const val webjars = "io.ktor:ktor-server-webjars:$ktor_version"
            const val websockets = "io.ktor:ktor-server-websockets:$ktor_version"
            const val double_receive = "io.ktor:ktor-server-double-receive:$ktor_version"

            fun full(scope: DependencyHandlerScope) = with(scope) {
                implementation(auth)
                implementation(auth_jwt)
                implementation(auto_head)
                implementation(caching_headers)
                implementation(content_negotiation)
                implementation(compression)
                implementation(core)
                implementation(cors)
                implementation(default_headers)
                implementation(hsts)
                implementation(html_builder)
                implementation(host_common)
                implementation(netty)
                implementation(metrics)
                implementation(partial_content)
                implementation(sessions)
                implementation(status_pages)
                implementation(webjars)
                implementation(websockets)
                implementation(double_receive)
                // Tests
                testImplementation(Test.host)
            }
        }

        object Client {
            const val core = "io.ktor:ktor-client-core:$ktor_version"
            const val content_negotiation = "io.ktor:ktor-client-content-negotiation:$ktor_version"
            const val websockets = "io.ktor:ktor-client-websockets:$ktor_version"
            const val apache = "io.ktor:ktor-client-apache:$ktor_version"
            const val cio = "io.ktor:ktor-client-cio:$ktor_version"
            const val json = "io.ktor:ktor-client-json:$ktor_version"
            const val okhttp = "io.ktor:ktor-client-okhttp:$ktor_version"

            fun full(scope: DependencyHandlerScope) = with(scope) {
                implementation(core)
                implementation(content_negotiation)
                implementation(apache)
                implementation(cio)
                implementation(json)
                implementation(okhttp)
            }
        }

        object Common {
            const val serialization_jackson = "io.ktor:ktor-serialization-jackson:$ktor_version"
            const val serialization_kotlinx_json = "io.ktor:ktor-serialization-kotlinx-json:$ktor_version"
        }
    }

    object JavaLibs {
        object ArangoDb {
            // https://mvnrepository.com/artifact/com.arangodb/arangodb-java-driver
            private const val driver_version = "7.17.0"
            const val java_driver = "com.arangodb:arangodb-java-driver:$driver_version"
        }

        object Jackson {
            // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
            private const val jackson_version = "2.18.3"

            // https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-kotlin
            private const val jackson_kotlin_module_version = "2.18.3"

            const val databind = "com.fasterxml.jackson.core:jackson-databind:$jackson_version"
            const val annotations = "com.fasterxml.jackson.core:jackson-annotations:$jackson_version"
            const val datatype_jdk8 = "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jackson_version"
            const val datatype_jsr310 = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version"

            const val module_kotlin =
                "com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_kotlin_module_version"

            fun fullImpl(scope: DependencyHandlerScope) = with(scope) {
                implementation(databind)
                implementation(annotations)
                implementation(datatype_jdk8)
                implementation(datatype_jsr310)
                implementation(module_kotlin)
            }
        }

        object Aws {
            // https://mvnrepository.com/artifact/software.amazon.awssdk/s3
            const val awssdk_version = "2.25.26"

            const val s3 = "software.amazon.awssdk:s3:$awssdk_version"
            const val ses = "software.amazon.awssdk:ses:$awssdk_version"
        }

        object Stripe {
            // TODO: upgrade at some point BUT this also means an API-Level update ...
            // https://mvnrepository.com/artifact/com.stripe/stripe-java
            private const val sdk_version = "21.15.0"
            const val java_sdk = "com.stripe:stripe-java:$sdk_version"
        }

        object Google {
            // https://mvnrepository.com/artifact/com.google.auto.service/auto-service
            private const val auto_service_version = "1.1.1"
            const val auto_service = "com.google.auto.service:auto-service:$auto_service_version"

            // https://mvnrepository.com/artifact/com.google.api-client/google-api-client
            private const val api_client_version = "2.7.2"
            const val api_client = "com.google.api-client:google-api-client:$api_client_version"

            // https://mvnrepository.com/artifact/com.google.firebase/firebase-admin
            private const val firebase_admin_version = "9.2.0"
            const val firebase_admin = "com.google.firebase:firebase-admin:$firebase_admin_version"
        }

        object Pdf {
            // https://mvnrepository.com/artifact/org.xhtmlrenderer/flying-saucer-core
            private const val flying_saucer_version = "9.7.2"
            const val flying_saucer_core = "org.xhtmlrenderer:flying-saucer-core:$flying_saucer_version"

            // https://mvnrepository.com/artifact/org.xhtmlrenderer/flying-saucer-pdf-itext5
            private const val flying_saucer_itext5_version = "9.7.2"
            const val flying_saucer_itext = "org.xhtmlrenderer:flying-saucer-pdf-itext5:$flying_saucer_itext5_version"

            // https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox
            private const val apache_pdfbox_version = "3.0.4"
            const val apache_pdfbox = "org.apache.pdfbox:pdfbox:$apache_pdfbox_version"

            fun full(scope: DependencyHandlerScope) = with(scope) {
                implementation(flying_saucer_core)
                implementation(flying_saucer_itext)
                implementation(apache_pdfbox)
            }
        }

        object Moshi {
            // https://mvnrepository.com/artifact/com.squareup.moshi/moshi-kotlin
            private const val moshi_version = "1.15.2"
            const val kotlin = "com.squareup.moshi:moshi-kotlin:$moshi_version"
            const val adapters = "com.squareup.moshi:moshi-adapters:$moshi_version"
        }

        object ApacheCommons {
            // https://mvnrepository.com/artifact/org.apache.commons/commons-email
            private const val email_version = "1.6.0"
            const val email = "org.apache.commons:commons-email:$email_version"

            // https://mvnrepository.com/artifact/commons-cli/commons-cli
            private const val cli_version = "1.6.0"
            const val cli = "commons-cli:commons-cli:$cli_version"
        }

        // https://mvnrepository.com/artifact/com.auth0/java-jwt
        private const val auth0_java_jwt_version = "4.5.0"
        const val auth0_java_jwt = "com.auth0:java-jwt:$auth0_java_jwt_version"

        // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
        private const val logback_version = "1.5.17"
        const val logback_classic = "ch.qos.logback:logback-classic:$logback_version"

        // https://github.com/atteo/classindex
        // https://search.maven.org/artifact/org.atteo.classindex/classindex
        private const val classindex_version = "3.13"
        const val classindex = "org.atteo.classindex:classindex:$classindex_version"

        // https://mvnrepository.com/artifact/org.commonmark/commonmark
        private const val commonmark_version = "0.24.0"
        const val commonmark = "org.commonmark:commonmark:$commonmark_version"
        const val commonmark_ext_gfm_tables = "org.commonmark:commonmark-ext-gfm-tables:$commonmark_version"

        // https://mvnrepository.com/artifact/com.talanlabs/avatar-generator
        private const val avatar_generator_version = "1.1.0"
        const val avatar_generator = "com.talanlabs:avatar-generator:$avatar_generator_version"
        const val avatar_generator_smiley = "com.talanlabs:avatar-generator-smiley:$avatar_generator_version"
        const val avatar_generator_8bit = "com.talanlabs:avatar-generator-8bit:$avatar_generator_version"

        // https://mvnrepository.com/artifact/com.notnoop.apns/apns
        // Java Apple Push Notification Service Library
        private const val apns_version = "1.0.0.Beta6"
        const val apns = "com.notnoop.apns:apns:$apns_version"

        // https://mvnrepository.com/artifact/org.apache.tika/tika-core
        // ... mime type detector based on content of file
        private const val tika_version = "3.1.0"
        const val tika_core = "org.apache.tika:tika-core:$tika_version"

        // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
        private const val okhttp_version = "4.12.0"
        const val okhttp = "com.squareup.okhttp3:okhttp:$okhttp_version"

        // https://search.maven.org/artifact/io.github.java-diff-utils/java-diff-utils
        private const val diffutils_version = "4.15"
        const val diffutils = "io.github.java-diff-utils:java-diff-utils:$diffutils_version"

        // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
        private const val slf4j_version = "2.0.17"
        const val slf4j_api = "org.slf4j:slf4j-api:$slf4j_version"
    }

    // // NPM dependencies /////////////////////////////////////////////////////////////////////////

    object Npm {
        operator fun <T> invoke(block: Npm.() -> T): T {
            return this.block()
        }

        // https://www.npmjs.com/package/whatwg-fetch
        fun KotlinDependencyHandler.polyfillFetch() = npm("whatwg-fetch", "3.6.20")

        // https://www.npmjs.com/package/@js-joda/core
        fun KotlinDependencyHandler.jsJodaCore() = npm("@js-joda/core", "5.6.4")

        // https://www.npmjs.com/package/@js-joda/timezone
        fun KotlinDependencyHandler.jsJodaTimezone() = npm("@js-joda/timezone", "2.21.2")
    }

    // // Test dependencies ////////////////////////////////////////////////////////////////////////

    object Test {

        operator fun invoke(block: Test.() -> Unit) {
            this.block()
        }

        // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
        const val logback_version = "1.5.17"
        const val logback_classic = "ch.qos.logback:logback-classic:$logback_version"

        // https://plugins.gradle.org/plugin/io.kotest.multiplatform
//        const val kotest_plugin_version = "6.0.0.M2"
        const val kotest_plugin_version = "5.9.1"

        //        const val kotest_plugin_version = "6.0.0.M1"
        // https://mvnrepository.com/artifact/io.kotest/kotest-common
//        const val kotest_version = "6.0.0.M1"
        const val kotest_version = "5.9.1"

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
            withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
                useJUnitPlatform()

                outputs.upToDateWhen { false }

                filter {
                    isFailOnNoMatchingTests = true
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

    private fun DependencyHandlerScope.testImplementation(dep: String) =
        add("testImplementation", dep)

    private fun DependencyHandlerScope.api(dep: String) =
        add("api", dep)

    private fun DependencyHandlerScope.implementation(dep: String) =
        add("implementation", dep)
}
