package io.peekandpoke.ultra.datetime

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

    /**
     * Mutable kronos.
     *
     * [set] and [mutate] throw [IllegalArgumentException] for an inner that reads this mutable, which would
     * recurse forever on every read. The check sees through mutable and [advanceBy] layers only, not into
     * `advanceBy { }` providers, clocks or other [Kronos] implementations. It is not safe against concurrent
     * writers: two threads setting two mutables to read each other can still build a cycle.
     */
    interface Mutable : Kronos {
        /** Sets the inner kronos */
        fun set(inner: Kronos)

        /** Mutates the inner kronos */
        fun mutate(block: Kronos.() -> Kronos)
    }

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
         * Creates a [Kronos] that is stuck on 'now'.
         */
        fun fixedNow(): Kronos = fixed(MpInstant.now())

        /**
         * Creates a [Kronos] from the given [descriptor].
         */
        fun from(descriptor: KronosDescriptor): Kronos {

            return when (descriptor) {
                is KronosDescriptor.SystemClock -> {
                    systemUtc
                }

                is KronosDescriptor.AdvancedBy -> {
                    // Nesting comes from files written before chained advances collapsed, or from a Mutable between
                    // layers. Walked in a loop rather than recursion, summed innermost first like a live chain reads.
                    val durations = mutableListOf<Duration>()
                    var current: KronosDescriptor = descriptor

                    while (current is KronosDescriptor.AdvancedBy) {
                        durations.add(current.durationMs.milliseconds)
                        current = current.inner
                    }

                    val total = durations.foldRight(Duration.ZERO) { duration, acc ->
                        require(!(acc.isInfinite() && duration.isInfinite() && acc.isPositive() != duration.isPositive())) {
                            "KronosDescriptor.AdvancedBy cannot sum opposite infinite durations"
                        }

                        acc + duration
                    }

                    current.instantiate().advanceBy(total)
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
     * Implementation that gets [instantNow] from the [inner] advanced by [settled] plus all [pending] durations.
     *
     * Chained advances collapse into one instance, so [inner] is never an [AdvancedBy].
     */
    private class AdvancedBy(
        val inner: Kronos,
        private val settled: Duration,
        private val pending: Pending?,
    ) : Kronos {

        /** Immutable list of lazy durations nobody has needed yet, newest first. Shared between chained instances. */
        class Pending(val duration: Lazy<Duration>, val older: Pending?)

        // PUBLICATION: no lock is held while the shared pending lazies run, as before the collapse
        private val total: Lazy<Duration> = lazy(LazyThreadSafetyMode.PUBLICATION) {
            // Oldest first, as the former nested implementation did for instantNow()
            val lazies = mutableListOf<Lazy<Duration>>()
            var current = pending

            while (current != null) {
                lazies.add(current.duration)
                current = current.older
            }

            lazies.foldRight(settled) { lazy, acc -> acc + lazy.value }
        }

        /**
         * Describes the Kronos.
         */
        override fun describe(): KronosDescriptor = KronosDescriptor.AdvancedBy(
            durationMs = total.value.inWholeMilliseconds,
            inner = inner.describe(),
        )

        /**
         * Return the 'now' moment in time.
         */
        override fun instantNow(): MpInstant {
            return inner.instantNow().plus(total.value)
        }

        override fun advanceBy(duration: () -> Duration): Kronos = when {
            total.isInitialized() -> AdvancedBy(inner, total.value, Pending(lazy(duration), null))
            else -> AdvancedBy(inner, settled, Pending(lazy(duration), pending))
        }

        override fun advanceBy(duration: Duration): Kronos = when {
            total.isInitialized() -> AdvancedBy(inner, total.value + duration, null)
            else -> AdvancedBy(inner, settled + duration, pending)
        }
    }

    /**
     * Implementation of [Mutable]
     */
    private class MutableImpl(inner: Kronos) : Mutable {

        private var _inner: Kronos = inner

        override fun set(inner: Kronos) {
            val chain = inner.recurse {
                when (val rec = this) {
                    is MutableImpl -> rec._inner
                    is AdvancedBy -> rec.inner
                    else -> null
                }
            }

            require(this !in chain) {
                "Kronos.Mutable cannot be set to a kronos that reads the mutable itself"
            }

            _inner = inner
        }

        override fun mutate(block: Kronos.() -> Kronos) {
            set(block(_inner))
        }

        override fun describe(): KronosDescriptor = _inner.describe()

        override fun instantNow(): MpInstant = _inner.instantNow()
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
     * The provider [duration] is called once, the first time the clock is read, never while chaining.
     * A provider that throws is called again on the next read.
     */
    fun advanceBy(duration: () -> Duration): Kronos = AdvancedBy(
        inner = this,
        settled = Duration.ZERO,
        pending = AdvancedBy.Pending(lazy(duration), null),
    )

    /**
     * Creates a Kronos the manipulates created dates and times.
     *
     * The created dates and times will be advances by the given [duration].
     */
    fun advanceBy(duration: Duration): Kronos = AdvancedBy(inner = this, settled = duration, pending = null)

    /** Makes this Kronos a [Mutable] */
    fun mutable(): Mutable = when (this) {
        is Mutable -> this
        else -> MutableImpl(this)
    }

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
