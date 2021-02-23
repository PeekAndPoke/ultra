package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable(with = PortableTimeSerializer::class)
data class PortableTime(val milliSeconds: Long) {

    companion object {
        fun of(secondOfDay: Int) = PortableTime(1000L * secondOfDay)

        fun of(hours: Int, minutes: Int) = of(
            secondOfDay = hours * 60 * 60 + minutes * 60
        )

        fun of(hours: Int, minutes: Int, seconds: Int) = of(
            secondOfDay = hours * 60 * 60 + minutes * 60 + seconds
        )
    }
}
