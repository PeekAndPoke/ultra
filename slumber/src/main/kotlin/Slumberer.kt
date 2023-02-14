package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes

interface Slumberer {

    interface Context {

        val codec: Codec
        val attributes: TypedAttributes
        val path: String

        fun stepInto(step: String): Context

        fun slumber(data: Any?): Any? = codec.slumber(data, this)

//        fun slumber(targetType: KType, data: Any?): Any? = codec.slumber(targetType, data, this)

        @Throws(SlumbererException::class)
        fun reportNullError(): Nothing = throw SlumbererException(
            "Value at path '$path' must no be null"
        )

        class Fast internal constructor(
            override val codec: Codec,
            override val attributes: TypedAttributes,
        ) : Context {
            override val path: String = "<unknown>"

            override fun stepInto(step: String): Fast = this
        }

        class Tracking internal constructor(
            override val codec: Codec,
            override val attributes: TypedAttributes,
            override val path: String,
        ) : Context {

            override fun stepInto(step: String): Tracking = Tracking(
                codec = codec,
                attributes = attributes,
                path = "$path.$step",
            )
        }
    }

    fun slumber(data: Any?, context: Context): Any?
}
