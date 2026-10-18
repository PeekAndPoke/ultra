package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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

    "Kronos.from(descriptor) - a nested descriptor from an old file instantiates and describes back flat" {

        // Written before chained advances collapsed: one nesting level per advanceBy call
        val json = """
            {"type":"advanced-by","durationMs":3600000,"inner":
                {"type":"advanced-by","durationMs":600000,"inner":{"type":"system-clock"}}}
        """.trimIndent()

        val nested = Json.decodeFromString<KronosDescriptor>(json)
        val flat = KronosDescriptor.AdvancedBy(
            durationMs = (10.minutes + 1.hours).inWholeMilliseconds,
            inner = KronosDescriptor.SystemClock,
        )

        nested.instantiate().describe() shouldBe flat
        Json.encodeToString<KronosDescriptor>(flat) shouldBe
                """{"type":"advanced-by","durationMs":4200000,"inner":{"type":"system-clock"}}"""
    }

    "Kronos.from(descriptor) - an infinite advance round-trips" {

        listOf(Duration.INFINITE, -Duration.INFINITE).forEach { infinite ->
            val descriptor = Kronos.systemUtc.advanceBy(infinite).describe()

            descriptor.instantiate().describe() shouldBe descriptor
        }
    }

    "Kronos.from(descriptor) - opposite infinite durations are rejected with a named error" {

        val json = """
            {"type":"advanced-by","durationMs":${Long.MIN_VALUE},"inner":
                {"type":"advanced-by","durationMs":${Long.MAX_VALUE},"inner":{"type":"system-clock"}}}
        """.trimIndent()

        val descriptor = Json.decodeFromString<KronosDescriptor>(json)

        shouldThrow<IllegalArgumentException> { descriptor.instantiate() }.message shouldBe
                "KronosDescriptor.AdvancedBy cannot sum opposite infinite durations"
    }

    "Kronos.from(descriptor) - only opposite infinities are rejected" {

        fun nested(outer: Long, inner: Long) = KronosDescriptor.AdvancedBy(
            durationMs = outer,
            inner = KronosDescriptor.AdvancedBy(durationMs = inner, inner = KronosDescriptor.SystemClock),
        )

        fun flat(durationMs: Long) = KronosDescriptor.AdvancedBy(durationMs, KronosDescriptor.SystemClock)

        listOf(
            nested(Long.MAX_VALUE, Long.MAX_VALUE) to flat(Long.MAX_VALUE),
            nested(Long.MIN_VALUE, Long.MIN_VALUE) to flat(Long.MIN_VALUE),
            nested(Long.MAX_VALUE, -60_000) to flat(Long.MAX_VALUE),
            nested(-60_000, Long.MAX_VALUE) to flat(Long.MAX_VALUE),
            nested(Long.MIN_VALUE, 60_000) to flat(Long.MIN_VALUE),
            nested(60_000, Long.MIN_VALUE) to flat(Long.MIN_VALUE),
        ).forEach { (descriptor, expected) ->
            withClue(descriptor) {
                descriptor.instantiate().describe() shouldBe expected
            }
        }
    }

    "Kronos.from(descriptor) - layers are summed innermost first, as a nested chain reads" {

        // Long.MAX_VALUE / 2 - 1: the largest finite millisecond Duration
        val largest = 4_611_686_018_427_387_902L

        // Innermost first: largest + largest saturates to INFINITE, which then absorbs -largest.
        // Outermost first would give -largest + largest + largest = largest instead.
        val descriptor = KronosDescriptor.AdvancedBy(
            durationMs = -largest,
            inner = KronosDescriptor.AdvancedBy(
                durationMs = largest,
                inner = KronosDescriptor.AdvancedBy(durationMs = largest, inner = KronosDescriptor.SystemClock),
            ),
        )

        descriptor.instantiate().describe() shouldBe
                KronosDescriptor.AdvancedBy(durationMs = Long.MAX_VALUE, inner = KronosDescriptor.SystemClock)
    }

    "Kronos.from(descriptor) - a deeply nested descriptor instantiates without recursion" {

        val depth = 200_000
        var descriptor: KronosDescriptor = KronosDescriptor.SystemClock

        repeat(depth) { descriptor = KronosDescriptor.AdvancedBy(durationMs = 1, inner = descriptor) }

        descriptor.instantiate().describe() shouldBe
                KronosDescriptor.AdvancedBy(durationMs = depth.toLong(), inner = KronosDescriptor.SystemClock)
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

    "Kronos.describe - Kronos.systemUtc.advancedBy.advancedBy collapses into one" {

        val duration1 = 10.seconds
        val duration2 = 5.minutes

        val subject = Kronos.systemUtc.advanceBy { duration1 }.advanceBy(duration2).describe()

        subject shouldBe KronosDescriptor.AdvancedBy(
            durationMs = (duration1 + duration2).inWholeMilliseconds,
            inner = KronosDescriptor.SystemClock,
        )
    }

    "Kronos.describe - advances summing to zero keep the AdvancedBy shape" {

        val subject = Kronos.systemUtc.advanceBy(10.minutes).advanceBy((-10).minutes).describe()

        subject shouldBe KronosDescriptor.AdvancedBy(durationMs = 0, inner = KronosDescriptor.SystemClock)
    }

    "Kronos.describe - sub-millisecond advances add up before truncation" {

        var subject = Kronos.systemUtc

        repeat(4) { subject = subject.advanceBy(500.microseconds) }

        subject.describe() shouldBe KronosDescriptor.AdvancedBy(durationMs = 2, inner = KronosDescriptor.SystemClock)
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    val manySteps = 200_000

    "Kronos.advanceBy - $manySteps chained advances stay one layer deep" {

        var subject = fixedKronos

        repeat(manySteps) { subject = subject.advanceBy(1.minutes) }

        subject.instantNow() shouldBe fixedInstant.plus(manySteps.minutes)
        subject.describe() shouldBe KronosDescriptor.AdvancedBy(
            durationMs = manySteps.minutes.inWholeMilliseconds,
            inner = KronosDescriptor.SystemClock,
        )
    }

    "Kronos.advanceBy - $manySteps unread lazy advances do not recurse" {

        var subject = fixedKronos

        repeat(manySteps) { subject = subject.advanceBy { 1.minutes } }

        subject.instantNow() shouldBe fixedInstant.plus(manySteps.minutes)
    }

    "Kronos.advanceBy - lazy provider is not called by chaining, and exactly once on first read" {

        var calls = 0
        val first = fixedKronos.advanceBy { calls++; 1.hours }

        var subject = first
        repeat(manySteps) { subject = subject.advanceBy(1.minutes) }
        subject = subject.advanceBy { 1.days }

        calls shouldBe 0

        subject.instantNow() shouldBe fixedInstant.plus(1.hours + manySteps.minutes + 1.days)
        calls shouldBe 1

        // Reading again, reading an older instance of the chain, and chaining on a read instance: no further call
        subject.instantNow() shouldBe fixedInstant.plus(1.hours + manySteps.minutes + 1.days)
        first.instantNow() shouldBe fixedInstant.plus(1.hours)
        subject.advanceBy(1.minutes).instantNow() shouldBe fixedInstant.plus(1.hours + (manySteps + 1).minutes + 1.days)
        calls shouldBe 1
    }

    "Kronos.advanceBy - lazy provider is called exactly once when an older instance is read first" {

        var calls = 0
        val first = fixedKronos.advanceBy { calls++; 1.hours }
        val second = first.advanceBy { 2.hours }

        first.instantNow() shouldBe fixedInstant.plus(1.hours)
        second.instantNow() shouldBe fixedInstant.plus(3.hours)
        calls shouldBe 1
    }

    "Kronos.advanceBy - lazy providers are called oldest first" {

        val order = mutableListOf<String>()

        val subject = fixedKronos
            .advanceBy { order.add("first"); 1.hours }
            .advanceBy(1.minutes)
            .advanceBy { order.add("second"); 2.hours }
            .advanceBy { order.add("third"); 3.hours }

        subject.instantNow() shouldBe fixedInstant.plus(6.hours + 1.minutes)
        order shouldBe listOf("first", "second", "third")
    }

    "Kronos.advanceBy - lazy advance on an already read instance keeps its total" {

        var calls = 0
        val read = fixedKronos.advanceBy { calls++; 1.hours }.advanceBy(1.minutes)

        read.instantNow() shouldBe fixedInstant.plus(1.hours + 1.minutes)

        val subject = read.advanceBy { 2.hours }

        subject.instantNow() shouldBe fixedInstant.plus(3.hours + 1.minutes)
        subject.describe() shouldBe KronosDescriptor.AdvancedBy(
            durationMs = (3.hours + 1.minutes).inWholeMilliseconds,
            inner = KronosDescriptor.SystemClock,
        )
        calls shouldBe 1
    }

    "Kronos.advanceBy - a provider that throws is retried on the next read, also through chained instances" {

        var calls = 0
        val first = fixedKronos.advanceBy {
            calls++
            if (calls == 1) error("offset not available yet")
            1.hours
        }
        val chained = first.advanceBy(1.minutes)

        shouldThrow<IllegalStateException> { chained.instantNow() }

        chained.instantNow() shouldBe fixedInstant.plus(1.hours + 1.minutes)
        first.instantNow() shouldBe fixedInstant.plus(1.hours)
        calls shouldBe 2
    }

    "Kronos.advanceBy - lazy provider that is never needed is never called" {

        var calls = 0

        fixedKronos.advanceBy { calls++; 1.hours }.advanceBy(1.minutes).advanceBy { calls++; 1.days }

        calls shouldBe 0
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

    "Kronos.mutable - $manySteps mutate { advanceBy } stay one layer deep" {

        val subject = fixedKronos.mutable()

        repeat(manySteps) { subject.mutate { advanceBy(1.minutes) } }

        subject.instantNow() shouldBe fixedInstant.plus(manySteps.minutes)
        subject.describe() shouldBe KronosDescriptor.AdvancedBy(
            durationMs = manySteps.minutes.inWholeMilliseconds,
            inner = KronosDescriptor.SystemClock,
        )
    }

    "Kronos.mutable - an advance over a mutable keeps following it" {

        val mutable = fixedKronos.mutable()
        val subject = mutable.advanceBy(1.hours).advanceBy { 1.minutes }

        subject.instantNow() shouldBe fixedInstant.plus(1.hours + 1.minutes)

        val later = fixedInstant.plus(1.days)
        mutable.set(Kronos.fixed(later))

        subject.instantNow() shouldBe later.plus(1.hours + 1.minutes)
    }

    "Kronos.mutable - set rejects a kronos that reads the mutable itself" {

        val subject = fixedKronos.mutable()
        val wrapsSubject = subject.advanceBy(1.hours).mutable()

        listOf(
            subject,
            subject.advanceBy(1.hours),
            subject.advanceBy { 1.hours }.advanceBy(1.minutes),
            wrapsSubject,
            wrapsSubject.advanceBy(1.minutes),
        ).forEach { cyclic ->
            shouldThrow<IllegalArgumentException> { subject.set(cyclic) }

            subject.instantNow() shouldBe fixedInstant
        }

        // A legitimate set still applies after rejected ones
        val later = Kronos.fixed(fixedInstant.plus(1.days))
        subject.set(later)

        subject.instantNow() shouldBe fixedInstant.plus(1.days)
    }

    "Kronos.mutable - mutate rejects a result that reads the mutable itself" {

        val subject = fixedKronos.mutable()

        shouldThrow<IllegalArgumentException> { subject.mutate { subject.advanceBy(1.hours) } }

        subject.instantNow() shouldBe fixedInstant

        subject.mutate { advanceBy(1.hours) }

        subject.instantNow() shouldBe fixedInstant.plus(1.hours)
    }

    "Kronos.mutable - set accepts another mutable that does not read this one" {

        val other = Kronos.fixed(fixedInstant.plus(1.days)).mutable()
        val subject = fixedKronos.mutable()

        subject.set(other.advanceBy(1.hours))

        subject.instantNow() shouldBe fixedInstant.plus(1.days + 1.hours)
    }

    "Kronos.mutable - Mutable.mutable return the same instance" {

        val subject = Kronos.systemUtc.mutable()

        subject shouldBeSameInstanceAs subject.mutable()
    }
})
