package io.peekandpoke.funktor.cluster.locks.domain

import io.peekandpoke.ultra.datetime.MpInstant

/**
 * Global lock entry that can be stored.
 */
data class GlobalLockEntry(
    /** The lock key */
    val key: String,
    /** The id of the server that holds the lock */
    val serverId: String,
    /** The timestamp when the lock was created */
    val created: MpInstant,
    /** The timestamp when the lock expires */
    val expires: MpInstant,
)
