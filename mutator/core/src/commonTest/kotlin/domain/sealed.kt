package de.peekandpoke.mutator.domain

import de.peekandpoke.mutator.Mutable

@Mutable
sealed interface SealedInterface {

    data class One(
        val value: String,
    ) : SealedInterface

    data class Two(
        val value: Int,
    ) : SealedInterface
}
