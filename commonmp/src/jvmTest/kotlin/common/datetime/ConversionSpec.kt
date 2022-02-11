package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class ConversionSpec : FreeSpec() {

    init {
        "Conversion Round trips" - {

            "Instant" {
                val subject = Instant.now()

                subject shouldBe Instant.ofEpochMilli(subject.portable.timestamp)
            }

            "LocalDate" {

                val subject = LocalDate.now()

                subject shouldBe subject.portable.date
            }

            "LocalDate - Genesis & Doomsday" {

                GenesisLocalDate shouldBe GenesisLocalDate.portable.date

                DoomsdayLocalDate shouldBe DoomsdayLocalDate.portable.date
            }

            "LocalDateTime" {
                val subject = LocalDateTime.now()

                subject shouldBe subject.portable.date
            }

            "LocalDateTime - Genesis & Doomsday" {

                GenesisLocalDateTime shouldBe GenesisLocalDateTime.portable.date

                DoomsdayLocalDateTime shouldBe DoomsdayLocalDateTime.portable.date
            }
        }
    }
}
