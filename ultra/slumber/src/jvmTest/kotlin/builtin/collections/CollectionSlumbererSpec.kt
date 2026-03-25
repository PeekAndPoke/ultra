package io.peekandpoke.ultra.slumber.builtin.collections

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.slumber

class CollectionSlumbererSpec : StringSpec({

    "Slumbering an emptyList must work" {

        val data = emptyList<Any>()

        val codec = Codec.default

        val result = codec.slumber(data)

        result shouldBe emptyList<Any>()
    }
})
