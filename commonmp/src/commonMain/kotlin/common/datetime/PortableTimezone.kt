package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable(with = PortableTimezoneSerializer::class)
data class PortableTimezone(val id: String)
