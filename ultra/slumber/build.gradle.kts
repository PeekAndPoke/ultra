@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("multiplatform")
    kotlin("kapt")
    id("com.vanniktech.maven.publish")
}

val ULTRA_GROUP: String by project
val VERSION_NAME: String by project

group = ULTRA_GROUP
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
                implementation(kotlin("reflect"))
                implementation(Deps.KotlinX.serialization_json)

                api(project(":ultra:common"))
            }
        }

        commonTest {
            dependencies {
                Deps.Test { commonTestDeps() }
            }
        }

        jsMain {
        }

        jsTest {
        }

        jvmMain {
            dependencies {
                api(Deps.JavaLibs.classindex)

                implementation(Deps.KotlinX.datetime)
            }
        }

        jvmTest {
            dependencies {
                implementation(project(":ultra:slumber-test-classes"))
                Deps.Test { jvmTestDeps() }
            }
        }
    }
}

dependencies {
    add("kapt", Deps.JavaLibs.classindex)
}

tasks {
    configureJvmTests()
}

mavenPublishing {
}
