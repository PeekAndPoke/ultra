package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("unused")
class KronosSpec : StringSpec({

    val fixedLocalDateTime = MpLocalDateTime.parse("2022-02-16T12:00:00.000Z")
    val fixedInstant = fixedLocalDateTime.toInstant(TimeZone.UTC)

    val fixedKronos = Kronos.fixed(fixedInstant)

    "Kronos.from(descriptor)" {

        val descriptors = listOf(
            Kronos.systemUtc.describe(),
            Kronos.systemUtc.advanceBy(10.minutes).describe(),
            Kronos.systemUtc.advanceBy(10.minutes).advanceBy(1.hours).describe(),
        )

        descriptors.forEach { descriptor ->
            descriptor.instantiate().describe() shouldBe descriptor

            Kronos.from(descriptor).describe() shouldBe descriptor
        }
    }

    "Kronos.fromClock fixed - secondsNow" {
        fixedKronos.secondsNow() shouldBe fixedInstant.toEpochSeconds()
    }

    "Kronos.fromClock fixed - millisNow" {
        fixedKronos.millisNow() shouldBe fixedInstant.toEpochMillis()
    }

    "Kronos.fromClock fixed - microsNow" {
        fixedKronos.microsNow() shouldBe fixedInstant.toEpochMillis() * 1000L
    }

    "Kronos.fromClock fixed - instantNow" {
        fixedKronos.instantNow() shouldBe fixedInstant
    }

    "Kronos.fromClock fixed - localDateTimeNow" {
        fixedKronos.localDateTimeNow() shouldBe fixedLocalDateTime
    }

    "Kronos.fromClock fixed - localDateNow - kotlinx timezone" {
        fixedKronos.localDateNow(TimeZone.UTC) shouldBe fixedLocalDateTime.toDate()

        fixedKronos.localDateNow(TimeZone.of("Europe/Berlin")) shouldBe fixedLocalDateTime.toDate()
    }

    "Kronos.fromClock fixed - localDateNow - MpTimezone" {
        fixedKronos.localDateNow(MpTimezone.UTC) shouldBe fixedLocalDateTime.toDate()

        fixedKronos.localDateNow(MpTimezone.of("Europe/Berlin")) shouldBe fixedLocalDateTime.toDate()
    }

    "Kronos.fromClock fixed - localTimeNow - kotlinx timezone" {
        fixedKronos.localTimeNow(TimeZone.UTC) shouldBe fixedLocalDateTime.toTime()

        fixedKronos.localTimeNow(TimeZone.of("Europe/Berlin")) shouldBe MpLocalTime.of(13, 0)
    }

    "Kronos.fromClock fixed - localTimeNow - MpTimezone" {
        fixedKronos.localTimeNow(MpTimezone.UTC) shouldBe fixedLocalDateTime.toTime()

        fixedKronos.localTimeNow(MpTimezone.of("Europe/Berlin")) shouldBe MpLocalTime.of(13, 0)
    }

    "Kronos.fromClock fixed - zonedDateTimeNow - kotlinx Timezone" {
        MpTimezone.supportedIds.take(10).forEach { zoneId ->
            val timezone = TimeZone.of(zoneId)

            withClue("Must work for timezone '$zoneId'") {
                fixedKronos.zonedDateTimeNow(timezone).toInstant() shouldBe fixedInstant
            }
        }
    }

    "Kronos.fromClock fixed - zonedDateTimeNow - MpTimezone" {
        MpTimezone.supportedIds.forEach { zoneId ->
            val timezone = MpTimezone.of(zoneId)

            withClue("Must work for timezone '$zoneId'") {
                fixedKronos.zonedDateTimeNow(timezone).toInstant() shouldBe fixedInstant
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    val advances: List<Duration> = listOf(
        (-1).days + (-1).hours,
        ((-2).hours + (-2).minutes) + (-2).seconds,
        0.seconds,
        (2.hours + 2.minutes) + 2.seconds,
        1.days + 1.hours,
    )

    advances.forEach { advance ->

        val subject: Kronos = fixedKronos.advanceBy { advance }

        "Kronos.advanceBy $advance - secondsNow" {
            subject.secondsNow() shouldBe fixedInstant.toEpochSeconds() + advance.inWholeSeconds
        }

        "Kronos.advanceBy $advance - millisNow" {
            subject.millisNow() shouldBe fixedInstant.toEpochMillis() + advance.inWholeMilliseconds
        }

        "Kronos.advanceBy $advance - microsNow" {
            subject.microsNow() shouldBe (fixedInstant.toEpochMillis() + advance.inWholeMilliseconds) * 1000L
        }

        "Kronos.advanceBy $advance - instantNow" {
            subject.instantNow() shouldBe fixedInstant.plus(advance)
        }

        "Kronos.advanceBy $advance - localDateNow" {
            subject.localDateNow(MpTimezone.UTC) shouldBe fixedLocalDateTime.atUTC().plus(advance).toLocalDate()
        }

        "Kronos.advanceBy $advance - localTimeNow" {
            MpTimezone.supportedIds.forEach { zoneId ->
                val timezone = MpTimezone.of(zoneId)

                withClue("Must work for timezone '$zoneId'") {
                    subject.localTimeNow(timezone) shouldBe
                            fixedInstant.plus(advance).atZone(timezone).toLocalTime()
                }
            }
        }

        "Kronos.advanceBy $advance - localDateTimeNow" {
            subject.localDateTimeNow() shouldBe fixedInstant.plus(advance).atZone(TimeZone.UTC).datetime
        }

        "Kronos.advanceBy $advance - zonedDateTimeNow" {
            MpTimezone.supportedIds.forEach {
                val timezone = TimeZone.of(it)

                withClue("Must work for timezone '$timezone'") {
                    subject.zonedDateTimeNow(timezone).toInstant() shouldBe fixedInstant.plus(advance)
                }
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    "Kronos.describe - Kronos.systemUtc" {

        val subject = Kronos.systemUtc.describe()

        subject shouldBe KronosDescriptor.SystemClock
    }

    "Kronos.describe - Kronos.fromClock (not fixed)" {
        val subject = Kronos.fromClock(Clock.System).describe()

        subject shouldBe KronosDescriptor.SystemClock
    }

    "Kronos.describe - Kronos.systemUtc.advancedBy" {

        val duration = 10.seconds

        val subject = Kronos.systemUtc.advanceBy { duration }.describe()

        subject shouldBe KronosDescriptor.AdvancedBy(
            durationMs = duration.inWholeMilliseconds,
            inner = KronosDescriptor.SystemClock
        )
    }

    "Kronos.describe - Kronos.systemUtc.advancedBy.advancedBy" {

        val duration1 = 10.seconds
        val duration2 = 5.minutes

        val subject = Kronos.systemUtc.advanceBy { duration1 }.advanceBy(duration2).describe()

        subject shouldBe KronosDescriptor.AdvancedBy(
            durationMs = duration2.inWholeMilliseconds,
            inner = KronosDescriptor.AdvancedBy(
                durationMs = duration1.inWholeMilliseconds,
                inner = KronosDescriptor.SystemClock,
            )
        )
    }

    "Kronos.mutable" {

        val inner = Kronos.fixed(MpInstant.now())
        val subject = inner.mutable()

        subject.shouldBeInstanceOf<Kronos.Mutable>()
        subject.describe() shouldBe inner.describe()
        subject.instantNow() shouldBe inner.instantNow()

        val modified = Kronos.fixed(MpInstant.now()).advanceBy(1.hours)
        subject.set(modified)

        subject.shouldBeInstanceOf<Kronos.Mutable>()
        subject.describe() shouldBe modified.describe()
        subject.instantNow() shouldBe modified.instantNow()
    }

    "Kronos.mutable - Mutable.mutable return the same instance" {

        val subject = Kronos.systemUtc.mutable()

        subject shouldBeSameInstanceAs subject.mutable()
    }
})
