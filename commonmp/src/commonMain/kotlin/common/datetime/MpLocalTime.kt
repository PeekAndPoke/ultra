package de.peekandpoke.ultra.common.datetime

import com.soywiz.klock.Time
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// TODO: test all of me
@Suppress("DataClassPrivateConstructor")
@Serializable(with = MpLocalTimeSerializer::class)
data class MpLocalTime private constructor(private val milliSeconds: Long) : Comparable<MpLocalTime> {

    companion object {
        private const val MillisPerSecond = 1_000L
        private const val MillisPerMinute = 60 * MillisPerSecond
        private const val MillisPerHour = 60 * MillisPerMinute
        private const val MillisPerDay = 24 * MillisPerHour

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

        fun ofMilliSeconds(milliSeconds: Long): MpLocalTime {
            return MpLocalTime(
                milliSeconds = milliSeconds % MillisPerDay
            )
        }

        fun of(hour: Int, minute: Int, second: Int = 0, milliSecond: Int = 0): MpLocalTime {
            return ofMilliSeconds(
                milliSeconds = (hour * 60 * 60 + minute * 60 + second) * 1_000L + milliSecond
            )
        }

        fun ofSecondOfDay(secondsOfDay: Long): MpLocalTime {
            return ofMilliSeconds(milliSeconds = secondsOfDay * 1_000L)
        }

        val Min: MpLocalTime = of(hour = 0, minute = 0, second = 0, milliSecond = 0)

        val Max: MpLocalTime = of(hour = 23, minute = 59, second = 59, milliSecond = 999)

        val Noon: MpLocalTime = of(hour = 12, minute = 0, second = 0, milliSecond = 0)
    }

    val hour: Int get() = ((milliSeconds % MillisPerDay) / MillisPerHour).toInt()
    val minute: Int get() = ((milliSeconds % MillisPerHour) / MillisPerMinute).toInt()
    val second: Int get() = ((milliSeconds % MillisPerMinute) / MillisPerSecond).toInt()
    val milliSecond: Int get() = (milliSeconds % MillisPerSecond).toInt()

    override fun compareTo(other: MpLocalTime): Int {
        return milliSeconds.compareTo(other.milliSeconds)
    }

    override fun toString(): String {
        return "MpLocalTime(${toIsoString()})"
    }

    fun toIsoString(): String {
        return format("HH:mm:ss.SSS")
    }

    fun format(format: String): String {
        val klock = Time(hour = hour, minute = minute, second = second, millisecond = milliSecond)

        return klock.format(format)
    }

    fun inWholeMilliSeconds(): Long {
        return milliSeconds
    }

    fun asDuration(): Duration {
        return milliSeconds.milliseconds
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
