@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version Deps.Ksp.version
    id("io.kotest")
    id("com.vanniktech.maven.publish")
}

val FUNKTOR_GROUP = providers.gradleProperty("FUNKTOR_GROUP").get()
val VERSION_NAME = providers.gradleProperty("VERSION_NAME").get()

group = FUNKTOR_GROUP
version = VERSION_NAME

kotlin {
    js {
        compilerOptions {
            target.set("es2015")
        }

        browser {
            testTask {
            }
        }
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":funktor:core"))
                // `api`: on the JVM ultra:common is this module's surface anyway (through ultra:depot, whose
                // facade takes a Lookup); funktor:core is `implementation` here, so it does not bring it.
                api(project(":ultra:common"))
                implementation(project(":funktor:rest"))
                implementation(project(":funktor:inspect"))
            }
        }

        commonTest {
            dependencies {
                Deps.Test {
                    commonTestDeps()
                }
            }
        }

        jsMain {
        }

        jsTest {
            dependencies {
                Deps.Test {
                    jsTestDeps()
                }
            }
        }

        jvmMain {
            dependencies {
                // `api`: the depot's repositories and facade are part of this module's surface
                api(project(":ultra:depot"))
                // `api`: a stored entity names the data subject of a depot write (`Storable<T>.asDepotSubject()`)
                api(project(":ultra:vault"))

                implementation(project(":karango:core"))
                implementation(project(":monko:core"))
            }
        }

        jvmTest {
            dependencies {
                Deps.Test {
                    jvmTestDeps()
                }
            }
        }
    }
}

dependencies {
    add("kspJvm", project(":karango:ksp"))
    add("kspJvmTest", project(":karango:ksp"))
    add("kspJvm", project(":monko:ksp"))
    add("kspJvmTest", project(":monko:ksp"))
}

tasks {
    configureJvmTests()
}

mavenPublishing {
}
