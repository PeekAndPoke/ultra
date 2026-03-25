package de.peekandpoke.ultra.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ObjectKeySpec : StringSpec({

    "encode combines type and id" {
        ObjectKey(id = "123", type = "user").encode() shouldBe "user::123"
    }

    "encode with empty values" {
        ObjectKey.empty.encode() shouldBe "::"
    }

    "empty has blank id and type" {
        ObjectKey.empty.id shouldBe ""
        ObjectKey.empty.type shouldBe ""
    }
})
