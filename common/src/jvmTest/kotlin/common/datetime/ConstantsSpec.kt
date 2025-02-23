package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ConstantsSpec : FreeSpec() {

    init {

        "Genesis and Doomsday Constants" {

            assertSoftly {
                GenesisInstant.toString() shouldBe "-10000-01-01T00:00:00Z"
                GenesisInstant.toEpochMilli() shouldBe GENESIS_TIMESTAMP

                DoomsdayInstant.toString() shouldBe "+10000-01-01T00:00:00Z"
                DoomsdayInstant.toEpochMilli() shouldBe DOOMSDAY_TIMESTAMP

                GenesisLocalDate.toString() shouldBe "-10000-01-01"

                DoomsdayLocalDate.toString() shouldBe "+10000-01-01"

                GenesisLocalDateTime.toString() shouldBe "-10000-01-01T00:00"
                GenesisLocalDateTime.toInstant(utc).toEpochMilli() shouldBe GENESIS_TIMESTAMP

                DoomsdayLocalDateTime.toString() shouldBe "+10000-01-01T00:00"
                DoomsdayLocalDateTime.toInstant(utc).toEpochMilli() shouldBe DOOMSDAY_TIMESTAMP
            }
        }
    }
}
