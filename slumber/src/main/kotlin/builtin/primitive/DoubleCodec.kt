package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object DoubleCodec : Awaker, Slumberer {

    override fun awake(data: Any?) = map(data)

    override fun slumber(data: Any?) = map(data)

    private fun map(data: Any?): Double? {

        if (data is Number) {
            return data.toDouble()
        }

        if (data is String) {
            return data.toDoubleOrNull()
        }

        return null
    }
}
