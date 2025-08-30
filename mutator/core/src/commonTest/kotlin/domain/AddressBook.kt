package de.peekandpoke.mutator.domain

import de.peekandpoke.mutator.Mutable

@Mutable
data class AddressBook(
    val addresses: List<Address>,
)
