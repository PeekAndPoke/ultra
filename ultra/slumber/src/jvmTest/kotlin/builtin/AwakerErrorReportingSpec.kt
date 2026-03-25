package io.peekandpoke.ultra.slumber.builtin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.slumber.AwakerException
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import kotlin.reflect.typeOf

class AwakerErrorReportingSpec : StringSpec({

    "Missing parameters must be reported for simple data class" {

        data class Simple(
            val a: Int,
            val b: String,
        )

        val codec = Codec.default

        val ex = shouldThrow<AwakerException> {
            codec.awake(
                Simple::class,
                mapOf(
                    "a" to 100,
                )
            )
        }

        ex.message shouldContain "'root' must not be null"
        ex.message shouldContain "'root': ${typeOf<Simple>()} misses parameters 'b'"
    }

    "Wrong parameters must be reported for simple data class" {

        data class Simple(
            val a: Int,
            val b: String,
        )

        val codec = Codec.default

        val ex = shouldThrow<AwakerException> {
            codec.awake(
                Simple::class,
                mapOf(
                    "a" to "xyz",
                    "b" to "xyz",
                )
            )
        }

        ex.message shouldContain "'root.a' must not be null"
    }

    "Missing parameters must be reported for nested data class" {

        data class Child(
            val a: Int,
            val b: String,
        )

        data class Container(
            val a: Int,
            val child: Child,
        )

        val codec = Codec.default

        val ex = shouldThrow<AwakerException> {
            codec.awake(
                Container::class,
                mapOf(
                    "a" to 100,
                    "child" to mapOf(
                        "a" to 100,
                    )
                )
            )
        }

        ex.message shouldContain "'root.child' must not be null"
        ex.message shouldContain "'root.child': ${typeOf<Child>()} misses parameters 'b'"
    }

    "Error in collection element reports path with index" {

        data class Container(
            val items: List<Int>,
        )

        val codec = Codec.default

        val ex = shouldThrow<AwakerException> {
            codec.awake(
                Container::class,
                mapOf(
                    "items" to listOf(1, null, 3)
                )
            )
        }

        ex.message shouldContain "root.items.1"
        ex.message shouldContain "must not be null"
    }

    "Error in deeply nested structure (3+ levels) reports full path" {

        data class Level3(val value: Int)
        data class Level2(val level3: Level3)
        data class Level1(val level2: Level2)

        val codec = Codec.default

        val ex = shouldThrow<AwakerException> {
            codec.awake(
                Level1::class,
                mapOf(
                    "level2" to mapOf(
                        "level3" to mapOf(
                            "value" to "not_an_int"
                        )
                    )
                )
            )
        }

        ex.message shouldContain "root.level2.level3.value"
        ex.message shouldContain "must not be null"
    }
})
