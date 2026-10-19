package io.peekandpoke.funktor.cluster.locks.domain

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Vault

/**
 * Global lock entry that can be stored.
 */
@Vault
data class GlobalLockEntry(
    /** The lock key */
    val key: String,
    /** The id of the server that holds the lock */
    val serverId: String,
    /** The timestamp when the lock was created */
    val created: MpInstant,
    /** When the lock expires for servers that predate heartbeats; every heartbeat moves it forward */
    val expires: MpInstant,
    /** Random per acquisition; empty for a lock written before heartbeats existed */
    val token: String = "",
    /** The last time the holder signalled that it is still alive */
    val lastHeartbeat: MpInstant = created,
)
