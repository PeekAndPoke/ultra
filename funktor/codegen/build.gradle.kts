@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    // Test fixtures only: a `TypedApiEndpoint` needs a `KSerializer` for its response, so realistic
    // route fixtures need @Serializable response models.
    kotlin("plugin.serialization")
    id("com.vanniktech.maven.publish")
}

val FUNKTOR_GROUP = providers.gradleProperty("FUNKTOR_GROUP").get()
val VERSION_NAME = providers.gradleProperty("VERSION_NAME").get()

group = FUNKTOR_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

// A SEPARATE module from `funktor:rest`, deliberately. Code generation is a dev-time concern and
// `funktor:rest` is a runtime dependency of every production server — bundling the generator there is
// the mistake Phase 0 of the plan undid for the Dart generator.
//
// Plain `kotlin("jvm")` rather than multiplatform: this reads `ApiFeature` route graphs through JVM
// reflection and writes files. There is nothing here a JS target could run.
dependencies {
    implementation(kotlin("reflect"))

    api(project(":ultra:codegen"))
    // For ApiFeature / ApiRoute / CodeGenHints. Brings ":funktor:core" (TypedRoute, UriPattern)
    // transitively, since funktor:rest declares it `api`.
    api(project(":funktor:rest"))

    // Tests /////////////////////////
    Deps.Test {
        jvmTestDeps()
    }

    // TEST ONLY, deliberately: main code matches insights by NAME (see InsightsTsContributor) so the
    // generator never depends on it. The specs and the vue-verify harness need the REAL feature, whose
    // models the pages import; the fixtures only borrow its name.
    testImplementation(project(":funktor:insights"))
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)
}

tasks {
    configureJvmTests()
}

//  Vue verification  //////////////////////////////////////////////////////////////////////////////////////
//
//  Compiles, bundles and component-tests the Vue resources the way an app consumes them: an SDK generated
//  from the REAL insights feature, then `vue-tsc`, `vite build` and vitest + happy-dom over it. A Kotlin test
//  cannot see a broken `.vue`, and `vue-tsc` alone cannot see a missing one (`shims-vue.d.ts`).
//
//  `vue-verify/` follows `ultra/codegen/ts-verify/`: its own package.json, lockfile, pinned pnpm (the
//  packageManager field) and pinned Node (.npmrc use-node-version), installed with --frozen-lockfile.
//
//  Opt out with -PskipVueVerify=true. It fails loudly when the toolchain is missing.

val vueVerifyDir = layout.projectDirectory.dir("vue-verify")
val vueVerifySdkDir = layout.buildDirectory.dir("vue-verify-sdk")

val generateVueVerifySdk = tasks.register<JavaExec>("generateVueVerifySdk") {
    group = "verification"
    description = "Generates the SDK of an app with the real insights feature, for the vue-verify harness"

    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("io.peekandpoke.funktor.codegen.RealInsightsSdkKt")
    argumentProviders.add { listOf(vueVerifySdkDir.get().asFile.absolutePath) }

    inputs.files(sourceSets["main"].output, sourceSets["test"].output)
    outputs.dir(vueVerifySdkDir)
}

val vueVerifyInstall = tasks.register<Exec>("vueVerifyInstall") {
    group = "verification"
    description = "Installs the pinned Vue verification toolchain"

    workingDir = vueVerifyDir.asFile

    // --frozen-lockfile: fail rather than silently resolving different versions.
    commandLine("pnpm", "install", "--frozen-lockfile")

    inputs.file(vueVerifyDir.file("package.json"))
    inputs.file(vueVerifyDir.file("pnpm-lock.yaml"))
    inputs.file(vueVerifyDir.file(".npmrc"))
    outputs.dir(vueVerifyDir.dir("node_modules"))
}

val vueVerify = tasks.register<Exec>("vueVerify") {
    group = "verification"
    description = "Type-checks, bundles and component-tests the insights Vue pages (vue-tsc, vite, vitest)"

    dependsOn(generateVueVerifySdk, vueVerifyInstall)

    workingDir = vueVerifyDir.asFile

    commandLine("pnpm", "run", "--silent", "verify")

    inputs.dir(vueVerifySdkDir)
    outputs.upToDateWhen { false }

    doFirst {
        // The SDK is generated under build/; copy it where the harness app imports it from.
        val linked = vueVerifyDir.dir("src/funktorsdk").asFile
        linked.deleteRecursively()
        vueVerifySdkDir.get().asFile.copyRecursively(linked, overwrite = true)
    }
}

tasks.named("check") {
    if (!providers.gradleProperty("skipVueVerify").isPresent) {
        dependsOn(vueVerify)
    }
}

mavenPublishing {
}
