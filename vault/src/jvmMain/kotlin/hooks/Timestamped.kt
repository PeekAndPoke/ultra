package de.peekandpoke.ultra.vault.hooks

import de.peekandpoke.ultra.common.datetime.MpInstant

interface Timestamped {
    val createdAt: MpInstant
    val updatedAt: MpInstant

    fun withCreatedAt(instant: MpInstant): Timestamped
    fun withUpdatedAt(instant: MpInstant): Timestamped

    fun withTimestamps(now: MpInstant): Timestamped {
        val createdAtApplied = when (createdAt <= MpInstant.Epoch) {
            true -> withCreatedAt(now)
            else -> this
        }

        return createdAtApplied.withUpdatedAt(now)
    }
}

