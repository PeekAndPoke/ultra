package io.peekandpoke.ultra.common

/**
 * Toggles the presence of [value] in the set.
 *
 * If the set contains [value], returns a new set without it; otherwise returns a new set with it added.
 *
 * The receiver is never modified. Adding appends [value] at the end of the iteration order, so
 * toggling off and on again moves it to the back rather than restoring its original position.
 */
fun <X> Set<X>.toggle(value: X) = when (value in this) {
    true -> this.minus(value)
    false -> this.plus(value)
}
