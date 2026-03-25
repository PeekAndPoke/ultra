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
                implementation(kotlin("reflect"))
                implementation(Deps.KotlinX.serialization_json)

                api(project(":ultra:common"))
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
                api(Deps.JavaLibs.classindex)

                api(project(":ultra:cache"))
                api(project(":ultra:datetime"))
                api(project(":ultra:reflection"))
            }
        }

        jvmTest {
            dependencies {
                implementation(project(":ultra:slumber-test-classes"))
                Deps.Test {
                    jvmTestDeps()
                }
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
