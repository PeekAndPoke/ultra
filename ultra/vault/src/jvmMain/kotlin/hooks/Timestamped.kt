package io.peekandpoke.ultra.vault.hooks

import io.peekandpoke.ultra.datetime.MpInstant

/**
 * Entities that carry a creation and a modification instant.
 *
 * The stamping is done by [TimestampedHook], which a repository runs before every save.
 *
 * Implementations of the `with...` methods must return the receiver's own type: the hook casts the
 * result back to the entity type, and that cast is unchecked.
 *
 * See [TimestampedMillis] for the epoch-millis variant.
 */
interface Timestamped {
    /** When the entity was first written, or [MpInstant.Epoch] while it never has been. */
    val createdAt: MpInstant

    /** When the entity was last written — every save bumps this, whether the value changed or not. */
    val updatedAt: MpInstant

    /** Returns a copy with [createdAt] set to [instant]. */
    fun withCreatedAt(instant: MpInstant): Timestamped

    /** Returns a copy with [updatedAt] set to [instant]. */
    fun withUpdatedAt(instant: MpInstant): Timestamped

    /**
     * Returns a copy stamped with [now]: [updatedAt] always, [createdAt] only while it is unset.
     *
     * "Unset" means [MpInstant.Epoch] or earlier, so an entity whose [createdAt] was reset to the
     * default — or that legitimately predates 1970 — is stamped with [now] again.
     */
    fun withTimestamps(now: MpInstant): Timestamped {
        val createdAtApplied = when (createdAt <= MpInstant.Epoch) {
            true -> withCreatedAt(now)
            else -> this
        }

        return createdAtApplied.withUpdatedAt(now)
    }
}
