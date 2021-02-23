package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable(with = PortableDateSerializer::class)
data class PortableDate(val timestamp: Long) : Comparable<PortableDate> {

    override fun compareTo(other: PortableDate): Int {
        return when {
            timestamp < other.timestamp -> -1
            timestamp > other.timestamp -> 1
            else -> 0
        }
    }
}
