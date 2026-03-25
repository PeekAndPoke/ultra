package io.peekandpoke.karango.e2e.functions.stu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.UNSET
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.e2e.withDetailedClue

@Suppress("ClassName")
class E2E_Func_UNSET_Spec : StringSpec({

    "UNSET removes a single attribute" {

        val doc = mapOf("a" to 1, "b" to 2, "c" to 3).aql()

        val expression = UNSET<Map<String, Any?>>(doc, "b".aql())

        val result = karangoDriver.query {
            RETURN(expression)
        }

        withDetailedClue(expression, mapOf("a" to 1, "c" to 3)) {
            val list = result.toList()
            list.size shouldBe 1

            @Suppress("UNCHECKED_CAST")
            val unset = list[0] as Map<String, Any?>
            unset["a"] shouldBe 1
            unset.containsKey("b") shouldBe false
            unset["c"] shouldBe 3
        }
    }

    "UNSET removes multiple attributes" {

        val doc = mapOf("a" to 1, "b" to 2, "c" to 3).aql()

        val expression = UNSET<Map<String, Any?>>(doc, "a".aql(), "c".aql())

        val result = karangoDriver.query {
            RETURN(expression)
        }

        withDetailedClue(expression, mapOf("b" to 2)) {
            val list = result.toList()
            list.size shouldBe 1

            @Suppress("UNCHECKED_CAST")
            val unset = list[0] as Map<String, Any?>
            unset.containsKey("a") shouldBe false
            unset["b"] shouldBe 2
            unset.containsKey("c") shouldBe false
        }
    }
})
