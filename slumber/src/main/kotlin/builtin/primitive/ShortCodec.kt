package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object ShortCodec : Awaker, Slumberer {

    override fun awake(data: Any?) = map(data)

    override fun slumber(data: Any?) = map(data)

    private fun map(data: Any?): Short? = when (data) {

        is Number -> data.toShort()

        is String -> data.toShortOrNull()

        else -> null
    }
}
