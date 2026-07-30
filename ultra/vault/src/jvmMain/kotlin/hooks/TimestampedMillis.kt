package io.peekandpoke.ultra.vault.hooks

/**
 * Entities that carry a creation and a modification time as epoch millis.
 *
 * The stamping is done by [TimestampedMillisHook], which a repository runs before every save.
 *
 * Implementations of the `with...` methods must return the receiver's own type: the hook casts the
 * result back to the entity type, and that cast is unchecked.
 *
 * See [Timestamped] for the `MpInstant` variant.
 */
interface TimestampedMillis {
    /** When the entity was first written, or `0` while it never has been. */
    val createdMs: Long

    /** When the entity was last written — every save bumps this, whether the value changed or not. */
    val updatedMs: Long

    /** Returns a copy with [createdMs] set to [ms]. */
    fun withCreatedMs(ms: Long): TimestampedMillis

    /** Returns a copy with [updatedMs] set to [ms]. */
    fun withUpdatedMs(ms: Long): TimestampedMillis

    /**
     * Returns a copy stamped with [now]: [updatedMs] always, [createdMs] only while it is unset.
     *
     * "Unset" means `0` or negative, so an entity whose [createdMs] was reset to the default — or
     * that legitimately predates 1970 — is stamped with [now] again.
     */
    fun withTimestamps(now: Long): TimestampedMillis {
        val createdMsApplied = when (createdMs <= 0) {
            true -> withCreatedMs(now)
            else -> this
        }

        return createdMsApplied.withUpdatedMs(now)
    }
}
