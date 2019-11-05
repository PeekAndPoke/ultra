package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes

interface Slumberer {

    data class Context(val codec: Codec, val attributes: TypedAttributes) {
        fun slumber(data: Any?) = codec.slumber(data)
    }

    fun slumber(data: Any?, context: Context): Any?
}
