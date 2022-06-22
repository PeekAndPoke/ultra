package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.slumber.AwakerException
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
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
})
