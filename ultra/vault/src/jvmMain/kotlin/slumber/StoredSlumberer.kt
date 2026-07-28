package io.peekandpoke.ultra.vault.slumber

import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.VaultException

/**
 * Slumber [Slumberer] that serializes [Storable] instances into a flat map.
 *
 * The inner value is slumbered first, then `_id` and `_key` are added to the resulting map
 * when they are non-empty. This produces the structure expected by the database driver.
 *
 * Supported inputs are `Stored` and `New` — the two types [VaultSlumberModule] registers this
 * slumberer for. It reads the value through the non-suspending `valueInternal`, so a `Ref` works
 * only after it has been resolved; an unresolved one — including a freshly built `Ref.eager`, which
 * caches nothing until the first `resolve()` — throws `IllegalStateException`. In practice a `Ref`
 * lands on [RefCodec] instead, both as a declared type and as a field value, and is written as its
 * plain `_id`.
 *
 * Returns `null` for input that is not a [Storable] (including `null`), and throws [VaultException]
 * when the inner value does not slumber to a Map — e.g. a `Stored<String>`, whose value slumbers to
 * a String. Note [VaultException] is a `Throwable`, not an `Exception`.
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
