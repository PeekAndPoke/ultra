package de.peekandpoke.common.datetime

import kotlinx.serialization.Serializable

@Serializable(with = SerializableDateTimeSerializer::class)
data class PortableDateTime(val timestamp: Long)

