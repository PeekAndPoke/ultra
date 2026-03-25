package io.peekandpoke.funktor.rest.codegen.dart.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.peekandpoke.funktor.rest.codegen.dart.DartInt
import io.peekandpoke.funktor.rest.codegen.dart.DartString
import io.peekandpoke.funktor.rest.codegen.dart.addClass
import io.peekandpoke.funktor.rest.codegen.dart.addDefaultConstructor
import io.peekandpoke.funktor.rest.codegen.dart.addProperty
import io.peekandpoke.funktor.rest.codegen.dartProject
import io.peekandpoke.funktor.rest.codegen.shouldHaveNoDiffs

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
