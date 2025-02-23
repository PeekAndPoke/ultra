package de.peekandpoke.ultra.vault.slumber

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.vault.Stored
import kotlin.reflect.KType

class StoredAwaker(private val innerType: KType) : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is Map<*, *>) {
            return null
        }

        val id = data["_id"]
        val key = data["_key"]
        val rev = data["_rev"]

        return when {
            id is String && key is String && rev is String -> {

                val value = context.awake(innerType, data)

                return Stored(value = value, _id = id, _key = key, _rev = rev)
            }

            else -> null
        }
    }
}
