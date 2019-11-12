package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import kotlin.reflect.KType

interface Awaker {

    data class Context(val codec: Codec, val attributes: TypedAttributes, val path: String) {

        fun stepInto(step: String) = copy(path = "${path}.$step")

        fun awake(type: KType, data: Any?): Any? = codec.awake(type, data, this)

        fun reportNullError(): Nothing = throw AwakerException(
            "Value at path '$path' must no be null"
        )
    }

    fun awake(data: Any?, context: Context): Any?
}
