package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PortableDateTimeTimeSpec : StringSpec({

    "Comparison" {

        assertSoftly {
            (PortableDateTime(1000) < PortableDateTime(1001)) shouldBe true
            (PortableDateTime(1000) == PortableDateTime(1001)) shouldBe false
            (PortableDateTime(1000) > PortableDateTime(1001)) shouldBe false

            (PortableDateTime(1001) < PortableDateTime(1000)) shouldBe false
            (PortableDateTime(1001) == PortableDateTime(1000)) shouldBe false
            (PortableDateTime(1001) > PortableDateTime(1000)) shouldBe true

            (PortableDateTime(1000) < PortableDateTime(1000)) shouldBe false
            (PortableDateTime(1000) == PortableDateTime(1000)) shouldBe true
            (PortableDateTime(1000) > PortableDateTime(1000)) shouldBe false
        }
    }
})
