package de.peekandpoke.ultra.vault.hooks

interface TimestampedMillis {
    val createdMs: Long
    val updatedMs: Long

    fun withCreatedMs(ms: Long): TimestampedMillis
    fun withUpdatedMs(ms: Long): TimestampedMillis

    fun withTimestamps(now: Long): TimestampedMillis {
        val createdMsApplied = when (createdMs <= 0) {
            true -> withCreatedMs(now)
            else -> this
        }

        return createdMsApplied.withUpdatedMs(now)
    }
}

