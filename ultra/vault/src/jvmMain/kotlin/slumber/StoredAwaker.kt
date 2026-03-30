package io.peekandpoke.ultra.vault.slumber

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.ensureKey
import kotlin.reflect.KType

/**
 * Slumber [Awaker] that reconstructs a [Stored] from a map of database fields.
 *
 * Reads `_id`, `_key`, and `_rev` from the map, delegates awakening of the inner value
 * to the context using [innerType], and wraps everything in a [Stored] instance.
 *
 * @param innerType the [KType] of the value contained in the [Stored] wrapper.
 */
class StoredAwaker(private val innerType: KType) : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is Map<*, *>) {
            return null
        }

        val id = data["_id"] as? String
        val key = data["_key"] as? String
        val rev = data["_rev"] as? String ?: ""

        return when {
            id is String -> {

                val value = context.awake(innerType, data)

                return Stored(
                    value = value,
                    _id = id,
                    _key = key ?: id.ensureKey,
                    _rev = rev,
                )
            }

            else -> null
        }
    }
}
