package io.peekandpoke.ultra.slumber.builtin.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.datetime.MpLocalDateTime
import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import io.peekandpoke.ultra.slumber.AwakerException
import io.peekandpoke.ultra.slumber.Codec
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf
import kotlinx.datetime.Month
import kotlinx.datetime.LocalDate as KxLocalDate
import kotlinx.datetime.LocalDateTime as KxLocalDateTime

/**
 * A date awaker returns null for input it cannot read, as every awaker does, and Slumber turns that into an
 * [AwakerException] naming the path (ULTRA-00294). They used to THROW: an NPE, a `ClassCastException` or a zone
 * exception, which a REST body turned into a 500, and which failed an object even where the field is nullable.
 */
class DateAwakersMalformedInputSpec : StringSpec({

    val codec = Codec.default

    val ts = 1649116800000L

    /** The date types that read a `{ts, timezone}` map, with whether they must refuse a zone that is missing or not a string. */
    val mapped: List<Pair<KType, Boolean>> = listOf(
        typeOf<MpLocalDate>() to true,
        typeOf<MpLocalDateTime>() to true,
        typeOf<MpZonedDateTime>() to true,
        typeOf<ZonedDateTime>() to true,
        typeOf<KxLocalDate>() to false, // a missing or non-string zone reads as UTC, documented
        typeOf<KxLocalDateTime>() to false,
        typeOf<MpInstant>() to false, // reads no zone
        typeOf<Instant>() to false,
        typeOf<LocalDate>() to false,
        typeOf<LocalDateTime>() to false,
        typeOf<Date>() to false,
    )

    /** Those that read the zone at all: an unknown one is refused. */
    val zoned = listOf(
        typeOf<MpLocalDate>(), typeOf<MpLocalDateTime>(), typeOf<MpZonedDateTime>(), typeOf<ZonedDateTime>(),
        typeOf<KxLocalDate>(), typeOf<KxLocalDateTime>(),
    )

    val missingZone = mapOf("ts" to ts)
    val nonStringZones = listOf(mapOf("ts" to ts, "timezone" to 42), mapOf("ts" to ts, "timezone" to mapOf("a" to 1)))
    val unknownZones = listOf("Mars/Olympus_Mons", "", "+99:00", "Europe/").map { mapOf("ts" to ts, "timezone" to it) }
    val noTs = listOf(mapOf("timezone" to "UTC"), mapOf("ts" to "yesterday", "timezone" to "UTC"), emptyMap<String, Any>())
    val notAMap = listOf("2022-04-05", 42, listOf(1), true)

    /** Read as [type] at the root and in a box, non-null: refused; in a nullable box: null. */
    fun refused(type: KType, input: Any?) = withClue("$type <- $input") {
        shouldThrow<AwakerException> { codec.awake(type, input) }
        shouldThrow<AwakerException> { codec.awake(boxOf(type), mapOf("value" to input)) }
        (codec.awake(nullableBoxOf(type), mapOf("value" to input)) as NullableBox<*>).value.shouldBeNull()
    }

    "a zone that is missing or not a string is refused by the awakers that need one" {
        mapped.filter { it.second }.forEach { (type, _) ->
            refused(type, missingZone)
            nonStringZones.forEach { refused(type, it) }
        }
    }

    "a zone no one knows is refused by every awaker that reads the zone" {
        zoned.forEach { type -> unknownZones.forEach { refused(type, it) } }
    }

    "a missing or non-numeric ts, or no map at all, is refused by every map-reading awaker" {
        mapped.forEach { (type, _) -> (noTs + notAMap).forEach { refused(type, it) } }
    }

    "no date awaker throws anything but AwakerException, whatever it is given" {
        val everything: List<Any?> = listOf(missingZone) + nonStringZones + unknownZones + noTs + notAMap +
            listOf(Long.MAX_VALUE, Long.MIN_VALUE, 1e300, -1e300, Double.NaN).flatMap { n ->
                listOf(n, mapOf("ts" to n, "timezone" to "UTC"), mapOf("ts" to n, "timezone" to "Pacific/Kiritimati"))
            }
        val types = mapped.map { it.first } +
            listOf(typeOf<LocalTime>(), typeOf<MpLocalTime>(), typeOf<ZoneId>(), typeOf<MpTimezone>())

        types.forEach { type ->
            everything.forEach { input ->
                withClue("$type <- $input") {
                    try {
                        codec.awake(type, input)
                    } catch (_: AwakerException) {
                        // refused: fine
                    }
                }
            }
        }
    }

    "valid zones still read as before, the date in the zone given: a region, an offset, Z and the system zone" {
        // 2022-04-04T22:00Z: still April 4 in UTC, already April 5 east of it. A zone ignored reads the wrong day.
        val lateEvening = 1649109600000L

        codec.awake(typeOf<MpLocalDate>(), mapOf("ts" to lateEvening, "timezone" to "Europe/Bucharest")) shouldBe
            MpLocalDate.of(2022, Month.APRIL, 5)
        codec.awake(typeOf<MpLocalDate>(), mapOf("ts" to lateEvening, "timezone" to "+03:00")) shouldBe
            MpLocalDate.of(2022, Month.APRIL, 5)
        codec.awake(typeOf<MpLocalDate>(), mapOf("ts" to lateEvening, "timezone" to "UTC+03:00")) shouldBe
            MpLocalDate.of(2022, Month.APRIL, 5)
        codec.awake(typeOf<MpLocalDate>(), mapOf("ts" to lateEvening, "timezone" to "Z")) shouldBe
            MpLocalDate.of(2022, Month.APRIL, 4)
        codec.awake(typeOf<MpLocalDateTime>(), mapOf("ts" to lateEvening, "timezone" to "SYSTEM")).shouldNotBeNull()
        codec.awake(typeOf<KxLocalDate>(), mapOf("ts" to lateEvening, "timezone" to "Europe/Bucharest")) shouldBe
            KxLocalDate(2022, 4, 5)
    }
})

private data class Box<T>(val value: T)

private fun boxOf(type: KType): KType = Box::class.createType(listOf(KTypeProjection.invariant(type)))

/** `T?` in the constructor: a `Box<X?>` would not do, Slumber reads nullability off the declared parameter. */
private data class NullableBox<T>(val value: T?)

private fun nullableBoxOf(type: KType): KType = NullableBox::class.createType(listOf(KTypeProjection.invariant(type)))
