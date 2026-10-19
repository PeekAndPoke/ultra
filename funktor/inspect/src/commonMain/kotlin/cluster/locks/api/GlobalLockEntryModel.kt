package io.peekandpoke.funktor.inspect.cluster.locks.api

import io.peekandpoke.ultra.datetime.MpInstant
import kotlinx.serialization.Serializable

@Serializable
data class GlobalLockEntryModel(
    /** The lock key */
    val key: String,
    /** The id of the server that holds the lock */
    val serverId: String,
    /** The timestamp when the lock was created */
    val created: MpInstant,
    /** When the lock expires for servers that predate heartbeats */
    val expires: MpInstant,
    /** The last time the holder signalled that it is still alive; defaulted for a server that predates heartbeats */
    val lastHeartbeat: MpInstant = created,
)
