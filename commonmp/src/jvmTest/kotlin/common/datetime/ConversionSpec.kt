package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ConversionSpec : FreeSpec() {

    init {
        "Portable - Conversion Round trips" - {

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

        "Mp" - {

            "MpInstant.jvm" {
                val mp: MpInstant = MpInstant.parse("2022-04-05T12:13:14.123Z")
                val jvm: Instant = mp.jvm

                jvm shouldBe Instant.parse("2022-04-05T12:13:14.123Z")

                jvm.toEpochMilli() shouldBe mp.toEpochMillis()
            }

            "Instant.mp" {
                val jvm: Instant = Instant.parse("2022-04-05T12:13:14.123Z")
                val mp: MpInstant = jvm.mp

                mp shouldBe MpInstant.parse("2022-04-05T12:13:14.123Z")

                jvm.toEpochMilli() shouldBe mp.toEpochMillis()
            }

            "MpLocalDateTime.jvm" {
                val mp: MpLocalDateTime = MpLocalDateTime.of(
                    2022, Month.APRIL, 5, 12, 13, 14, 123_000_000
                )
                val jvm: LocalDateTime = mp.jvm

                jvm shouldBe LocalDateTime.of(
                    2022, Month.APRIL, 5, 12, 13, 14, 123_000_000
                )

                jvm.toEpochSecond(ZoneOffset.UTC) shouldBe mp.atUTC().toEpochSeconds()
            }

            "LocalDateTime.mp" {
                val jvm: LocalDateTime = LocalDateTime.of(
                    2022, Month.APRIL, 5, 12, 13, 14, 123_000_000
                )

                val mp: MpLocalDateTime = jvm.mp

                mp shouldBe MpLocalDateTime.of(
                    2022, Month.APRIL, 5, 12, 13, 14, 123_000_000
                )

                jvm.toEpochSecond(ZoneOffset.UTC) shouldBe mp.atUTC().toEpochSeconds()
            }

            "MpLocalDate.jvm" {
                val mp: MpLocalDate = MpLocalDate.of(2022, Month.APRIL, 5)
                val jvm: LocalDate = mp.jvm

                jvm shouldBe LocalDate.of(2022, Month.APRIL, 5)
            }

            "LocalDate.mp" {
                val jvm: LocalDate = LocalDate.of(2022, Month.APRIL, 5)
                val mp: MpLocalDate = jvm.mp

                mp shouldBe MpLocalDate.of(2022, Month.APRIL, 5)
            }

            "MpZonedDateTime.jvm" {
                val mp: MpZonedDateTime = MpZonedDateTime.parse("2022-04-05T12:13:14.123[Europe/Bucharest]")
                val jvm: ZonedDateTime = mp.jvm

                jvm shouldBe ZonedDateTime.of(
                    LocalDateTime.parse("2022-04-05T12:13:14.123"),
                    ZoneId.of("Europe/Bucharest")
                )

                jvm.toInstant().toEpochMilli() shouldBe mp.toEpochMillis()
                jvm.toLocalDateTime() shouldBe mp.datetime.jvm
                jvm.zone.id shouldBe mp.timezone.id
            }

            "ZonedDateTime.mp" {
                val jvm: ZonedDateTime = ZonedDateTime.of(
                    LocalDateTime.parse("2022-04-05T12:13:14.123"),
                    ZoneId.of("Europe/Bucharest")
                )

                val mp: MpZonedDateTime = jvm.mp

                mp shouldBe MpZonedDateTime.parse("2022-04-05T12:13:14.123[Europe/Bucharest]")

                mp.toEpochMillis() shouldBe jvm.toInstant().toEpochMilli()
                mp.toLocalDateTime().jvm shouldBe jvm.toLocalDateTime()
                mp.timezone.id shouldBe jvm.zone.id
            }
        }
    }
}
