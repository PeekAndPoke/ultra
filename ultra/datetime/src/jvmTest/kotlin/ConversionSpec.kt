package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ConversionSpec : FreeSpec() {

    init {
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
                    2022, Month.APRIL, 5, 12, 13, 14, 123
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
                    2022, Month.APRIL, 5, 12, 13, 14, 123
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

            "OffsetDateTime.mp (to MpInstant)" {
                val odt: OffsetDateTime = OffsetDateTime.of(
                    2022, 4, 5, 12, 13, 14, 0, ZoneOffset.ofHours(3)
                )

                val result: MpInstant = odt.mp

                result shouldBe MpInstant.parse("2022-04-05T09:13:14.000Z")
                result.toEpochMillis() shouldBe odt.toInstant().toEpochMilli()
            }

            "OffsetDateTime.mp(zone) (to MpZonedDateTime)" {
                val odt: OffsetDateTime = OffsetDateTime.of(
                    2022, 4, 5, 12, 13, 14, 0, ZoneOffset.ofHours(3)
                )
                val zone = MpTimezone.of("Europe/Berlin")

                val result: MpZonedDateTime = odt.mp(zone)

                result shouldBe odt.mp.atZone(zone)
                result.toEpochMillis() shouldBe odt.toInstant().toEpochMilli()
            }

            "LocalTime.mp (to MpLocalTime)" {
                val jvm: LocalTime = LocalTime.of(12, 13, 14)

                val result: MpLocalTime = jvm.mp

                result shouldBe MpLocalTime.of(hour = 12, minute = 13, second = 14)
                result.hour shouldBe 12
                result.minute shouldBe 13
                result.second shouldBe 14
            }

            "MpLocalTime.jvm (to java LocalTime)" {
                val mp: MpLocalTime = MpLocalTime.of(hour = 12, minute = 13, second = 14)

                val result: LocalTime = mp.jvm

                result shouldBe LocalTime.of(12, 13, 14)
            }

            "ZoneId.kotlinx (to kotlinx TimeZone)" {
                val zoneId: ZoneId = ZoneId.of("Europe/Berlin")

                val result: TimeZone = zoneId.kotlinx

                result shouldBe TimeZone.of("Europe/Berlin")
                result.id shouldBe "Europe/Berlin"
            }

            "TimeZone.of(ZoneId) (factory)" {
                val zoneId: ZoneId = ZoneId.of("Europe/Berlin")

                val result: TimeZone = TimeZone.of(zoneId)

                result shouldBe TimeZone.of("Europe/Berlin")
                result.id shouldBe "Europe/Berlin"
            }

            "TimeZone.jvm (to java ZoneId)" {
                val tz: TimeZone = TimeZone.of("Europe/Berlin")

                val result: ZoneId = tz.jvm

                result shouldBe ZoneId.of("Europe/Berlin")
                result.id shouldBe "Europe/Berlin"
            }

            "ZoneId.mp (to MpTimezone)" {
                val zoneId: ZoneId = ZoneId.of("Europe/Berlin")

                val result: MpTimezone = zoneId.mp

                result shouldBe MpTimezone.of("Europe/Berlin")
                result.id shouldBe "Europe/Berlin"
            }

            "MpTimezone.jvm (to java ZoneId)" {
                val mp: MpTimezone = MpTimezone.of("Europe/Berlin")

                val result: ZoneId = mp.jvm

                result shouldBe ZoneId.of("Europe/Berlin")
                result.id shouldBe "Europe/Berlin"
            }
        }
    }
}
