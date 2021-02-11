package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

class ObjectInstanceCodec(private val instance: Any) : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context): Any? {
        if (data == null) {
            return null
        }

        return instance
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? {

        if (data == null) {
            return null
        }

        return emptyMap<Any, Any>()
    }
}
