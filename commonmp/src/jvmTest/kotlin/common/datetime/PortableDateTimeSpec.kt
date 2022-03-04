package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PortableDateTimeSpec : StringSpec({

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

    "Genesis" {
        assertSoftly {
            PortableDateTime(0).isGenesis() shouldBe false
            PortableDateTime(GENESIS_TIMESTAMP + 1).isGenesis() shouldBe false

            PortableDateTime(GENESIS_TIMESTAMP).isGenesis() shouldBe true
            PortableDateTime(GENESIS_TIMESTAMP - 1).isGenesis() shouldBe true
        }
    }

    "Doomsday" {
        assertSoftly {
            PortableDateTime(0).isDoomsday() shouldBe false
            PortableDateTime(DOOMSDAY_TIMESTAMP - 1).isDoomsday() shouldBe false

            PortableDateTime(DOOMSDAY_TIMESTAMP).isDoomsday() shouldBe true
            PortableDateTime(DOOMSDAY_TIMESTAMP + 1).isDoomsday() shouldBe true
        }
    }
})
