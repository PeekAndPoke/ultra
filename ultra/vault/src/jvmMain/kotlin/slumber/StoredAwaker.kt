package de.peekandpoke.ultra.vault.slumber

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.ensureKey
import kotlin.reflect.KType

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
