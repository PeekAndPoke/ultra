package de.peekandpoke.ultra.mutator

typealias OnModify<T> = (newValue: T) -> Unit

infix fun <X> X.isSameAs(other: X) = !isNotSameAs(other)

@Suppress("USELESS_CAST")
infix fun <X> X.isNotSameAs(other: X) = when {

    this == null -> other != null

    other == null -> true

    else -> {

        val cls = (this as Any)::class

        when {

            cls == String::class -> this != other

            cls.javaPrimitiveType != null -> this != other

            else -> this !== other
        }
    }
}
