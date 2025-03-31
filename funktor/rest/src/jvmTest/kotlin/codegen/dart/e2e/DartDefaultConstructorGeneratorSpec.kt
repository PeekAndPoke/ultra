package de.peekandpoke.funktor.rest.codegen.dart.e2e

import de.peekandpoke.funktor.rest.codegen.dart.DartInt
import de.peekandpoke.funktor.rest.codegen.dart.DartString
import de.peekandpoke.funktor.rest.codegen.dart.addClass
import de.peekandpoke.funktor.rest.codegen.dart.addDefaultConstructor
import de.peekandpoke.funktor.rest.codegen.dart.addProperty
import de.peekandpoke.funktor.rest.codegen.dartProject
import de.peekandpoke.funktor.rest.codegen.shouldHaveNoDiffs
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec

class DartDefaultConstructorGeneratorSpec : StringSpec(
    {
        "Default constructor for simple class" {

            val project = dartProject("test") {

                file("examples.dart") {
                    addClass("MyClass") {
                        addProperty(DartString, "pString")
                        addProperty(DartInt, "pInt")

                        addDefaultConstructor()
                    }
                }
            }

            val printed = project.printFiles()

            assertSoftly {
                printed[0].content shouldHaveNoDiffs """
                class MyClass {
                  String pString;
                  int pInt;
                  MyClass(
                    String pString,
                    int pInt
                  ) {
                    this.pString = pString;
                    this.pInt = pInt;
                  }
                }
            """.trimIndent()
            }
        }
    },
)
