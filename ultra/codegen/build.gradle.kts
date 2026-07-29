@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

val ULTRA_GROUP: String by project
val VERSION_NAME: String by project

group = ULTRA_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

dependencies {
    implementation(kotlin("reflect"))

    api(project(":ultra:common"))
    // Brings ":ultra:reflection" and ":ultra:datetime" in transitively (both declared `api` there).
    // Needed for the type walker (ReifiedKType), polymorphism introspection and the SlumberConfig
    // custom-codec check that guards against silently mis-typed output.
    api(project(":ultra:slumber"))
    // For KotlinxJsonTsContributor: Slumber has codecs for JsonElement & friends, so the generator
    // must be able to name those types. Slumber declares this `implementation`, hence not transitive.
    implementation(Deps.KotlinX.serialization_json)

    // Tests /////////////////////////
    Deps.Test {
        jvmTestDeps()
    }

    // Golden-file comparison of emitted code
    testImplementation(Deps.JavaLibs.diffutils)
    // @SerialName on polymorphic test fixtures — Slumber reads it as a child identifier
    testImplementation(Deps.KotlinX.serialization_core)
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)
}

tasks {
    configureJvmTests()
}

//  TypeScript verification  ///////////////////////////////////////////////////////////////////////
//
//  Runs the EMITTED TypeScript through the real toolchain: `tsc --noEmit` proves it type-checks, and
//  a node harness proves each schema accepts the JSON Slumber actually writes while rejecting
//  malformed input. Kotlin tests cannot establish either — they can only check the generator against
//  its own assumptions.
//
//  `ts-verify/` is self-contained by design: its own package.json, lockfile, pinned pnpm (via the
//  packageManager field) and pinned Node (via .npmrc use-node-version). It shares nothing with
//  docs-site or the Kotlin/JS build, so it only breaks when this generator breaks.
//
//  Opt out with -PskipTsVerify=true. It fails loudly rather than skipping silently when the toolchain
//  is missing — a verification that quietly does nothing is worse than none.

val tsVerifyDir = layout.projectDirectory.dir("ts-verify")
val tsGeneratedDir = layout.buildDirectory.dir("ts-verify-generated")

val generateTsFixtures by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Emits TypeScript fixtures plus real Slumber output for the ts-verify harness"

    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("io.peekandpoke.ultra.codegen.ts.TsFixtureGeneratorKt")
    argumentProviders.add { listOf(tsGeneratedDir.get().asFile.absolutePath) }

    inputs.files(sourceSets["main"].output, sourceSets["test"].output)
    outputs.dir(tsGeneratedDir)
}

val tsVerifyInstall by tasks.registering(Exec::class) {
    group = "verification"
    description = "Installs the pinned TypeScript verification toolchain"

    workingDir = tsVerifyDir.asFile

    // --frozen-lockfile: fail rather than silently resolving different versions. Without it a stale
    // lockfile would be "fixed" on the fly and the gate would stop being reproducible.
    commandLine("pnpm", "install", "--frozen-lockfile")

    inputs.file(tsVerifyDir.file("package.json"))
    inputs.file(tsVerifyDir.file("pnpm-lock.yaml"))
    inputs.file(tsVerifyDir.file(".npmrc"))
    outputs.dir(tsVerifyDir.dir("node_modules"))
}

val tsVerify by tasks.registering(Exec::class) {
    group = "verification"
    description = "Type-checks and executes the generated TypeScript (tsc + zod via pnpm)"

    dependsOn(generateTsFixtures, tsVerifyInstall)

    workingDir = tsVerifyDir.asFile

    // Plain pnpm, NOT corepack: pnpm enforces the `packageManager` field itself via
    // manage-package-manager-versions in .npmrc. See the note there for why corepack is avoided.
    commandLine("pnpm", "run", "--silent", "verify")

    inputs.dir(tsGeneratedDir)
    inputs.file(tsVerifyDir.file("package.json"))
    inputs.file(tsVerifyDir.file("pnpm-lock.yaml"))
    inputs.file(tsVerifyDir.file("tsconfig.json"))
    inputs.file(tsVerifyDir.file("verify.ts"))
    outputs.upToDateWhen { false }

    doFirst {
        // The generated sources live under build/, so link them in where tsconfig expects them.
        val linked = tsVerifyDir.dir("generated").asFile
        linked.deleteRecursively()
        tsGeneratedDir.get().asFile.copyRecursively(linked, overwrite = true)
    }
}

tasks.named("check") {
    if (!providers.gradleProperty("skipTsVerify").isPresent) {
        dependsOn(tsVerify)
    }
}

mavenPublishing {
}
