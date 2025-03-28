package de.peekandpoke.ultra.common

fun <X> Set<X>.toggle(value: X) = when (value in this) {
    true -> this.minus(value)
    false -> this.plus(value)
}
