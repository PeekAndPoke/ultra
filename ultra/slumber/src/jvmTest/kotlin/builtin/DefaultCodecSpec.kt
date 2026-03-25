package io.peekandpoke.ultra.slumber.builtin

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.slumber

class DefaultCodecSpec : StringSpec({

    "Slumbering a Map<String, Any>" {

        val codec = Codec.default

        val data = mapOf("a" to 1, "b" to 2.5)

        codec.slumber(data) shouldBe mapOf("a" to 1, "b" to 2.5)
    }
})
