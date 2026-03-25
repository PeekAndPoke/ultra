package io.peekandpoke.mutator.domain

import io.peekandpoke.mutator.Mutable

@Mutable
data class PersonWithNullableAddress(
    val name: String,
    val address: Address?,
)
