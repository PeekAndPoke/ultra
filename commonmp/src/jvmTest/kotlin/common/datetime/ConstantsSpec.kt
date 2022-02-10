package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ConstantsSpec : FreeSpec() {

    init {

        "Genesis and Doomsday Constants" {

            assertSoftly {
                GenesisInstant.toString() shouldBe "-10000-01-01T00:00:00Z"
                DoomsdayInstant.toString() shouldBe "+10000-01-01T00:00:00Z"

                GenesisLocalDate.toString() shouldBe "-10000-01-01"
                DoomsdayLocalDate.toString() shouldBe "+10000-01-01"

                GenesisLocalDateTime.toString() shouldBe "-10000-01-01T00:00"
                DoomsdayLocalDateTime.toString() shouldBe "+10000-01-01T00:00"
            }
        }
    }
}
