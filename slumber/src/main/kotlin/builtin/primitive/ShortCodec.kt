package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object ShortCodec : Awaker, Slumberer {

    override fun awake(data: Any?) = map(data)

    override fun slumber(data: Any?) = map(data)

    private fun map(data: Any?): Short? {

        if (data is Number) {
            return data.toShort()
        }

        if (data is String) {
            return data.toShortOrNull()
        }

        return null
    }
}
