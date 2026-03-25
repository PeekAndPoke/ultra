package io.peekandpoke.ultra.slumber.builtin.objects

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

object AnyAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = data
}

object AnySlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = context.slumber(data)
}
