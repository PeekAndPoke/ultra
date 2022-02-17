package de.peekandpoke.ultra.foundation.timing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

interface Kronos {

    companion object {
        val systemDefaultZone: Kronos = fromClock(Clock.systemDefaultZone())

        val systemUtc: Kronos = fromClock(Clock.systemUTC())

        fun fromClock(clock: Clock): Kronos = FromClock(clock)
    }

    private class FromClock(private val clock: Clock) : Kronos {

        override fun timezone(): ZoneId = clock.zone

        override fun instantNow(): Instant =
            Instant.now(clock)

        override fun localDateNow(): LocalDate =
            LocalDate.now(clock)

        override fun localDateTimeNow(): LocalDateTime =
            LocalDateTime.now(clock)

        override fun zonedDateTimeNow(): ZonedDateTime =
            ZonedDateTime.now(clock)

        override fun zonedDateTimeNow(timezone: ZoneId): ZonedDateTime =
            ZonedDateTime.now(clock).withZoneSameInstant(timezone)

        override fun localTimeNow(): LocalTime =
            LocalTime.now(clock)
    }

    private class AdvancedBy(private val inner: Kronos, private val provider: () -> Duration) : Kronos {

        private val duration by lazy { provider() }

        override fun timezone(): ZoneId =
            inner.timezone()

        override fun instantNow(): Instant =
            inner.instantNow().plus(duration)

        override fun localDateNow(): LocalDate =
            inner.localDateNow().plusDays(duration.toDays())

        override fun localDateTimeNow(): LocalDateTime =
            inner.localDateTimeNow().plus(duration)

        override fun zonedDateTimeNow(): ZonedDateTime =
            inner.zonedDateTimeNow().plus(duration)

        override fun zonedDateTimeNow(timezone: ZoneId): ZonedDateTime =
            inner.zonedDateTimeNow(timezone).plus(duration)

        override fun localTimeNow(): LocalTime =
            inner.localTimeNow().plus(duration)
    }

    /**
     * Creates a Kronos the manipulates created dates and times.
     *
     * The created dates and times will be advances by the given [duration].
     *
     * The provider [duration] is guaranteed to be only called exactly once.
     * It will be called the first time it is needed.
     */
    fun advanceBy(duration: () -> Duration): Kronos = AdvancedBy(inner = this, provider = duration)

    /**
     * Creates a Kronos the manipulates created dates and times.
     *
     * The created dates and times will be advances by the given [duration].
     */
    fun advanceBy(duration: Duration): Kronos = advanceBy { duration }

    /** Get the [ZoneOffset] of the internal time source */
    fun timezone(): ZoneId

    /** Get 'now' in epoch millis */
    fun millisNow(): Long = instantNow().toEpochMilli()

    /** Get 'now' in epoch micros */
    fun microsNow(): Long = millisNow() * 1000L

    /** Creates an [Instant] for 'now' */
    fun instantNow(): Instant

    /** Creates a [LocalDate] for 'now' */
    fun localDateNow(): LocalDate

    /** Creates a [LocalDateTime] for 'now' */
    fun localDateTimeNow(): LocalDateTime

    /** Creates a [ZonedDateTime] for 'now' */
    fun zonedDateTimeNow(): ZonedDateTime

    /** Creates a [ZonedDateTime] for 'now' at the given [timezone] */
    fun zonedDateTimeNow(timezone: ZoneId): ZonedDateTime

    /** Creates a [LocalTime] for 'now' */
    fun localTimeNow(): LocalTime
}
