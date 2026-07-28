package io.peekandpoke.ultra.vault.addons

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Vault
import kotlinx.serialization.Serializable

/**
 * The tombstone carried by a [SoftDeletable] entity — present means deleted, `null` means alive.
 *
 * Deletion is expressed by the marker's presence rather than a boolean flag, so the DSL predicates
 * test for null. [Vault] makes the KSP processors generate property paths for the nested fields, so
 * queries can reach `entity.softDelete.deletedAt`.
 */
@Vault
@Serializable
data class SoftDelete(
    /** When the entity was soft-deleted. */
    val deletedAt: MpInstant,
)
