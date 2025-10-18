package de.peekandpoke.funktor.core.broker.vault

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.common.reflection.kType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class IncomingJavaTimeConverterSpec : FreeSpec() {

    init {
        "Supported types" - {

            listOf(
                tuple(Instant::class, true),

                tuple(LocalDate::class, true),
                tuple(LocalTime::class, true),
                tuple(LocalDateTime::class, true),
                tuple(ZonedDateTime::class, true),

                tuple(ZoneId::class, true),

                // negative cases
                tuple(Object::class, false),
            ).forEach { (type, supported) ->

                "Must ${if (supported) "" else " NOT "} support the type '$type'" {

                    val subject = IncomingJavaTimeConverter()

                    subject.canHandle(type.kType().type) shouldBe supported
                }
            }
        }

        "Converting to a ZoneId" - {

            sequenceOf<String>()
                .plus(listOf("UTC", "Z"))
                .plus(listOf("+00:00", "+01:00", "+00:30", "-01:00", "-00:30"))
                .plus(ZoneId.getAvailableZoneIds().sorted())
                .plus(ZoneId.SHORT_IDS.keys)
                .distinct()
                .forEach {
                    "'$it' must be converted to a ZoneId" {

                        val subject = IncomingJavaTimeConverter()

                        subject.convert(it, ZoneId::class).shouldBeInstanceOf<ZoneId>()
                    }
                }
        }

        "Converting to an Instant" - {

            listOf(
                tuple("2019-01-01", Instant.ofEpochMilli(1546300800000)),
                tuple("2019-01-01T12:00", Instant.ofEpochMilli(1546344000000)),
                tuple("2019-01-01 12:00", Instant.ofEpochMilli(1546344000000)),
                tuple("2019-01-01T12:00:00", Instant.ofEpochMilli(1546344000000)),
                tuple("2019-01-01 12:00:00", Instant.ofEpochMilli(1546344000000)),
                tuple("2019-01-01T12:00:00.123", Instant.ofEpochMilli(1546344000123)),
                tuple("2019-01-01 12:00:00.123", Instant.ofEpochMilli(1546344000123)),
                tuple("2019-01-01T12:00:00.123Z", Instant.ofEpochMilli(1546344000123)),
                tuple("2019-01-01 12:00:00.123Z", Instant.ofEpochMilli(1546344000123)),
                tuple(
                    "2019-01-01T12:00:00.123+01:00",
                    ZonedDateTime.parse("2019-01-01T12:00:00.123+01:00").toInstant()
                ),
                tuple(
                    "2019-01-01 12:00:00.123+01:00",
                    ZonedDateTime.parse("2019-01-01T12:00:00.123+01:00").toInstant()
                ),
                tuple(
                    "2019-01-01T00:00:00.123-01:00",
                    ZonedDateTime.parse("2019-01-01T00:00:00.123-01:00").toInstant()
                ),
                tuple(
                    "2019-01-01 00:00:00.123-01:00",
                    ZonedDateTime.parse("2019-01-01T00:00:00.123-01:00").toInstant()
                ),
            ).forEach { (input, expected) ->

                "'$input' must be converted to an Instant" {

                    val subject = IncomingJavaTimeConverter()

                    subject.convert(input, Instant::class) shouldBe expected
                }
            }
        }

        "Converting to a LocalTime" - {

            listOf(
                tuple("00:00", LocalTime.MIN),
                tuple("12:00", LocalTime.of(12, 0)),
                tuple("12:13:14", LocalTime.of(12, 13, 14)),
                tuple("12:13:14.1", LocalTime.of(12, 13, 14, 100 * 1_000_000)),
                tuple("12:13:14.12", LocalTime.of(12, 13, 14, 120 * 1_000_000)),
                tuple("12:13:14.123", LocalTime.of(12, 13, 14, 123 * 1_000_000)),
            ).forEach { (input, expected) ->

                "'$input' must be converted to a LocalTime" {

                    val subject = IncomingJavaTimeConverter()

                    subject.convert(input, LocalTime::class) shouldBe expected
                }
            }
        }

        "Converting to a LocalDate" - {

            listOf(
                tuple("2019-01-01", LocalDate.parse("2019-01-01")),
                tuple("2019-01-01T12:00", LocalDate.parse("2019-01-01")),
                tuple("2019-01-01 12:00", LocalDate.parse("2019-01-01")),
                tuple("2019-01-01T12:00:00", LocalDate.parse("2019-01-01")),
                tuple("2019-01-01 12:00:00", LocalDate.parse("2019-01-01")),
                tuple("2019-01-01T12:00:00.123", LocalDate.parse("2019-01-01")),
                tuple("2019-01-01 12:00:00.123", LocalDate.parse("2019-01-01")),
                tuple("2019-01-01T12:00:00.123Z", LocalDate.parse("2019-01-01")),
                tuple("2019-01-01 12:00:00.123Z", LocalDate.parse("2019-01-01")),
                tuple("2019-01-01T00:00:00.123+01:00", LocalDate.parse("2019-01-01")),
                tuple("2019-01-01 00:00:00.123+01:00", LocalDate.parse("2019-01-01")),
                tuple("2019-01-01T00:00:00.123-01:00", LocalDate.parse("2019-01-01")),
                tuple("2019-01-01 00:00:00.123-01:00", LocalDate.parse("2019-01-01")),
            ).forEach { (input, expected) ->

                "'$input' must be converted to a LocalDate" {

                    val subject = IncomingJavaTimeConverter()

                    subject.convert(input, LocalDate::class) shouldBe expected
                }
            }
        }

        "Converting to LocalDateTime" - {

            listOf(
                tuple("2019-01-01", LocalDateTime.parse("2019-01-01T00:00:00")),
                tuple("2019-01-01T12:00", LocalDateTime.parse("2019-01-01T12:00:00")),
                tuple("2019-01-01 12:00", LocalDateTime.parse("2019-01-01T12:00:00")),
                tuple("2019-01-01T12:00:00", LocalDateTime.parse("2019-01-01T12:00:00")),
                tuple("2019-01-01 12:00:00", LocalDateTime.parse("2019-01-01T12:00:00")),
                tuple("2019-01-01T12:00:00.123", LocalDateTime.parse("2019-01-01T12:00:00.123")),
                tuple("2019-01-01 12:00:00.123", LocalDateTime.parse("2019-01-01T12:00:00.123")),
                tuple("2019-01-01T12:00:00.123Z", LocalDateTime.parse("2019-01-01T12:00:00.123")),
                tuple("2019-01-01 12:00:00.123Z", LocalDateTime.parse("2019-01-01T12:00:00.123")),
                tuple("2019-01-01T00:00:00.123+01:00", LocalDateTime.parse("2019-01-01T00:00:00.123")),
                tuple("2019-01-01 00:00:00.123+01:00", LocalDateTime.parse("2019-01-01T00:00:00.123")),
                tuple("2019-01-01T00:00:00.123-01:00", LocalDateTime.parse("2019-01-01T00:00:00.123")),
                tuple("2019-01-01 00:00:00.123-01:00", LocalDateTime.parse("2019-01-01T00:00:00.123")),
            ).forEach { (input, expected) ->

                "'$input' must be converted to a LocalDateTime" {

                    val subject = IncomingJavaTimeConverter()

                    subject.convert(input, LocalDateTime::class) shouldBe expected
                }
            }
        }

        "Converting to a ZonedDateTime" - {

            listOf(
                tuple("2019-01-01", ZonedDateTime.parse("2019-01-01T00:00:00Z")),
                tuple("2019-01-01T12:00", ZonedDateTime.parse("2019-01-01T12:00:00Z")),
                tuple("2019-01-01 12:00", ZonedDateTime.parse("2019-01-01T12:00:00Z")),
                tuple("2019-01-01T12:00:00", ZonedDateTime.parse("2019-01-01T12:00:00Z")),
                tuple("2019-01-01 12:00:00", ZonedDateTime.parse("2019-01-01T12:00:00Z")),
                tuple("2019-01-01T12:00:00.123", ZonedDateTime.parse("2019-01-01T12:00:00.123Z")),
                tuple("2019-01-01 12:00:00.123", ZonedDateTime.parse("2019-01-01T12:00:00.123Z")),
                tuple("2019-01-01T12:00:00.123Z", ZonedDateTime.parse("2019-01-01T12:00:00.123Z")),
                tuple("2019-01-01 12:00:00.123Z", ZonedDateTime.parse("2019-01-01T12:00:00.123Z")),
                tuple("2019-01-01T00:00:00.123+01:00", ZonedDateTime.parse("2019-01-01T00:00:00.123+01:00")),
                tuple("2019-01-01 00:00:00.123+01:00", ZonedDateTime.parse("2019-01-01T00:00:00.123+01:00")),
                tuple("2019-01-01T00:00:00.123-01:00", ZonedDateTime.parse("2019-01-01T00:00:00.123-01:00")),
                tuple("2019-01-01 00:00:00.123-01:00", ZonedDateTime.parse("2019-01-01T00:00:00.123-01:00")),
                // TODO: fix the next two
                // tuple("2019-01-01T12:00:00.123[Europe/Berlin]", ZonedDateTime.parse("2019-01-01T12:00:00.123Z[Europe/Berlin]")),
                // tuple("2019-01-01 12:00:00.123[Europe/Berlin]", ZonedDateTime.parse("2019-01-01T12:00:00.123Z[Europe/Berlin]")),
            ).forEach { (input, expected) ->

                "'$input' must be converted to a ZonedDateTime" {

                    val subject = IncomingJavaTimeConverter()

                    subject.convert(input, ZonedDateTime::class) shouldBe expected
                }
            }
        }
    }
}
