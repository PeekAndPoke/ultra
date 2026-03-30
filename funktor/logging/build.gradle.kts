@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version Deps.Ksp.version
    id("io.kotest")
    id("com.vanniktech.maven.publish")
}

val FUNKTOR_GROUP: String by project
val VERSION_NAME: String by project

group = FUNKTOR_GROUP
version = VERSION_NAME

Docs {
    useEmptyJavadoc()
}

kotlin {
    js {
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
                api(project(":funktor:core"))
                api(project(":funktor:rest"))
                api(project(":funktor:inspect"))
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
                implementation(Deps.JavaLibs.logback_classic)

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
