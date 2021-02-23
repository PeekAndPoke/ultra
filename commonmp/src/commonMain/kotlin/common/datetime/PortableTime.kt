package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable(with = PortableTimeSerializer::class)
data class PortableTime(val milliSeconds: Long)
