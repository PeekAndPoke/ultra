package de.peekandpoke.ultra.common

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime

class TypedKeySpec : StringSpec({

    "Getting the name of a TypedKey" {

        val key = TypedKey<LocalDateTime>("data")

        assertSoftly {
            key.name shouldBe "data"
            key.toString() shouldBe "data"
        }
    }

    "Two TypedKeys with the same name must not be equal" {

        TypedKey<LocalDateTime>("data") shouldNotBe TypedKey<LocalDateTime>("data")
    }
})
