package de.peekandpoke.ultra.common.datetime

import com.soywiz.klock.Time
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// TODO: test all of me
@Suppress("DataClassPrivateConstructor")
@Serializable(with = MpLocalTimeSerializer::class)
data class MpLocalTime private constructor(val milliSeconds: Long) : Comparable<MpLocalTime> {

    companion object {
        private const val MillisPerSecond = 1_000L
        private const val MillisPerMinute = 60 * MillisPerSecond
        private const val MillisPerHour = 60 * MillisPerMinute
        private const val MillisPerDay = 24 * MillisPerHour

        fun ofMilliSeconds(milliSeconds: Long): MpLocalTime {
            return MpLocalTime(
                milliSeconds = milliSeconds % MillisPerDay
            )
        }

        fun of(hours: Int, minutes: Int, seconds: Int = 0, millis: Int = 0): MpLocalTime {
            return ofMilliSeconds(
                milliSeconds = (hours * 60 * 60 + minutes * 60 + seconds) * 1_000L + millis
            )
        }

        fun ofSecondOfDay(secondsOfDay: Long): MpLocalTime {
            return ofMilliSeconds(milliSeconds = secondsOfDay * 1_000L)
        }

        val Min: MpLocalTime = of(hours = 0, minutes = 0, seconds = 0, millis = 0)

        val Max: MpLocalTime = of(hours = 23, minutes = 59, seconds = 59, millis = 999)

        val Noon: MpLocalTime = of(hours = 12, minutes = 0, seconds = 0, millis = 0)
    }

    val hours: Int get() = ((milliSeconds % MillisPerDay) / MillisPerHour).toInt()
    val minutes: Int get() = ((milliSeconds % MillisPerHour) / MillisPerMinute).toInt()
    val seconds: Int get() = ((milliSeconds % MillisPerMinute) / MillisPerSecond).toInt()
    val millis: Int get() = (milliSeconds % MillisPerSecond).toInt()

    override fun toString(): String {
        return "MpLocalTime(${toIsoString()})"
    }

    fun toIsoString(): String {
        return format("HH:mm:ss.SSS")
    }

    fun format(format: String): String {
        val klock = Time(hour = hours, minute = minutes, second = seconds, millisecond = millis)

        return klock.format(format)
    }

    override fun compareTo(other: MpLocalTime): Int {
        return milliSeconds.compareTo(other.milliSeconds)
    }

    fun plus(duration: Duration): MpLocalTime {
        return ofMilliSeconds(milliSeconds + duration.inWholeMilliseconds)
    }

    fun minus(duration: Duration): MpLocalTime {
        return plus(duration.unaryMinus())
    }

    operator fun minus(other: MpLocalTime): Duration {
        return (milliSeconds - other.milliSeconds).milliseconds
    }
}

fun MpLocalTime.formatHhMm(): String {
    return format("HH:mm")
}

fun MpLocalTime.formatHhMmSs(): String {
    return format("HH:mm:ss")
}
