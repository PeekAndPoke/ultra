package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable(with = PortableDateTimeSerializer::class)
data class PortableDateTime(val timestamp: Long)
