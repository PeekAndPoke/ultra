@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.kotest")
    id("com.google.devtools.ksp")
    id("com.vanniktech.maven.publish")
}

val FUNKTOR_GROUP: String by project
val VERSION_NAME: String by project

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
                // add all funktor sub-projects
                api(project(":funktor:auth"))
                api(project(":funktor:cluster"))
                api(project(":funktor:core"))
                api(project(":funktor:inspect"))
                api(project(":funktor:insights"))
                api(project(":funktor:logging"))
                api(project(":funktor:messaging"))
                api(project(":funktor:rest"))
                api(project(":funktor:testing"))
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
                api(Deps.JavaLibs.logback_classic)

                api(project(":funktor:staticweb"))
            }
        }

        jvmTest {
            dependencies {
                Deps.Test {
                    jvmTestDeps()
                }
                implementation(project(":karango:core"))
                implementation(project(":monko:core"))
            }
        }
    }
}

dependencies {
    add("kspJvmTest", project(":karango:ksp"))
    add("kspJvmTest", project(":monko:ksp"))
}

tasks {
    configureJvmTests()
}

mavenPublishing {
}
