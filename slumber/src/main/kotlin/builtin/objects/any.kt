package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object AnyAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = data
}

object AnySlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = context.slumber(data)
}
