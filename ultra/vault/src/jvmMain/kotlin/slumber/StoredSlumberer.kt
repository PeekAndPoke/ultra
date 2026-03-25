package io.peekandpoke.ultra.vault.slumber

import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.vault.Storable

object StoredSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any?>? {

        if (data !is Storable<*>) {
            return null
        }

        val slumbered = mutableMapOf<String, Any?>()

        @Suppress("UNCHECKED_CAST")
        slumbered.putAll(
            context.slumber(data.value) as Map<String, Any?>
        )

        if (data._id.isNotEmpty()) {
            slumbered["_id"] = data._id
        }

        if (data._key.isNotEmpty()) {
            slumbered["_key"] = data._key
        }

        return slumbered
    }
}
