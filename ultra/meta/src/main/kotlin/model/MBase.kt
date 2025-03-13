package de.peekandpoke.ultra.meta.model

import de.peekandpoke.ultra.meta.ProcessorUtils

abstract class MBase(val model: Model) : ProcessorUtils {
    override val ctx = model.ctx
}
