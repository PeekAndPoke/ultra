package io.peekandpoke.ultra.vault.slumber

import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.VaultException

/**
 * Slumber [Slumberer] that serializes [Storable] instances into a flat map.
 *
 * The inner value is slumbered first, then `_id` and `_key` are added to the resulting map
 * when they are non-empty. This produces the structure expected by the database driver.
 */
object StoredSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any?>? {

        if (data !is Storable<*>) {
            return null
        }

        val slumbered = mutableMapOf<String, Any?>()

        val inner = context.slumber(data.valueInternal)

        if (inner !is Map<*, *>) {
            throw VaultException(
                "Expected slumbered value to be a Map but got ${inner?.let { it::class.qualifiedName } ?: "null"}"
            )
        }

        @Suppress("UNCHECKED_CAST")
        slumbered.putAll(inner as Map<String, Any?>)

        if (data._id.isNotEmpty()) {
            slumbered["_id"] = data._id
        }

        if (data._key.isNotEmpty()) {
            slumbered["_key"] = data._key
        }

        return slumbered
    }
}
