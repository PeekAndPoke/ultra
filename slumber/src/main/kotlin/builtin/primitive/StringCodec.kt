package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object StringCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context) = map(data)

    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)

    private fun map(data: Any?): String? = when {

        data is String -> data

        data != null && data::class.java.isPrimitive -> data.toString()

        else -> null
    }
}
