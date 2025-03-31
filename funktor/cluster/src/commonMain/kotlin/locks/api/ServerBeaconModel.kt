package de.peekandpoke.funktor.cluster.locks.api

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.Serializable

@Serializable
data class ServerBeaconModel(
    val serverId: String,
    val serverVersion: String,
    val lastPing: MpInstant,
)
