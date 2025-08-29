package de.peekandpoke.mutator.domain

import de.peekandpoke.mutator.Mutable

@Mutable
data class Address(
    val street: String,
    val city: String,
    val zip: String,
)
