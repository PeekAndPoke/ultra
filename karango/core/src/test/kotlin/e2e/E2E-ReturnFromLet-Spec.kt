package de.peekandpoke.karango.e2e

import de.peekandpoke.karango.KarangoCursor
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kMapType
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-ReturnFromLet-Spec` : StringSpec({

    "Returning a String defined by LET" {

        val result = karangoDriver.query {
            val a = LET("a", "string")
            RETURN(a)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN `l_a`
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to "string")
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

    "Returning a List<String> defined by LET" {

        val result = karangoDriver.query {
            val a = LET("a") { listOf("s1", "s2") }
            RETURN(a)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN `l_a`
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to listOf("s1", "s2"))
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

    "Returning a Map<String, String> defined by LET" {

        val result: KarangoCursor<Map<String, String>> = karangoDriver.query {
            val a = LET("a") { mapOf("s1" to "s2") }
            RETURN(a)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN `l_a`
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to mapOf("s1" to "s2"))
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(mapOf("s1" to "s2"))
            }

            withClue("TypeRef for deserialization") {
                result.query.root.innerType() shouldBe kMapType<String, String>()
            }

            withClue("TypeRef of TerminalExpr") {
                result.query.root.getType() shouldBe kMapType<String, String>().list
            }
        }
    }

    "Returning a List<Map<String, Int>> defined by LET" {

        val input = listOf(
            mapOf("s1" to 1, "s2" to 2),
            mapOf("s3" to 3)
        )

        val result: KarangoCursor<List<Map<String, Int>>> = karangoDriver.query {
            val a = LET("a", input)
            RETURN(a)
        }

        assertSoftly {

            withClue("AQL-Query") {
                result.query.query shouldBe """
                    |LET `l_a` = (@l_a_1)
                    |RETURN `l_a`
                    |
                """.trimMargin()
            }

            withClue("AQL-vars") {
                result.query.vars shouldBe mapOf("l_a_1" to input)
            }

            withClue("Query-result") {
                result.toList() shouldBe listOf(input)
            }

            withClue("TypeRef for deserialization") {
                result.query.root.innerType() shouldBe kMapType<String, Int>().list
            }

            withClue("TypeRef of TerminalExpr") {
                result.query.root.getType() shouldBe kMapType<String, Int>().list.list
            }
        }
    }
})
