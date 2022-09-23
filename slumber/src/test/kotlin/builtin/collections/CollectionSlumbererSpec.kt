package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CollectionSlumbererSpec : StringSpec({

    "Slumbering an emptyList must work" {

        val data = emptyList<Any>()

        val codec = Codec.default

        val result = codec.slumber(data)

        result shouldBe emptyList<Any>()
    }
})
