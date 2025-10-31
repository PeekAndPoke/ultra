package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.slumber
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DefaultCodecSpec : StringSpec({

    "Slumbering a Map<String, Any>" {

        val codec = Codec.default

        val data = mapOf("a" to 1, "b" to 2.5)

        codec.slumber(data) shouldBe mapOf("a" to 1, "b" to 2.5)
    }
})
