package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import kotlin.reflect.KType

interface Awaker {

    data class Context(val codec: Codec, val attributes: TypedAttributes) {
        fun awakeOrNull(type: KType, data: Any?) = codec.awakeOrNull(type, data)
    }

    fun awake(data: Any?, context: Context): Any?
}
