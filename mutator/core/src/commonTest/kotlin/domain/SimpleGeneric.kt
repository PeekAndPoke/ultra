package de.peekandpoke.mutator.domain

import de.peekandpoke.mutator.Mutable

@Mutable
data class SimpleGeneric<T>(
    val value: T,
)
