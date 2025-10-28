package de.peekandpoke.karango.e2e

import de.peekandpoke.karango.aql.ARRAY
import de.peekandpoke.karango.aql.OBJECT
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kMapType
import de.peekandpoke.ultra.common.reflection.kType
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-ReturnDirectly-Spec` : StringSpec() {

    init {
        "Directly returning a String" {

            val result = karangoDriver.query {
                RETURN(
                    "string".aql("ret")
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN @ret_1
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("ret_1" to "string")
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf("string")
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe TypeRef.String
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe TypeRef.String.list
                }
            }
        }

        "Directly returning a List<String>" {

            val result = karangoDriver.query {
                RETURN(
                    listOf("s1", "s2").aql()
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN @v_1
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to listOf("s1", "s2"))
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(listOf("s1", "s2"))
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe TypeRef.String.list
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe TypeRef.String.list.list
                }
            }
        }

        "Directly returning a List<Integer> created with ARRAY()" {

            val result = karangoDriver.query {
                RETURN(
                    ARRAY(1.aql, 2.aql)
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN [@v_1, @v_2]
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to 1, "v_2" to 2)
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(listOf(1, 2))
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe TypeRef.Int.list
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe TypeRef.Int.list.list
                }
            }
        }

        "Directly returning a Map<String, Integer> created with OBJECT()" {

            val result = karangoDriver.query {
                RETURN(
                    OBJECT("a".aql to 1.aql, "b".aql to 2.aql)
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN {@v_1: (@v_2), @v_3: (@v_4)}
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to "a", "v_2" to 1, "v_3" to "b", "v_4" to 2)
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(mapOf("a" to 1, "b" to 2))
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe kMapType<String, Int>()
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe kMapType<String, Int>().list
                }
            }
        }

        "Directly returning a Map<String, Integer> created with OBJECT() with String keys" {

            val result = karangoDriver.query {
                RETURN(
                    OBJECT("a" to 1.aql, "b" to 2.aql)
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN {@v_1: (@v_2), @v_3: (@v_4)}
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to "a", "v_2" to 1, "v_3" to "b", "v_4" to 2)
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(mapOf("a" to 1, "b" to 2))
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe kMapType<String, Int>()
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe kMapType<String, Int>().list
                }
            }
        }

        "Directly returning a Double" {

            val result = karangoDriver.query {
                RETURN(
                    12.34.aql()
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN @v_1
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to 12.34)
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(12.34)
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe TypeRef.Double
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe TypeRef.Double.list
                }
            }
        }

        "Directly returning an Integer" {

            val result = karangoDriver.query {
                RETURN(
                    1234.aql()
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN @v_1
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to 1234)
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(1234)
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe TypeRef.Int
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe TypeRef.Int.list
                }
            }
        }

        "Directly returning a Long" {

            val result = karangoDriver.query {
                RETURN(
                    1234L.aql()
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN @v_1
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to 1234L)
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(1234L)
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe TypeRef.Long
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe TypeRef.Long.list
                }
            }
        }

        "Directly returning a List<Double>" {

            val result = karangoDriver.query {
                RETURN(
                    listOf(1.0, 2.5).aql()
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN @v_1
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to listOf(1.0, 2.5))
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(listOf(1.0, 2.5))
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe TypeRef.Double.list
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe TypeRef.Double.list.list
                }
            }
        }

        "Directly returning a List of mixed numeric values" {

            val result = karangoDriver.query {
                RETURN(
                    listOf(0L, 1f, 2.5).aql()
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN @v_1
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to listOf(0L, 1f, 2.5))
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(listOf(0L, 1.0, 2.5))
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe TypeRef.Any.list
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe TypeRef.Any.list.list
                }
            }
        }

        "Directly returning a List of mixed numeric values declared as List<Number>" {

            val result = karangoDriver.query {
                RETURN(
                    listOf<Number>(0L, 1f, 2.5).aql()
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN @v_1
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to listOf(0L, 1f, 2.5))
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(listOf(0L, 1.0, 2.5))
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe TypeRef.Number.list
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe TypeRef.Number.list.list
                }
            }
        }

        "Directly returning a domain object" {

            val data = E2ePerson("Eddard", 42)

            val result = karangoDriver.query {
                RETURN(
                    data.aql()
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN @v_1
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to data)
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(data)
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType().type.classifier shouldBe E2ePerson::class
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType().type.classifier shouldBe List::class
                    result.query.root.getType().type.arguments[0].type!!.classifier shouldBe E2ePerson::class
                }
            }
        }

        "Directly returning a list of domain objects" {

            val data = listOf(
                E2ePerson("Eddard", 42),
                E2ePerson("John", 22)
            )

            val result = karangoDriver.query {
                RETURN(
                    data.aql()
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN @v_1
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to data)
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(data)
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe kType<E2ePerson>().list
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe kType<E2ePerson>().list.list
                }
            }
        }

        "Directly returning a map of strings to domain objects" {

            val data = mapOf(
                "a" to E2ePerson("Eddard", 42),
                "b" to E2ePerson("John", 22)
            )

            val result = karangoDriver.query {
                RETURN(
                    data.aql()
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN @v_1
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to data)
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(data)
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe kMapType<String, E2ePerson>()
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe kMapType<String, E2ePerson>().list
                }
            }
        }

        "Directly returning a map of strings to lists of domain objects" {

            val data = mapOf(
                "a" to listOf(E2ePerson("Eddard", 42), E2ePerson("John", 22)),
                "b" to listOf()
            )

            val result = karangoDriver.query {
                RETURN(
                    data.aql()
                )
            }

            assertSoftly {

                withClue("AQL-Query") {
                    result.query.query shouldBe """
                    |RETURN @v_1
                    |
                """.trimMargin()
                }

                withClue("AQL-vars") {
                    result.query.vars shouldBe mapOf("v_1" to data)
                }

                withClue("Query-result") {
                    result.toList() shouldBe listOf(data)
                }

                withClue("TypeRef for deserialization") {
                    result.query.root.innerType() shouldBe kMapType<String, List<E2ePerson>>()
                }

                withClue("TypeRef of TerminalExpr") {
                    result.query.root.getType() shouldBe kMapType<String, List<E2ePerson>>().list
                }
            }
        }
    }
}
