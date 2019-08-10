package de.peekandpoke.ultra.mutator

infix fun <X> X.isSameAs(other: X) = !isNotSameAs(other)

@Suppress("USELESS_CAST")
infix fun <X> X.isNotSameAs(other: X) = when {

    this == null -> other != null

    other == null -> true

    (this as Any)::class == String::class -> this != other

    (this as Any)::class.javaPrimitiveType != null -> this != other

    else -> this !== other
}
