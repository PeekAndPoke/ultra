package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object NullCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context): Any? = null

    override fun slumber(data: Any?, context: Slumberer.Context): Any? = null
}
