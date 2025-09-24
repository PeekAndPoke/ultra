@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version Deps.Ksp.version
    id("io.kotest.multiplatform")
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
        browser {}
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    sourceSets {

        commonMain {
            dependencies {
                implementation(project(":ultra:semanticui"))
                implementation(project(":kraft:core"))

                api(project(":funktor:core"))
                api(project(":funktor:rest"))
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
            dependencies {
            }
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
}

tasks {
    configureJvmTests()
}

mavenPublishing {

}
