@file:Suppress("detekt:all", "unused")

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.TaskContainerScope
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("MemberVisibilityCanBePrivate", "ConstPropertyName")
object Deps {
    operator fun invoke(block: Deps.() -> Unit) {
        this.block()
    }

    // Kotlin ////////////////////////////////////////////////////////////////////////////////////
    const val kotlinVersion = "2.2.20"

    object Ksp {
        // https://github.com/google/ksp/releases
        const val version = "2.2.21-2.0.4"
        const val symbol_processing = "com.google.devtools.ksp:symbol-processing-api:$version"

        // https://mvnrepository.com/artifact/dev.zacsweers.kctfork/ksp
        private const val compiletesting_version = "0.11.0"
        const val compiletesting_ksp = "dev.zacsweers.kctfork:ksp:$compiletesting_version"
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////

    // JVM ///////////////////////////////////////////////////////////////////////////////////////
    val jvmTarget: JvmTarget = JvmTarget.JVM_17
    val jvmTargetVersion: Int = jvmTarget.target.toInt()
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // Dokka /////////////////////////////////////////////////////////////////////////////////////
    // https://mvnrepository.com/artifact/org.jetbrains.dokka/dokka-gradle-plugin
    // Dokka gradle plugin org.jetbrains.dokka
    const val dokkaVersion = "2.1.0" // kotlinVersion
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // Publishing ////////////////////////////////////////////////////////////////////////////////
    // https://search.maven.org/artifact/com.vanniktech/gradle-maven-publish-plugin
    const val mavenPublishVersion = "0.32.0"
    // ///////////////////////////////////////////////////////////////////////////////////////////

    object KotlinLibs {
        // https://central.sonatype.com/search?q=g:io.github.reactivecircus.cache4k
        private const val cache4k_version = "0.14.0"
        const val cache4k = "io.github.reactivecircus.cache4k:cache4k:$cache4k_version"

        // https://mvnrepository.com/artifact/com.github.ajalt.clikt/clikt
        private const val clikt_version = "5.0.3"
        const val clikt = "com.github.ajalt.clikt:clikt:$clikt_version"

        // https://mvnrepository.com/artifact/com.github.doyaaaaaken/kotlin-csv
        private const val csv_version = "1.10.0"
        const val csv = "com.jsoizo:kotlin-csv:$csv_version"

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
        private const val qrcode_version = "4.5.0"
        const val qrcode = "io.github.g0dkar:qrcode-kotlin:$qrcode_version"

        // https://mvnrepository.com/artifact/io.github.serpro69/kotlin-faker
        private const val faker_version = "1.16.0"
        const val faker = "io.github.serpro69:kotlin-faker:$faker_version"

        // https://mvnrepository.com/artifact/com.squareup/kotlinpoet
        private const val kotlinpoet_version = "2.2.0"
        const val kotlinpoet = "com.squareup:kotlinpoet:$kotlinpoet_version"

        object MongoDb {
            private const val mongodb_driver_version = "5.6.1"
            const val mongodb_driver_bom = "org.mongodb:mongodb-driver-bom:$mongodb_driver_version"
            const val mongodb_driver_kotlin_coroutine =
                "org.mongodb:mongodb-driver-kotlin-coroutine:$mongodb_driver_version"
            const val mongodb_bson_kotlinx =
                "org.mongodb:bson-kotlinx:$mongodb_driver_version"
        }
    }

    object KotlinX {
        // https://github.com/Kotlin/kotlinx.coroutines/releases
        private const val coroutines_version = "1.10.2"
        const val coroutines_core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version"
        const val coroutines_test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines_version"

        // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
        private const val datetime_version = "0.6.2"
        const val datetime = "org.jetbrains.kotlinx:kotlinx-datetime:$datetime_version"

        // https://github.com/Kotlin/kotlinx.serialization/releases
        private const val serialization_version = "1.9.0"
        const val serialization_core =
            "org.jetbrains.kotlinx:kotlinx-serialization-core:$serialization_version"
        const val serialization_json =
            "org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version"

        // https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven/org/jetbrains/kotlinx/kotlinx-html/
        private const val html_version = "0.12.0"
        const val html = "org.jetbrains.kotlinx:kotlinx-html:$html_version"

        // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-css
        private const val wrappers_version = "2025.10.7"
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
        const val ktor_version = "3.3.1"

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
            const val sse = "io.ktor:ktor-server-sse:$ktor_version"
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
                implementation(sse)
                implementation(status_pages)
                implementation(webjars)
                implementation(websockets)
                implementation(double_receive)
                // Tests
                testImplementation(Test.host)
            }

            fun full(scope: KotlinDependencyHandler) = with(scope) {
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
                implementation(sse)
                implementation(status_pages)
                implementation(webjars)
                implementation(websockets)
                implementation(double_receive)
            }

            fun fullTest(scope: KotlinDependencyHandler) = with(scope) {
                implementation(Test.host)
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
            private const val driver_version = "7.22.1"
            const val java_driver = "com.arangodb:arangodb-java-driver:$driver_version"
        }

        object ApacheCommons {
            // https://mvnrepository.com/artifact/org.apache.commons/commons-email
            private const val email_version = "1.6.0"
            const val email = "org.apache.commons:commons-email:$email_version"

            // https://mvnrepository.com/artifact/commons-cli/commons-cli
            private const val cli_version = "1.10.0"
            const val cli = "commons-cli:commons-cli:$cli_version"
        }

        object Aws {
            // https://mvnrepository.com/artifact/software.amazon.awssdk/s3
            const val awssdk_version = "2.35.8"

            const val s3 = "software.amazon.awssdk:s3:$awssdk_version"
            const val ses = "software.amazon.awssdk:ses:$awssdk_version"
        }

        object Google {
            // https://mvnrepository.com/artifact/com.google.auto.service/auto-service
            private const val auto_service_version = "1.1.1"
            const val auto_service = "com.google.auto.service:auto-service:$auto_service_version"

            // https://mvnrepository.com/artifact/com.google.api-client/google-api-client
            private const val api_client_version = "2.8.1"
            const val api_client = "com.google.api-client:google-api-client:$api_client_version"

            // https://mvnrepository.com/artifact/com.google.firebase/firebase-admin
            private const val firebase_admin_version = "9.7.0"
            const val firebase_admin = "com.google.firebase:firebase-admin:$firebase_admin_version"
        }

        object Jackson {
            // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
            private const val jackson_version = "2.19.0"

            // https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-kotlin
            private const val jackson_kotlin_module_version = "2.20.0"

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

        object Sendgrid {
            // https://mvnrepository.com/artifact/com.sendgrid/sendgrid-java
            private const val sendgrid_version = "5.0.0-rc.1"
            const val sendgrid_java = "com.sendgrid:sendgrid-java:$sendgrid_version"
        }

        // https://mvnrepository.com/artifact/com.auth0/java-jwt
        private const val auth0_java_jwt_version = "4.5.0"
        const val auth0_java_jwt = "com.auth0:java-jwt:$auth0_java_jwt_version"

        // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
        private const val logback_version = "1.5.19"
        const val logback_classic = "ch.qos.logback:logback-classic:$logback_version"

        // https://github.com/atteo/classindex
        // https://search.maven.org/artifact/org.atteo.classindex/classindex
        private const val classindex_version = "3.13"
        const val classindex = "org.atteo.classindex:classindex:$classindex_version"

        // https://mvnrepository.com/artifact/org.commonmark/commonmark
        private const val commonmark_version = "0.27.0"
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
        private const val tika_version = "3.2.3"
        const val tika_core = "org.apache.tika:tika-core:$tika_version"

        // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
        private const val okhttp_version = "5.2.1"
        const val okhttp = "com.squareup.okhttp3:okhttp:$okhttp_version"

        // https://search.maven.org/artifact/io.github.java-diff-utils/java-diff-utils
        private const val diffutils_version = "4.16"
        const val diffutils = "io.github.java-diff-utils:java-diff-utils:$diffutils_version"

        //https://mvnrepository.com/artifact/com.password4j/password4j
        private const val password4j_version = "1.8.4"
        const val password4j = "com.password4j:password4j:$password4j_version"

        // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
        private const val slf4j_version = "2.0.17"
        const val slf4j_api = "org.slf4j:slf4j-api:$slf4j_version"
    }

    object WebJars {
        // jQuery: https://www.webjars.org/
        private const val jquery_version = "3.7.1"
        const val jquery = "org.webjars:jquery:$jquery_version"

        // Lazy Sized: https://www.webjars.org/
        private const val lazysizes_version = "5.1.2"
        const val lazysizes = "org.webjars.bower:lazysizes:$lazysizes_version"

        // VisJS: https://www.webjars.org/
        private const val visjs_version = "4.21.0"
        const val visjs = "org.webjars:visjs:$visjs_version"

        // https://mvnrepository.com/artifact/org.webjars.bower/slick-carousel
        private const val slick_carousel_version = "1.8.1"
        const val slick_carousel = "org.webjars.bower:slick-carousel:$slick_carousel_version"

        // https://mvnrepository.com/artifact/org.webjars.npm/glidejs__glide
        private const val glidejs_version = "3.4.1"
        const val glidejs = "org.webjars.npm:glidejs__glide:$glidejs_version"

        // https://mvnrepository.com/artifact/org.webjars.npm/prismjs
        private const val prismjs_version = "1.29.0"
        const val prismjs = "org.webjars.npm:prismjs:$prismjs_version"

        // https://www.webjars.org/
        private const val fomanticui_version = "2.8.8"
        const val fomanticui_css = "org.webjars.npm:fomantic-ui-css:$fomanticui_version"
        const val fomanticui_js = "org.webjars.npm:fomantic-ui-js:$fomanticui_version"
    }

    object IDE {
        // https://mvnrepository.com/artifact/org.jetbrains/annotations
        const val jetbrains_annotations_version = "26.0.2"
        const val jetbrains_annotations = "org.jetbrains:annotations:$jetbrains_annotations_version"
    }

    // // NPM dependencies /////////////////////////////////////////////////////////////////////////

    object Npm {
        operator fun <T> invoke(block: Npm.() -> T): T {
            return this.block()
        }

        // https://www.npmjs.com/package/@js-joda/timezone
        fun KotlinDependencyHandler.jsJodaTimezone() = npm("@js-joda/timezone", "2.22.0")

        // https://www.npmjs.com/package/bowser
        fun KotlinDependencyHandler.bowser() = npm("bowser", "2.12.1")

        // https://www.npmjs.com/package/chart.js
        fun KotlinDependencyHandler.chartJs() = npm("chart.js", "4.5.0")

        // TODO: check update to 4.x versions
        // https://www.npmjs.com/package/jwt-decode
        fun KotlinDependencyHandler.jwtDecode() = npm("jwt-decode", "3.1.2")

        // TODO: check update to 9.x versions
        // https://www.npmjs.com/package/konva
        fun KotlinDependencyHandler.konva() = npm("konva", "8.4.3")

        // https://www.npmjs.com/package/minidenticons
        fun KotlinDependencyHandler.minidenticons() = npm("minidenticons", "4.2.1")

        // https://www.npmjs.com/package/marked
        fun KotlinDependencyHandler.marked() = npm("marked", "15.0.12")

        // https://www.npmjs.com/package/dompurify
        fun KotlinDependencyHandler.domPurify() = npm("dompurify", "3.2.7")

        // https://www.npmjs.com/package/@nx-js/compiler-util
        fun KotlinDependencyHandler.nxJsCompilerUtil() = npm("@nx-js/compiler-util", "2.0.0")

        // https://www.npmjs.com/package/pdfjs-dist
        // See [PdfJs.kt] as this lib is loaded asynchronously
        @Suppress("UnusedReceiverParameter", "unused")
        fun KotlinDependencyHandler.pdfJs() = "4.10.38"

        // https://www.npmjs.com/package/preact
        fun KotlinDependencyHandler.preact() = npm("preact", "10.26.4")

        // https://www.npmjs.com/package/prismjs
        fun KotlinDependencyHandler.prismjs() = npm("prismjs", "1.30.0")

        // https://www.npmjs.com/package/signature_pad
        fun KotlinDependencyHandler.signaturepad() = npm("signature_pad", "5.1.1")

        // https://www.npmjs.com/package/sourcemapped-stacktrace
        fun KotlinDependencyHandler.sourcemappedStacktrace() = npm("sourcemapped-stacktrace", "1.1.11")

        // https://www.npmjs.com/package/trim-canvas
        fun KotlinDependencyHandler.trimCanvas() = npm("trim-canvas", "0.1.2")
    }

    // // Test dependencies ////////////////////////////////////////////////////////////////////////

    object Test {

        operator fun invoke(block: Test.() -> Unit) {
            this.block()
        }

        // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
        const val logback_version = "1.5.19"
        const val logback_classic = "ch.qos.logback:logback-classic:$logback_version"

        // https://plugins.gradle.org/plugin/io.kotest
        const val kotest_plugin_version = "6.0.7"
//        const val kotest_plugin_version = "5.9.1"

        // https://mvnrepository.com/artifact/io.kotest/kotest-common
        const val kotest_version = "6.0.7"
//        const val kotest_version = "5.9.1"

        const val kotest_framework_engine = "io.kotest:kotest-framework-engine:$kotest_version"
        const val kotest_assertions_core = "io.kotest:kotest-assertions-core:$kotest_version"
        const val kotest_runner_junit_jvm = "io.kotest:kotest-runner-junit5-jvm:$kotest_version"

        fun KotlinDependencyHandler.commonTestDeps() {
            kotlin("test-common")
            kotlin("test-annotations-common")
            implementation(kotest_assertions_core)
            implementation(kotest_framework_engine)
        }

        fun KotlinDependencyHandler.jsTestDeps() {
            implementation(kotest_assertions_core)
            implementation(kotest_framework_engine)
        }

        fun KotlinDependencyHandler.jvmTestDeps() {
            implementation(logback_classic)
            implementation(kotest_runner_junit_jvm)
            implementation(kotest_assertions_core)
            implementation(kotest_framework_engine)
        }

        fun DependencyHandlerScope.jvmTestDeps() {
            testImplementation(logback_classic)
            testImplementation(kotest_runner_junit_jvm)
            testImplementation(kotest_assertions_core)
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

    fun Project.createVersionFile(
        vararg outputDirs: String = arrayOf(
            "./src/main/resources",
            "./tmp"
        ),
    ) {
        tasks.register("versionFile") {
            doLast {
                val rootProject = project.rootProject
                val projectDir = project.projectDir

                val git = Grgit.open {
                    dir = rootProject.projectDir
                }

                File(projectDir, "tmp").mkdirs()

                val now = LocalDateTime.now()
                val nowStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

                val content = """{
                "project": "${project.path.removePrefix(":").replace(":", "-")}",
                "version": "${project.version}",
                "gitBranch": "${git.branch.current().name}",
                "gitRev": "${git.head().id.take(8)}",
                "gitDesc": "${git.describe { tags = true }}",
                "date": "$nowStr"
            }""".trimIndent()

                outputDirs.forEach {
                    val dir = File(projectDir, it)
                    dir.mkdirs()
                    File(dir, "version.json").writeText(content)
                }
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
