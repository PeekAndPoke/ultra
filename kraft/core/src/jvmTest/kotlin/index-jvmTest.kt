package de.peekandpoke.kraft

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class JvmRootSpec : StringSpec({
    "Jvm OK" {
        1 shouldBe 1
    }
})
