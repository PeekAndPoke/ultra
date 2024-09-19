package de.peekandpoke.ultra.common.datetime

import korlibs.time.Time
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// TODO: test all of me
@Suppress("DataClassPrivateConstructor")
@Serializable(with = MpLocalTimeSerializer::class)
data class MpLocalTime private constructor(private val milliSeconds: Long) : Comparable<MpLocalTime> {

    companion object {
        const val MillisPerSecond = 1_000L
        const val MillisPerMinute = 60 * MillisPerSecond
        const val MillisPerHour = 60 * MillisPerMinute
        const val MillisPerDay = 24 * MillisPerHour

        /**
         * Parses the given input into an [MpLocalTime].
         */
        fun parse(input: String): MpLocalTime {
            val parts = input.split(":")

            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val lastPart = (parts.getOrNull(2) ?: "").split(".")

            val second = lastPart.getOrNull(0)?.toIntOrNull() ?: 0
            val millis = lastPart.getOrNull(1)?.toIntOrNull() ?: 0

            return of(
                hour = hour,
                minute = minute,
                second = second,
                milliSecond = millis,
            )
        }

        /**
         * Parses an [MpLocalTime] from the given [input].
         *
         * Returns null if parsing fails.
         */
        fun tryParse(input: String): MpLocalTime? {
            if (input.isBlank()) {
                return null
            }

            return parse(input)
        }

        /**
         * Creates an [MpLocalTime].
         */
        fun of(hour: Int, minute: Int, second: Int = 0, milliSecond: Int = 0): MpLocalTime {
            return ofMilliSeconds(
                milliSecondsOfDay = (hour * 60 * 60 + minute * 60 + second) * 1_000L + milliSecond
            )
        }

        /**
         * Creates an [MpLocalTime] from the given [milliSecondsOfDay].
         */
        fun ofMilliSeconds(milliSecondsOfDay: Long): MpLocalTime {
            return MpLocalTime(
                milliSeconds = milliSecondsOfDay % MillisPerDay
            )
        }

        /**
         * Creates an [MpLocalTime] from the given [secondsOfDay].
         */
        fun ofSecondOfDay(secondsOfDay: Long): MpLocalTime {
            return ofMilliSeconds(milliSecondsOfDay = secondsOfDay * 1_000L)
        }

        /**
         * The minimal possible time at 00:00:00.000
         */
        val Min: MpLocalTime = of(hour = 0, minute = 0, second = 0, milliSecond = 0)

        /**
         * The minimal possible time at 23:59:59.999
         */
        val Max: MpLocalTime = of(hour = 23, minute = 59, second = 59, milliSecond = 999)

        /**
         * The time at noon at 12:00:00.000
         */
        val Noon: MpLocalTime = of(hour = 12, minute = 0, second = 0, milliSecond = 0)
    }

    /** The hour component. */
    val hour: Int
        get() {
            return ((milliSeconds % MillisPerDay) / MillisPerHour).toInt()
        }

    /** The minute component. */
    val minute: Int
        get() {
            return ((milliSeconds % MillisPerHour) / MillisPerMinute).toInt()
        }

    /** The second component. */
    val second: Int
        get() {
            return ((milliSeconds % MillisPerMinute) / MillisPerSecond).toInt()
        }

    /** The millisecond component. */
    val milliSecond: Int
        get() {
            return (milliSeconds % MillisPerSecond).toInt()
        }

    /**
     * Compares to the [other].
     */
    override fun compareTo(other: MpLocalTime): Int {
        return milliSeconds.compareTo(other.milliSeconds)
    }

    /**
     * Converts to a string.
     */
    override fun toString(): String {
        return "MpLocalTime(${toIsoString()})"
    }

    /**
     * Converts to an iso string of the form HH:mm:ss.SSS
     */
    fun toIsoString(): String {
        return format("HH:mm:ss.SSS")
    }

    /**
     * Formats the date according to the given [format].
     *
     * See https://help.gooddata.com/cloudconnect/manual/date-and-time-format.html
     */
    fun format(format: String): String {
        val klock = Time(hour = hour, minute = minute, second = second, millisecond = milliSecond)

        return klock.format(format)
    }

    /**
     * Get the milliseconds of day represented by this time.
     */
    fun inWholeMilliSeconds(): Long {
        return milliSeconds
    }

    /**
     * Adds the given [duration].
     */
    fun plus(duration: Duration): MpLocalTime {
        return ofMilliSeconds(milliSeconds + duration.inWholeMilliseconds)
    }

    /**
     * Subtracts the given [duration].
     */
    fun minus(duration: Duration): MpLocalTime {
        return plus(duration.unaryMinus())
    }

    /**
     * Calculates the [Duration] between this and the [other].
     */
    operator fun minus(other: MpLocalTime): Duration {
        return (milliSeconds - other.milliSeconds).milliseconds
    }
}

/**
 * Formats as HH:mm
 */
fun MpLocalTime.formatHhMm(): String {
    return format("HH:mm")
}

/**
 * Formats as HH:mm:ss
 */
fun MpLocalTime.formatHhMmSs(): String {
    return format("HH:mm:ss")
}
