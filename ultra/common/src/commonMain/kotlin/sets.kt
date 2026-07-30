package io.peekandpoke.ultra.common

/**
 * Toggles the presence of [value] in the set.
 *
 * If the set contains [value], returns a new set without it; otherwise returns a new set with it added.
 *
 * The receiver is never modified. Toggling off and then on again returns a set that is `==` to the
 * original, but not in the original ITERATION ORDER — the value comes back at the end, because its
 * position is gone once removed. Use a `List` where the order is part of the meaning.
 */
fun <X> Set<X>.toggle(value: X): Set<X> = when (value in this) {
    true -> this.minus(value)
    false -> this.plus(value)
}
