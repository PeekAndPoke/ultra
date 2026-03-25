package io.peekandpoke.mutator.domain

import io.peekandpoke.mutator.Mutable

@Mutable
data class Address(
    val street: String,
    val city: String,
    val zip: String,
)
