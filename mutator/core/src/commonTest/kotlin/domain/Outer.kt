package de.peekandpoke.mutator.domain

import de.peekandpoke.mutator.Mutable

@Mutable
data class Outer(
    val inner: Inner,
) {
    data class Inner(
        val value: Int,
    )
}
