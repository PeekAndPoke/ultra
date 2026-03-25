package io.peekandpoke.ultra.slumber.builtin.objects

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

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
