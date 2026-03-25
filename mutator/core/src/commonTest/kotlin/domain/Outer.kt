package io.peekandpoke.mutator.domain

import io.peekandpoke.mutator.Mutable

@Mutable
data class Outer(
    val inner: Inner,
) {
    data class Inner(
        val value: Int,
    )
}
