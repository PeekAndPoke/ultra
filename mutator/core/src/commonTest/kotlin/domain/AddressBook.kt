package io.peekandpoke.mutator.domain

import io.peekandpoke.mutator.Mutable

@Mutable
data class AddressBook(
    val addresses: List<Address>,
)
