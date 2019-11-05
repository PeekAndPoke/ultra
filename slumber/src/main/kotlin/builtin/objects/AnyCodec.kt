package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object AnyCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context): Any? = data

    override fun slumber(data: Any?, context: Slumberer.Context): Any? {

        if (data == null) {
            return null
        }

        return context.slumber(data)
    }
}
