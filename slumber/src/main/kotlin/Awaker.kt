package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes

interface Awaker {

    data class Context(val attributes: TypedAttributes)

    fun awake(data: Any?, context: Context): Any?
}
