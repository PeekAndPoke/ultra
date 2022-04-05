package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.time.Duration

@Suppress("Detekt.TooManyFunctions")
interface Kronos {

    private class FixedClock(private val fixedInstant: Instant) : Clock {
        override fun now(): Instant {
            return fixedInstant
        }
    }

    companion object {
        val systemUtc: Kronos = fromClock(Clock.System)

        fun fromClock(clock: Clock): Kronos = SystemClock(clock)

        fun fixed(instant: MpInstant): Kronos = fromClock(FixedClock(instant.value))
    }

    private class SystemClock(private val clock: Clock) : Kronos {

        override fun describe(): KronosDescriptor = KronosDescriptor.SystemClock

        override fun instantNow(): MpInstant {
            return MpInstant(clock.now())
        }
    }

    private class AdvancedBy(private val inner: Kronos, private val provider: () -> Duration) : Kronos {

        private val duration by lazy { provider() }

        override fun describe(): KronosDescriptor = KronosDescriptor.AdvancedBy(
            durationMs = duration.inWholeMilliseconds,
            inner = inner.describe(),
        )

        override fun instantNow(): MpInstant {
            return inner.instantNow().plus(duration)
        }
    }

    /**
     * Returns a description of the kronos
     */
    fun describe(): KronosDescriptor

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

    /** Get 'now' in epoch seconds */
    fun secondsNow(): Long = instantNow().toEpochSeconds()

    /** Get 'now' in epoch millis */
    fun millisNow(): Long = instantNow().toEpochMillis()

    /** Get 'now' in epoch micros */
    fun microsNow(): Long = millisNow() * 1000L

    /** Creates an [MpInstant] for 'now' */
    fun instantNow(): MpInstant

    /** Creates a [MpLocalDate] for 'now' */
    fun localDateNow(): MpLocalDate {
        return instantNow().atZone(TimeZone.UTC).toLocalDate()
    }

    /** Creates a [MpLocalDateTime] for 'now' */
    fun localDateTimeNow(): MpLocalDateTime {
        return instantNow().atZone(TimeZone.UTC).toLocalDateTime()
    }

    /** Creates a [MpZonedDateTime] for 'now' */
    fun zonedDateTimeNow(timezone: TimeZone): MpZonedDateTime {
        return instantNow().atZone(timezone)
    }

    /** Creates a [MpLocalTime] for 'now' */
    fun localTimeNow(timezone: TimeZone): MpLocalTime {
        return zonedDateTimeNow(timezone).toLocalTime()
    }
}
