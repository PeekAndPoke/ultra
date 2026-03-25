package io.peekandpoke.mutator.domain

import io.peekandpoke.mutator.Mutable

@Mutable
data class WithCollections(
    val tags: Set<Address>,
    val lookup: Map<String, Address>,
)
