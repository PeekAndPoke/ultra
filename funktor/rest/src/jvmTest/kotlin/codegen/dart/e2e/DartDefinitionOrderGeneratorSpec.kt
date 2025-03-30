package de.peekandpoke.ktorfx.rest.codegen.dart.e2e

import de.peekandpoke.ktorfx.rest.codegen.dart.addClass
import de.peekandpoke.ktorfx.rest.codegen.dart.extends
import de.peekandpoke.ktorfx.rest.codegen.dartProject
import de.peekandpoke.ktorfx.rest.codegen.shouldHaveNoDiffs
import de.peekandpoke.ktorfx.rest.codegen.tagged
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec

class DartDefinitionOrderGeneratorSpec : StringSpec(
    {

        "Class extending another class (defined earlier in the same file)" {

            val project = dartProject("test") {

                file("examples.dart") {
                    addClass("MyBaseClass", tagged("MyBaseClass")) {}

                    addClass("MyClass") {
                        extends(useClass("MyBaseClass"))
                    }
                }
            }

            val printed = project.printFiles()

            assertSoftly {
                printed[0].content shouldHaveNoDiffs """
                class MyBaseClass {
                }

                class MyClass extends MyBaseClass {
                }
            """.trimIndent()
            }
        }

        "Class extending another class (defined later in the same file)" {

            val project = dartProject("test") {

                file("examples.dart") {
                    addClass("MyClass") {
                        extends(useClass("MyBaseClass"))
                    }

                    addClass("MyBaseClass", tagged("MyBaseClass")) {}
                }
            }

            val printed = project.printFiles()

            assertSoftly {
                printed[0].content shouldHaveNoDiffs """
                class MyClass extends MyBaseClass {
                }

                class MyBaseClass {
                }
            """.trimIndent()
            }
        }

        "Class extending another class (defined earlier in another file)" {

            val project = dartProject("test") {

                file("base.dart") {
                    addClass("MyBaseClass", tagged("MyBaseClass")) {}
                }

                file("extended.dart") {
                    addClass("MyClass") {
                        extends(useClass("MyBaseClass"))
                    }
                }
            }

            val printed = project.printFiles()

            assertSoftly {
                printed[0].content shouldHaveNoDiffs """
                class MyBaseClass {
                }
            """.trimIndent()

                printed[1].content shouldHaveNoDiffs """
                import 'package:test/base.dart';
                
                class MyClass extends MyBaseClass {
                }
            """.trimIndent()
            }
        }

        "Class extending another class (defined later in another file)" {

            val project = dartProject("test") {

                file("extended.dart") {
                    addClass("MyClass") {
                        extends(useClass("MyBaseClass"))
                    }
                }

                file("base.dart") {
                    addClass("MyBaseClass", tagged("MyBaseClass")) {}
                }
            }

            val printed = project.printFiles()

            assertSoftly {

                printed[0].content shouldHaveNoDiffs """
                import 'package:test/base.dart';
                
                class MyClass extends MyBaseClass {
                }
            """.trimIndent()

                printed[1].content shouldHaveNoDiffs """
                class MyBaseClass {
                }
            """.trimIndent()
            }
        }
    },
)
