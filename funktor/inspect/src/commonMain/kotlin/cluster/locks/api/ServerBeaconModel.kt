package io.peekandpoke.funktor.cluster.locks.api

import io.peekandpoke.ultra.datetime.MpInstant
import kotlinx.serialization.Serializable

@Serializable
data class ServerBeaconModel(
    val serverId: String,
    val serverVersion: String,
    val lastPing: MpInstant,
)
