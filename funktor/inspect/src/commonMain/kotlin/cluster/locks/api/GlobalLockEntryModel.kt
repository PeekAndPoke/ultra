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
    /** The timestamp when the lock expires */
    val expires: MpInstant,
)
