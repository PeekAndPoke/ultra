package de.peekandpoke.mutator.domain

import de.peekandpoke.mutator.Mutable

@Mutable
data class Person(
    val firstName: String,
    val lastName: String,
    val age: Int,
    val address: Address,
)
