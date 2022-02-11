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

    "Genesis" {
        assertSoftly {
            PortableDate(0).isGenesis() shouldBe false
            PortableDate(GENESIS_TIMESTAMP + 1).isGenesis() shouldBe false

            PortableDate(GENESIS_TIMESTAMP).isGenesis() shouldBe true
            PortableDate(GENESIS_TIMESTAMP - 1).isGenesis() shouldBe true
        }
    }

    "Doomsday" {
        assertSoftly {
            PortableDate(0).isDoomsday() shouldBe false
            PortableDate(DOOMSDAY_TIMESTAMP - 1).isDoomsday() shouldBe false

            PortableDate(DOOMSDAY_TIMESTAMP).isDoomsday() shouldBe true
            PortableDate(DOOMSDAY_TIMESTAMP + 1).isDoomsday() shouldBe true
        }
    }
})
