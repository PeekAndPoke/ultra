package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable(with = MpLocalTimeSerializer::class)
data class MpLocalTime(val milliSeconds: Long) {

    companion object {
        fun of(secondOfDay: Int) = MpLocalTime(1000L * secondOfDay)

        fun of(hours: Int, minutes: Int) = of(
            secondOfDay = hours * 60 * 60 + minutes * 60
        )

        fun of(hours: Int, minutes: Int, seconds: Int) = of(
            secondOfDay = hours * 60 * 60 + minutes * 60 + seconds
        )

        fun of(hours: Int, minutes: Int, seconds: Int, millis: Int) = MpLocalTime(
            milliSeconds = (hours * 60 * 60 + minutes * 60 + seconds) * 1_000L + millis
        )
    }
}
