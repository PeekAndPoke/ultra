package io.peekandpoke.ultra.slumber.builtin.objects

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.slumber.Codec

class NullCodecSpec : StringSpec({

    val codec = Codec.default

    "Awake Nothing type returns null" {
        val result = codec.awake(kType<Nothing?>().type, "anything")

        result shouldBe null
    }

    "Awake Unit type returns null" {
        val result = codec.awake(kType<Unit?>().type, "anything")

        result shouldBe null
    }

    "Slumber Nothing type returns null" {
        val result = codec.slumber(kType<Nothing?>().type, null)

        result shouldBe null
    }
})
