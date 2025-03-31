package de.peekandpoke.funktor.cluster.locks.api

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.Serializable

@Serializable
data class GlobalLockEntryModel(
    /** The lock key */
    val key: String,
    /** The id of the server that holds the lock */
    val serverId: String,
    /** The timestamp when the lock was created */
    val created: MpInstant,
    /** The timestamp when the lock expires */
    val expires: MpInstant,
)
