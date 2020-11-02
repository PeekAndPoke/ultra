package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PortableDateSpec : StringSpec({

    "Comparison" {

        assertSoftly {
            (PortableDate(1000) < PortableDate(1001)) shouldBe true
            (PortableDate(1000) == PortableDate(1001)) shouldBe false
            (PortableDate(1000) > PortableDate(1001)) shouldBe false

            (PortableDate(1001) < PortableDate(1000)) shouldBe false
            (PortableDate(1001) == PortableDate(1000)) shouldBe false
            (PortableDate(1001) > PortableDate(1000)) shouldBe true

            (PortableDate(1000) < PortableDate(1000)) shouldBe false
            (PortableDate(1000) == PortableDate(1000)) shouldBe true
            (PortableDate(1000) > PortableDate(1000)) shouldBe false
        }
    }
})
