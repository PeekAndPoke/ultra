package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Suppress("Detekt.TooManyFunctions")
/**
 * The source of truth for creating dates and times.
 */
interface Kronos {

    companion object {
        /**
         * The [Kronos] that operates on the system clock.
         */
        val systemUtc: Kronos = fromClock(Clock.System)

        /**
         * Creates a [Kronos] that operates on the given [clock].
         */
        fun fromClock(clock: Clock): Kronos {
            return UsingClock(clock)
        }

        /**
         * Creates a [Kronos] that is stuck on the given [instant].
         */
        fun fixed(instant: MpInstant): Kronos {
            return fromClock(FixedClock(instant.value))
        }

        /**
         * Creates a [Kronos] from the given [descriptor].
         */
        fun from(descriptor: KronosDescriptor): Kronos {

            return when (descriptor) {
                is KronosDescriptor.SystemClock -> {
                    systemUtc
                }

                is KronosDescriptor.AdvancedBy -> {
                    descriptor.inner.instantiate()
                        .advanceBy(descriptor.durationMs.milliseconds)
                }
            }
        }
    }

    /**
     * Fixed implementation of [Clock].
     *
     * Will always return the given [fixedInstant].
     */
    private class FixedClock(private val fixedInstant: Instant) : Clock {
        /**
         * 'Now' will always be [fixedInstant].
         */
        override fun now(): Instant {
            return fixedInstant
        }
    }

    /**
     * Implementation that gets [instantNow] from the  given [clock].
     */
    private class UsingClock(private val clock: Clock) : Kronos {

        /**
         * Describes the Kronos.
         */
        override fun describe(): KronosDescriptor = KronosDescriptor.SystemClock

        /**
         * Return the 'now' moment in time.
         */
        override fun instantNow(): MpInstant {
            return MpInstant(clock.now())
        }
    }

    /**
     * Implementation that gets [instantNow] from the [inner] advanced by [provider].
     */
    private class AdvancedBy(private val inner: Kronos, private val provider: () -> Duration) : Kronos {

        private val duration by lazy { provider() }

        /**
         * Describes the Kronos.
         */
        override fun describe(): KronosDescriptor = KronosDescriptor.AdvancedBy(
            durationMs = duration.inWholeMilliseconds,
            inner = inner.describe(),
        )

        /**
         * Return the 'now' moment in time.
         */
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

    /** Creates a [MpLocalDateTime] for 'now' */
    fun localDateTimeNow(): MpLocalDateTime {
        return instantNow().atZone(TimeZone.UTC).toLocalDateTime()
    }

    /** Creates a [MpLocalDate] in the given [timezone] */
    fun localDateNow(timezone: TimeZone): MpLocalDate {
        return instantNow().atZone(timezone).toLocalDate()
    }

    /** Creates a [MpLocalDate] in the given [timezone] */
    fun localDateNow(timezone: MpTimezone): MpLocalDate {
        return localDateNow(timezone.kotlinx)
    }

    /** Creates a [MpLocalTime] in the given [timezone] */
    fun localTimeNow(timezone: TimeZone): MpLocalTime {
        return zonedDateTimeNow(timezone).toLocalTime()
    }

    /** Creates a [MpLocalTime] in the given [timezone] */
    fun localTimeNow(timezone: MpTimezone): MpLocalTime {
        return localTimeNow(timezone.kotlinx)
    }

    /** Creates a [MpZonedDateTime] for 'now' */
    fun zonedDateTimeNow(timezone: TimeZone): MpZonedDateTime {
        return instantNow().atZone(timezone)
    }

    /** Creates a [MpZonedDateTime] for 'now' */
    fun zonedDateTimeNow(timezone: MpTimezone): MpZonedDateTime {
        return zonedDateTimeNow(timezone.kotlinx)
    }
}
