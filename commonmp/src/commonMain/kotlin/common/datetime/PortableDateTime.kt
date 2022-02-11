package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable(with = PortableDateTimeSerializer::class)
data class PortableDateTime(val timestamp: Long) : Comparable<PortableDateTime> {

    override fun compareTo(other: PortableDateTime): Int {
        return timestamp.compareTo(other.timestamp)
    }

    fun isGenesis() = timestamp <= GENESIS_TIMESTAMP

    fun isDoomsday() = timestamp >= DOOMSDAY_TIMESTAMP
}
