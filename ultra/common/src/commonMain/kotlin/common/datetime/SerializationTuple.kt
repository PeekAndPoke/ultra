package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable
internal data class SerializationTuple(
    val ts: Long,
    val timezone: String,
    val human: String? = "",
)
