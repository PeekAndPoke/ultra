package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable(with = PortableDateSerializer::class)
data class PortableDate(val timestamp: Long) : Comparable<PortableDate> {

    override fun compareTo(other: PortableDate): Int {
        return timestamp.compareTo(other.timestamp)
    }

    fun isGenesis() = timestamp <= GENESIS_TIMESTAMP

    fun isDoomsday() = timestamp >= DOOMSDAY_TIMESTAMP
}
