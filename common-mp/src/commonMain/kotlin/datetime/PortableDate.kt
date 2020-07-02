package de.peekandpoke.common.datetime

import kotlinx.serialization.Serializable

@Serializable(with = SerializableDateSerializer::class)
data class PortableDate(val timestamp: Long)

