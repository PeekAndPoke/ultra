package io.peekandpoke.karango.e2e.functions.mno

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.MERGE
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue

@Suppress("ClassName")
class E2E_Func_MERGE_Spec : StringSpec({

    "MERGE two documents" {

        val doc1 = mapOf("a" to 1).aql()
        val doc2 = mapOf("b" to 2).aql()

        val expression = MERGE<Map<String, Any?>>(doc1, doc2)

        val result = karangoDriver.query {
            RETURN(expression)
        }

        withDetailedClue(expression, mapOf("a" to 1, "b" to 2)) {
            val list = result.toList()
            list.size shouldBe 1

            @Suppress("UNCHECKED_CAST")
            val merged = list[0]
            merged["a"] shouldBe 1
            merged["b"] shouldBe 2
        }
    }

    "MERGE two documents via LET" {

        val doc1 = mapOf("x" to "hello").aql()
        val doc2 = mapOf("y" to "world").aql()

        val expression = MERGE<Map<String, Any?>>(doc1, doc2)

        val result = karangoDriver.query {
            val l = LET("l", expression)
            RETURN(l)
        }

        withDetailedClue(expression, mapOf("x" to "hello", "y" to "world")) {
            val list = result.toList()
            list.size shouldBe 1

            @Suppress("UNCHECKED_CAST")
            val merged = list[0]
            merged["x"] shouldBe "hello"
            merged["y"] shouldBe "world"
        }
    }

    "MERGE overwrites with later document" {

        val doc1 = mapOf("a" to 1, "b" to 2).aql()
        val doc2 = mapOf("b" to 99, "c" to 3).aql()

        val expression = MERGE<Map<String, Any?>>(doc1, doc2)

        val result = karangoDriver.query {
            RETURN(expression)
        }

        withDetailedClue(expression, mapOf("a" to 1, "b" to 99, "c" to 3)) {
            val list = result.toList()
            list.size shouldBe 1

            @Suppress("UNCHECKED_CAST")
            val merged = list[0]
            merged["a"] shouldBe 1
            merged["b"] shouldBe 99
            merged["c"] shouldBe 3
        }
    }
})
