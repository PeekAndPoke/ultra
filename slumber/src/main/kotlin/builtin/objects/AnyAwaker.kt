package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Awaker

object AnyAwaker : Awaker {

    override fun awake(data: Any?): Any? = data
}
