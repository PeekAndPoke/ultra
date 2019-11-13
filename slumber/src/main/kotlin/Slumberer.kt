package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import kotlin.reflect.KType

interface Slumberer {

    data class Context(val codec: Codec, val attributes: TypedAttributes, val path: String) {

        fun stepInto(step: String) = copy(path = "${path}.$step")

        fun slumber(data: Any?) = codec.slumber(data)

        fun slumber(targetType: KType, data: Any?) = codec.slumber(targetType, data)

        fun reportNullError(): Nothing = throw SlumbererException(
            "Value at path '$path' must no be null"
        )
    }

    fun slumber(data: Any?, context: Context): Any?
}
