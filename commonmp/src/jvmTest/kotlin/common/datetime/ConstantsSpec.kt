package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ConstantsSpec : FreeSpec() {

    init {

        "Genesis and Doomsday Constants" {

            assertSoftly {
                GenesisInstant.toString() shouldBe "-10000-01-01T00:00:00Z"
                GenesisInstant.portable.timestamp shouldBe GENESIS_TIMESTAMP

                DoomsdayInstant.toString() shouldBe "+10000-01-01T00:00:00Z"
                DoomsdayInstant.portable.timestamp shouldBe DOOMSDAY_TIMESTAMP

                GenesisLocalDate.toString() shouldBe "-10000-01-01"
                GenesisLocalDate.portable.timestamp shouldBe GENESIS_TIMESTAMP

                DoomsdayLocalDate.toString() shouldBe "+10000-01-01"
                DoomsdayLocalDate.portable.timestamp shouldBe DOOMSDAY_TIMESTAMP

                GenesisLocalDateTime.toString() shouldBe "-10000-01-01T00:00"
                GenesisLocalDateTime.portable.timestamp shouldBe GENESIS_TIMESTAMP

                DoomsdayLocalDateTime.toString() shouldBe "+10000-01-01T00:00"
                DoomsdayLocalDateTime.portable.timestamp shouldBe DOOMSDAY_TIMESTAMP
            }
        }
    }
}
